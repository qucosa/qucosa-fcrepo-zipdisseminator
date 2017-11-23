package de.qucosa.zipfiledisseminator;

import de.qucosa.XmlUtils.Namespaces;
import de.qucosa.XmlUtils.SimpleNamespaceContext;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static javax.servlet.http.HttpServletResponse.*;

public class DisseminationServlet extends HttpServlet {

    private static final String REQUEST_PARAM_METS_URL = "metsurl";
    private static final String xPathFLocat = "//mets:fileSec/mets:fileGrp[@USE='DOWNLOAD']/mets:file[@USE='ARCHIVE']/mets:FLocat";
    private static final String zipFileName = "content.zip";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private CloseableHttpClient httpClient;

    @Override
    public void init() throws ServletException {
        httpClient = HttpClientBuilder
                .create()
                .setConnectionManager(new PoolingHttpClientConnectionManager())
                .build();
    }

    @Override
    public void destroy() {
        try {
            httpClient.close();
        } catch (IOException e) {
            log.warn("Problem closing HTTP client: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            final URI metsDocumentUri = URI.create(getRequiredRequestParameterValue(req, REQUEST_PARAM_METS_URL));

            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(metsDocumentUri))) {
                if (SC_OK == response.getStatusLine().getStatusCode()) {
                    InputStream metsInputStream = response.getEntity().getContent();
                    // get METS document from Inputstream
                    Document metsDocument = getDocumentFromInputStream(metsInputStream);
                    // get url-map from METS document
                    Map<String, URL> fileNameList = getListOfFileUris(metsDocument, xPathFLocat);
                    // download files
                    List<File> fileList = downloadFiles(fileNameList);
                    // create zip outputstream by creating a zip file in memory with downloaded files
                    byte[] zip = zipFiles(fileList);

                    ServletOutputStream servletOutputStream = resp.getOutputStream();
                    resp.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
                    resp.setContentType("application/zip");

                    servletOutputStream.write(zip);
                    resp.setStatus(SC_OK);

                    servletOutputStream.flush();
                } else {
                    sendError(resp, SC_NOT_FOUND, "Cannot obtain METS document at " + metsDocumentUri.toASCIIString());
                }
            }
        } catch (MissingRequiredParameter | IllegalArgumentException e) {
            sendError(resp, SC_BAD_REQUEST, e.getMessage());
        } catch (Throwable anythingElse) {
            log.warn("Internal server error", anythingElse);
            sendError(resp, SC_INTERNAL_SERVER_ERROR, anythingElse.getMessage());
        }
    }

    private void sendError(HttpServletResponse resp, int status, String msg) throws IOException {
        resp.setStatus(status);
        resp.setContentType("text/plain");
        resp.setContentLength(msg.getBytes().length);
        resp.getWriter().print(msg);
    }

    private String getRequiredRequestParameterValue(ServletRequest request, String name)
            throws MissingRequiredParameter {
        final String v = request.getParameter(name);
        if (v == null || v.isEmpty()) {
            throw new MissingRequiredParameter("Missing parameter '" + REQUEST_PARAM_METS_URL + "'");
        }
        return v;
    }

    private Document getDocumentFromInputStream(InputStream metsInputStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = null;
        Document document = null;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        builder = documentBuilderFactory.newDocumentBuilder();
        document = (Document) builder.parse(metsInputStream);

        return document;
    }

    private Map<String, URL> getListOfFileUris(Document metsDocument, String xPath) throws XPathExpressionException, MalformedURLException {
//        List<URL> urlList = new ArrayList<>();
        Map<String, URL> urlList = new HashMap<>();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Namespaces.getPrefixUriMap());
        xpath.setNamespaceContext(namespaces);

        XPathExpression xPathExpr = xpath.compile(xPath);
        NodeList nodeFLocat = (NodeList) xPathExpr.evaluate(metsDocument, XPathConstants.NODESET);

        for (int k=0; k<nodeFLocat.getLength(); k++) {
            Element element = (Element) nodeFLocat.item(k);
            String downloadUrl = element.getAttribute("xlin:href");
            String fileTitle = element.getAttribute("xlin:title");
            String completeUrl = downloadUrl.replace(":8080", ".slub-dresden.de:8080");
            URL fileURL = new URL(completeUrl);
            urlList.put(fileTitle, fileURL);
        }

        return urlList;
    }

    private List<File> downloadFiles(Map<String, URL> fileList) throws IOException {
        List<File> contentFiles = new ArrayList<>();

        for (Map.Entry<String, URL> entry : fileList.entrySet()) {
            File file = new File(entry.getKey());
            FileUtils.copyURLToFile(entry.getValue(), file);
//            FileUtils.toFile(entry.getValue());
            contentFiles.add(file);
        }

        return contentFiles;
    }

    void downloadFromUrl(URL url, String localFilename) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;

        try {
            URLConnection urlConn = url.openConnection();//connect

            is = urlConn.getInputStream();               //get connection inputstream
            fos = new FileOutputStream(localFilename);   //open outputstream to local file

            byte[] buffer = new byte[4096];              //declare 4KB buffer
            int len;

            //while we have availble data, continue downloading and storing to local file
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        }
    }

    /* TODO streams verketten, byte-array direkt in outputsream schreiben */
    private byte[] zipFiles(List<File> fileList) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try(ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (File file : fileList) {
                FileInputStream fis = new FileInputStream(file);
                // TODO FileName dependent on MIMETYPE and XLIN:TITLE
                ZipEntry zipEntry = new ZipEntry(file.getName());
                zip.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zip.write(bytes, 0, length);
                }

                zip.closeEntry();
                fis.close();
            }
            zip.flush();
            baos.flush();
            zip.close();
            baos.close();
        } catch(IOException ioe) {
            ioe.printStackTrace();
        }

        return baos.toByteArray();
    }

}

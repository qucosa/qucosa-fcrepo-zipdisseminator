/*
 * Copyright (C) 2017 Saxon State and University Library Dresden (SLUB)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.zipfiledisseminator;

import de.qucosa.XmlUtils.Namespaces;
import de.qucosa.XmlUtils.SimpleNamespaceContext;
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
import java.util.ArrayList;
import java.util.List;
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

                    Document metsDocument = getDocumentFromInputStream(response.getEntity().getContent());
                    List<DocumentFile> documentFiles = getDocumentFiles(metsDocument, xPathFLocat);

                    ServletOutputStream servletOutputStream = resp.getOutputStream();
                    resp.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
                    resp.setContentType("application/zip");
                    // write Zip-file (ByteArrayOutputStream) to Output-Stream
                    getOutputStreamWithCompressedFile(documentFiles).writeTo(servletOutputStream);
                    resp.setStatus(SC_OK);

                    servletOutputStream.flush();
                } else {
                    sendError(resp, SC_NOT_FOUND, "Cannot obtain METS document at " + metsDocumentUri.toASCIIString());
                }
            }
        } catch (MissingRequiredParameter | IllegalArgumentException | MissingDocumentNode e) {
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
        document = builder.parse(metsInputStream);

        return document;
    }

    private List<DocumentFile> getDocumentFiles(Document metsDocument, String xPath) throws MissingDocumentNode, XPathExpressionException, MalformedURLException {
        List<DocumentFile> documentFileList = new ArrayList<>();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Namespaces.getPrefixUriMap());
        xpath.setNamespaceContext(namespaces);

        XPathExpression xPathExpr = xpath.compile(xPath);
        NodeList nodeFLocat = (NodeList) xPathExpr.evaluate(metsDocument, XPathConstants.NODESET);

        for (int k = 0; k < nodeFLocat.getLength(); k++) {
            DocumentFile documentFile = new DocumentFile();
            String downloadUrl;
            String urlWithDomain;
            Element element = (Element) nodeFLocat.item(k);
            if (!element.getAttribute("xlin:href").isEmpty() || !element.getAttribute("xlin:title").isEmpty()) {
                downloadUrl = element.getAttribute("xlin:href");
                urlWithDomain = downloadUrl.replace(":8080", ".slub-dresden.de:8080");
                URL fileURL = new URL(urlWithDomain);
//                URL fileURL = new URL(downloadUrl);

                documentFile.setContentUrl(fileURL);
                documentFile.setTitle(element.getAttribute("xlin:title"));
                documentFileList.add(documentFile);
            } else {
                throw new MissingDocumentNode("Cannot obtain content links (xlin:href or xlin:title) from METS document: " + metsDocument.getDocumentURI());
            }
        }

        return documentFileList;
    }

    private ByteArrayOutputStream getOutputStreamWithCompressedFile(List<DocumentFile> fileList) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(baos)) {
            for (DocumentFile file : fileList) {
                InputStream is = file.getContentUrl().openStream();

                // TODO FileName dependent on MIMETYPE and XLIN:TITLE
                ZipEntry zipEntry = new ZipEntry(file.getTitle());
                zip.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = is.read(bytes)) >= 0) {
                    zip.write(bytes, 0, length);
                }
                zip.closeEntry();
                is.close();
            }
            zip.flush();
            baos.flush();
            zip.close();
            baos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        return baos;
    }
}

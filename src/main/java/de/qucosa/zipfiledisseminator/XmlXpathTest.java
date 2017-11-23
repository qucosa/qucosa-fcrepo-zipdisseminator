package de.qucosa.zipfiledisseminator;

import de.qucosa.XmlUtils.Namespaces;
import de.qucosa.XmlUtils.SimpleNamespaceContext;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class XmlXpathTest {

    private static final String REQUEST_PARAM_METS_URL = "metsurl";
    private static final String xPathMetsFileGrp = "//mets:fileSec/mets:fileGrp[@USE='DOWNLOAD']/";
//    private static final String xPathFileHref = "//mets:fileSec/mets:fileGrp[@USE='DOWNLOAD']/mets:file[@USE='ARCHIVE']/mets:FLocat";
    private static final String xPathFileHref = "/main:workbook/main:sheets/main:sheet[1]";
    private static final String zipFileName = "content.zip";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) {
        try {
            XmlXpathTest xmlXpathTest = new XmlXpathTest();
            xmlXpathTest.doGet();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void doGet()
            throws IOException {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
                    File metsInputStream = new File(classLoader.getResource("XmlExample.xml").getFile());
                    // get METS document from Inputstream
                    Document metsDocument = getDocumentFromInputStream(metsInputStream);
                    // get Files-list from METS document
                    List<String> fileNameList = getListOfFileUris(metsDocument, xPathFileHref);
                    // download files
                    List<File> fileList = downloadFiles(fileNameList);
                    // create zip outputstream by creating a zip file in memory with downloaded files
                    byte[] zip = zipFiles(fileList);

        } catch (IllegalArgumentException e) {
            log.warn("BAD REQUEST: ", e.getMessage());
        } catch (Throwable anythingElse) {
            log.warn("Internal server error", anythingElse);
        }
    }

    private Document getDocumentFromInputStream(File metsInputStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder builder = null;
        Document document = null;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        builder = documentBuilderFactory.newDocumentBuilder();
        document = (Document) builder.parse(metsInputStream);

        return document;
    }

    private List<String> getListOfFileUris(Document metsDocument, String xPathExpression) throws XPathExpressionException {
        List<String> fileList = new ArrayList<>();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        /** namespace test **/
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Namespaces.getPrefixUriMap());
        xpath.setNamespaceContext(namespaces);

        XPathExpression xPathExpr = xpath.compile(xPathExpression);
        NodeList nodeFLocat = (NodeList) xPathExpr.evaluate(metsDocument, XPathConstants.NODESET);

        for (int k=0; k<nodeFLocat.getLength(); k++) {
            Element element = (Element) nodeFLocat.item(k);
            fileList.add(element.getAttribute("xlin:href"));
        }

        return fileList;
    }

    private List<File> downloadFiles(List<String> fileList) throws MalformedURLException {
        List<File> contentFiles = new ArrayList<>();
        for (String fileName : fileList) {
            File file = FileUtils.toFile(new URL(fileName));
            contentFiles.add(file);
        }

        return contentFiles;
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

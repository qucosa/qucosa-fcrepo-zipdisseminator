package de.qucosa.DisseminationTests;

import de.qucosa.XmlUtils.Namespaces;
import de.qucosa.zipfiledisseminator.DisseminationServlet;
import de.qucosa.zipfiledisseminator.DocumentFile;
import de.qucosa.zipfiledisseminator.MissingDocumentNode;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.List;

public class DisseminationTest {
    private static final String xPathFLocat = "//mets:fileSec/mets:fileGrp[@USE='DOWNLOAD']/mets:file[@USE='ARCHIVE']/mets:FLocat";

    @Test
    @Ignore("Not yet implemented.")
    public void XMLParsing() throws XPathExpressionException, MissingDocumentNode, IOException, SAXException, ParserConfigurationException {
        ClassLoader classLoader = getClass().getClassLoader();
        File metsFile = new File(classLoader.getResource("MetsExampleKleiner.xml").getFile());
        InputStream is = new FileInputStream(metsFile);
        DisseminationServlet servlet = new DisseminationServlet();
        Document metsDocument = servlet.getDocumentFromInputStream(is);
        List<DocumentFile> documentFiles = servlet.getDocumentFiles(metsDocument, xPathFLocat);
        FileOutputStream fos = new FileOutputStream(new File("content.zip"));
        servlet.getOutputStreamWithCompressedFile(documentFiles).writeTo(fos);

        /* TODO read and confirm zip */
    }

    @BeforeClass
    static public void setupXpathNamespaces() {
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(Namespaces.getPrefixUriMap()));
    }
}

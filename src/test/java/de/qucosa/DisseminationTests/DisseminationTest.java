package de.qucosa.DisseminationTests;


import de.qucosa.XmlUtils.Namespaces;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.List;

import static org.custommonkey.xmlunit.XMLAssert.*;

public class DisseminationTest {
    @Test
    public void XMLParsing() {

//        Document metsDocument = getDocumentFromInputStream(metsInputStream);
//        List fileList = getListOfFilesToZip(metsDocument, xPathMetsFileGrp);
        //assertXpathExists();
    }

    @BeforeClass
    static public void setupXpathNamespaces() {
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(Namespaces.getPrefixUriMap()));
    }
}

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

package de.qucosa.zipdisseminator;

import de.qucosa.xmlutils.Namespaces;
import de.qucosa.xmlutils.SimpleNamespaceContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipDisseminator {

    private static final String xPathFLocat = "//mets:fileSec/mets:fileGrp[@USE='DOWNLOAD']/mets:file[@USE='ARCHIVE']/mets:FLocat";
    private DocumentBuilderFactory documentBuilderFactory;

    public ZipDisseminator() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
    }

    void disseminateZipForMets(InputStream in, OutputStream out) throws InvalidMETSDocument, IOException {
        disseminateZipForMets(in, out, FilenameFilterConfiguration.EMPTY);
    }

    void disseminateZipForMets(
            InputStream metsInputStream,
            OutputStream zipOutputStream,
            FilenameFilterConfiguration conf) throws InvalidMETSDocument, IOException {

        Document metsDocument;
        try {
            metsDocument = documentBuilderFactory.newDocumentBuilder().parse(metsInputStream);
        } catch (SAXException e) {
            // XML parsing error
            throw new InvalidMETSDocument(e);
        } catch (ParserConfigurationException e) {
            // parser configuration should work
            throw new RuntimeException(e);
        }

        List<DocumentFile> documentFiles = getDocumentFiles(metsDocument, xPathFLocat);

        for (DocumentFile f : documentFiles) {
            if (!conf.replacements().isEmpty()) {
                String filtered = f.getTitle();
                for (String k : conf.replacements().keySet()) {
                    String v = conf.replacements().get(k);
                    filtered = filtered.replaceAll(k, v);
                }
                f.setTitle(filtered);
            }
        }

        zip(documentFiles, zipOutputStream);
    }

    private List<DocumentFile> getDocumentFiles(Document metsDocument, String xPath) throws InvalidMETSDocument {
        List<DocumentFile> documentFileList = new ArrayList<>();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Namespaces.getPrefixUriMap());
        xpath.setNamespaceContext(namespaces);

        XPathExpression xPathExpr;
        NodeList nodeFLocat;
        try {
            xPathExpr = xpath.compile(xPath);
            nodeFLocat = (NodeList) xPathExpr.evaluate(metsDocument, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            // if hard coded xpath is incorrect, the program is broken
            throw new RuntimeException(e);
        }

        try {
            for (int k = 0; k < nodeFLocat.getLength(); k++) {
                Element element = (Element) nodeFLocat.item(k);

                String href = element.getAttributeNS(Namespaces.XLIN.getURI(), "href");
                String title = element.getAttributeNS(Namespaces.XLIN.getURI(), "title");

                if (href.isEmpty() || title.isEmpty()) {
                    throw new InvalidMETSDocument("Cannot obtain content links from METS document: " + metsDocument.getDocumentURI());
                }

                DocumentFile documentFile = new DocumentFile();
                documentFile.setContentUrl(new URL(href));
                documentFile.setTitle(title);

                documentFileList.add(documentFile);
            }
        } catch (MalformedURLException e) {
            // throw on invalid URLs in METS document
            throw new InvalidMETSDocument(e);
        }

        return documentFileList;
    }

    private void zip(List<DocumentFile> fileList, OutputStream outputStream) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(outputStream)) {
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
        }
    }

}

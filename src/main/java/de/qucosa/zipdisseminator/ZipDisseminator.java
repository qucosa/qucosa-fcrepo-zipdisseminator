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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ZipDisseminator {

    private static final String xPathFLocat = "//mets:fileSec/mets:fileGrp[@USE='DOWNLOAD']/mets:file[@USE='ARCHIVE']/mets:FLocat";
    private final DocumentBuilderFactory documentBuilderFactory;

    public ZipDisseminator() {
        documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
    }

    void disseminateZipForMets(InputStream in, OutputStream out) throws InvalidMETSDocument, IOException {
        disseminateZipForMets(in, out, new FileFilterBuilder().build());
    }

    void disseminateZipForMets(
            InputStream metsInputStream,
            OutputStream zipOutputStream,
            FileFilter fileFilter) throws InvalidMETSDocument, IOException {

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

        List<DocumentFile> documentFiles = filterExclusiveMimetypes(
                getDocumentFiles(metsDocument, fileFilter),
                fileFilter);

        zip(documentFiles, zipOutputStream);
    }

    private List<DocumentFile> filterExclusiveMimetypes(List<DocumentFile> documentFiles, FileFilter fileFilter) {
        Set<String> presentMimetypes = new HashSet<>();
        for (DocumentFile documentFile : documentFiles) {
            presentMimetypes.add(documentFile.getContentType());
        }

        Set<String> exclusiveMimetypes = fileFilter.exclusiveMimetypes(presentMimetypes);
        if (exclusiveMimetypes.isEmpty()) {
            return documentFiles;
        } else {
            LinkedList<DocumentFile> result = new LinkedList<>();
            for (DocumentFile documentFile : documentFiles) {
                if (exclusiveMimetypes.contains(documentFile.getContentType())) {
                    result.add(documentFile);
                }
            }
            return result;
        }
    }

    private List<DocumentFile> getDocumentFiles(Document metsDocument, FileFilter fileFilter)
            throws InvalidMETSDocument {

        List<DocumentFile> documentFileList = new ArrayList<>();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(Namespaces.getPrefixUriMap());
        xpath.setNamespaceContext(namespaces);

        XPathExpression xPathExpr;
        NodeList nodeFLocat;
        try {
            xPathExpr = xpath.compile(ZipDisseminator.xPathFLocat);
            nodeFLocat = (NodeList) xPathExpr.evaluate(metsDocument, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            // if hard coded xpath is incorrect, the program is broken
            throw new RuntimeException(e);
        }

        try {
            for (int k = 0; k < nodeFLocat.getLength(); k++) {
                Element flocat = (Element) nodeFLocat.item(k);
                String href = flocat.getAttributeNS(Namespaces.XLIN.getURI(), "href");
                String title = flocat.getAttributeNS(Namespaces.XLIN.getURI(), "title");

                if (href.isEmpty() || title.isEmpty()) {
                    throw new InvalidMETSDocument("Cannot obtain content links from METS document: " + metsDocument.getDocumentURI());
                }

                Element file = (Element) flocat.getParentNode();
                String contentType = file.getAttribute("MIMETYPE");

                if (fileFilter.accepts(title, contentType)) {
                    title = fileFilter.transformName(title, contentType);

                    DocumentFile documentFile = new DocumentFile();
                    documentFile.setContentUrl(new URL(href));
                    documentFile.setTitle(title);
                    documentFile.setContentType(contentType);
                    documentFileList.add(documentFile);
                }
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

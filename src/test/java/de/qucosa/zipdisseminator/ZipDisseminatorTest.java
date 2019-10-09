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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class ZipDisseminatorTest {

    private ZipDisseminator disseminator;
    private PipedInputStream in;
    private PipedOutputStream out;

    @Before
    public void setup() throws IOException {
        in = new PipedInputStream();
        out = new PipedOutputStream(in);
        disseminator = new ZipDisseminator();
    }

    @After
    public void teardown() {
        close(out);
        close(in);
    }

    @Test
    public void METS_without_files_returns_empty_ZIP() throws Exception {
        String xml = "<mets:mets xmlns:mets=\"http://www.loc.gov/METS/\"/>";

        disseminator.disseminateZipForMets(stringAsStream(xml), out);

        ZipInputStream zis = new ZipInputStream(in);
        assertNull("ZIP should not contain entries", zis.getNextEntry());
    }

    @Test(expected = InvalidMETSDocument.class)
    public void File_elements_without_href_throws_exception() throws Exception {
        String xml = buildMetsXml("<file USE=\"ARCHIVE\"><FLocat xlink:title=\"some title\"/></file>");

        disseminator.disseminateZipForMets(stringAsStream(xml), out);
    }

    @Test(expected = InvalidMETSDocument.class)
    public void File_elements_without_title_throws_exception() throws Exception {
        String xml = buildMetsXml("<file USE=\"ARCHIVE\"><FLocat xlink:href=\"classpath:a.txt\"/></file>");

        disseminator.disseminateZipForMets(stringAsStream(xml), out);
    }

    @Test
    public void File_elements_without_USE_ARCHIVE_should_not_be_zipped() throws Exception {
        String xml = buildMetsXml("<file><FLocat xlink:href=\"classpath:a.txt\"/></file>");

        disseminator.disseminateZipForMets(stringAsStream(xml), out);

        ZipInputStream zis = new ZipInputStream(in);
        assertNull("ZIP should not contain entries", zis.getNextEntry());
    }

    @Test
    public void File_name_has_no_spaces_or_braces() throws Exception {
        String xml = buildMetsXml("<file USE=\"ARCHIVE\">" +
                "<FLocat xlink:href=\"classpath:a.txt\" xlink:title=\"(when you hear the words) brace brace\"/>" +
                "</file>");

        FileFilterBuilder fileFilterBuilder = new FileFilterBuilder()
            .replaceAll("[\\(\\)]", "")
            .replaceAll("\\s", "-");

        disseminator.disseminateZipForMets(stringAsStream(xml), out, fileFilterBuilder.build());

        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry zipEntry = zis.getNextEntry();
        String fileName = zipEntry.getName();

        assertThat("Filename should not contain space or braces.", fileName, not(matchesPattern("(.*)[\\(\\)\\s](.*)")));
    }

    @Test
    public void ZIP_contains_two_test_files() throws Exception {
        disseminator.disseminateZipForMets(new URL("classpath:minimal.mets.xml").openStream(), out);

        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry ze1 = zis.getNextEntry();
        ZipEntry ze2 = zis.getNextEntry();
        assertEquals("Plain text title A", ze1.getName());
        assertEquals("Plain text title B", ze2.getName());
    }

    @Test
    public void Appends_mimetype_specific_file_extensions_if_missing() throws Exception {
        String template = "<file USE=\"ARCHIVE\" MIMETYPE=\"%s\"><FLocat xlink:href=\"classpath:c\" xlink:title=\"%s\"/></file>";
        String xml = buildMetsXml(
                String.format(template, "text/html", "A"),
                String.format(template, "text/plain", "B"),
                String.format(template, "application/pdf", "C"),
                String.format(template, "application/pdf", "D.pdf"));

        FileFilterBuilder fileFilterBuilder = new FileFilterBuilder()
                .appendMissingFileExtension("text/html", "html")
                .appendMissingFileExtension("text/plain", "txt")
                .appendMissingFileExtension("application/pdf", "pdf");

        disseminator.disseminateZipForMets(stringAsStream(xml), out, fileFilterBuilder.build());

        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry ze1 = zis.getNextEntry();
        ZipEntry ze2 = zis.getNextEntry();
        ZipEntry ze3 = zis.getNextEntry();
        ZipEntry ze4 = zis.getNextEntry();
        assertEquals("A.html", ze1.getName());
        assertEquals("B.txt", ze2.getName());
        assertEquals("C.pdf", ze3.getName());
        assertEquals("D.pdf", ze4.getName());
    }

    @Test
    public void Filter_defined_file_names() throws Exception {
        String template = "<file USE=\"ARCHIVE\" MIMETYPE=\"%s\"><FLocat xlink:href=\"classpath:c\" xlink:title=\"%s\"/></file>";
        String xml = buildMetsXml(
                String.format(template, "text/plain", "Digitale Signatur"),
                String.format(template, "text/plain", "signatur.txt.asc"));

        FileFilterBuilder fileFilterBuilder = new FileFilterBuilder()
                .reject("text/plain", "Digitale Signatur")
                .reject("text/plain", "signatur.txt.asc");

        disseminator.disseminateZipForMets(stringAsStream(xml), out, fileFilterBuilder.build());

        ZipInputStream zis = new ZipInputStream(in);
        assertNull("No signature files should be includes in ZIP file.", zis.getNextEntry());
    }

    @BeforeClass
    static public void registerClasspathProtocolHandler() {
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                return "classpath".equals(protocol) ? new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL u) throws IOException {
                        ClassLoader classLoader = getClass().getClassLoader();
                        URL url = classLoader.getResource(u.getPath());
                        if (url != null) {
                            return url.openConnection();
                        } else {
                            throw new IOException("Resource not found: " + u.toString());
                        }
                    }
                } : null;
            }
        });
    }

    private String buildMetsXml(String... files) {
        StringBuilder sb = new StringBuilder()
                .append("<mets xmlns=\"http://www.loc.gov/METS/\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">")
                .append("<fileSec>")
                .append("<fileGrp USE=\"DOWNLOAD\">");

        for (String file : files) sb.append(file);

        sb.append("</fileGrp>")
                .append("</fileSec>")
                .append("</mets>");

        return sb.toString();
    }

    private InputStream stringAsStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes());
    }

    private void close(Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }
}

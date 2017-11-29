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

package de.qucosa.DisseminationTests;

import de.qucosa.zipdisseminator.ZipDisseminator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Closeable;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.assertEquals;

public class ZipDisseminatorTest {

    private PipedInputStream in;
    private PipedOutputStream out;

    @Before
    public void plumbing() throws IOException {
        in = new PipedInputStream();
        out = new PipedOutputStream(in);
    }

    @After
    public void dispose() {
        close(out);
        close(in);
    }

    @Test
    public void ZIP_contains_two_test_files() throws Exception {
        ZipDisseminator disseminator = new ZipDisseminator();

        disseminator.disseminateZipForMets(new URL("classpath:minimal.mets.xml").openStream(), out);

        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry ze1 = zis.getNextEntry();
        ZipEntry ze2 = zis.getNextEntry();
        assertEquals("Plain text title A", ze1.getName());
        assertEquals("Plain text title B", ze2.getName());
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

    private void close(Closeable c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }
}

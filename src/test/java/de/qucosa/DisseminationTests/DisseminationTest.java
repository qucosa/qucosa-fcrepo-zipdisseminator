package de.qucosa.DisseminationTests;

import de.qucosa.zipfiledisseminator.DisseminationServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ServletException;
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

import static org.junit.Assert.assertEquals;

public class DisseminationTest {

    private DisseminationServlet disseminationServlet;
    private InputStream minimalMets;
    private PipedInputStream sink;
    private PipedOutputStream source;

    @Before
    public void plumbing() throws IOException {
        sink = new PipedInputStream();
        source = new PipedOutputStream(sink);
    }

    @Before
    public void prepareServlet() throws ServletException {
        disseminationServlet = new DisseminationServlet();
        disseminationServlet.init();
    }

    @After
    public void shutdownServlet() {
        disseminationServlet.destroy();
    }

    @Before
    public void prepareMinimalMets() throws IOException {
        minimalMets = new URL("classpath:minimal.mets.xml").openStream();
    }

    @After
    public void disposeMinimalMets() {
        try {
            minimalMets.close();
        } catch (IOException ignored) {
        }
    }

    @Test
    public void ZIP_contains_two_test_files() throws Exception {
        disseminationServlet.disseminateZip(minimalMets, source);

        ZipInputStream zis = new ZipInputStream(sink);
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
}

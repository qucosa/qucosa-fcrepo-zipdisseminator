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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;

public class DisseminationServlet extends HttpServlet {

    private static final String REQUEST_PARAM_METS_URL = "metsurl";
    private static final String REQUEST_PARAM_FILENAME_FILTERING = "xmdpfilter";
    private static final String zipFileName = "content.zip";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private CloseableHttpClient httpClient;

    @Override
    public void init() {
        httpClient = HttpClientBuilder.create()
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {

            URI metsDocumentURI;
            try {
                String metsRequestParam = getRequestParameterValue(req, REQUEST_PARAM_METS_URL, true);
                metsDocumentURI = new URL(metsRequestParam).toURI();
            } catch (MissingRequiredParameter | URISyntaxException e) {
                sendError(resp, SC_BAD_REQUEST, e.getMessage());
                return;
            }

            boolean xmdpFilenameFiltering =
                    Boolean.parseBoolean(getRequestParameterValue(req, REQUEST_PARAM_FILENAME_FILTERING, false));

            final FileFilterBuilder fileFilterBuilder = new FileFilterBuilder();
            if (xmdpFilenameFiltering) {
                fileFilterBuilder
                        .replaceAll("[\\(\\)]", "")
                        .replaceAll("\\s", "-")
                        .reject("text/plain", "Digitale Signatur")
                        .reject("text/plain", "signatur.txt.asc")
                        .appendMissingFileExtension("text/html", "html")
                        .appendMissingFileExtension("text/plain", "txt")
                        .appendMissingFileExtension("application/pdf", "pdf")
                        .appendMissingFileExtension("application/postscript", "ps")
                        .exclusiveMimeTypeIfPresent("application/pdf");
            }
            final FileFilter fileFilter = fileFilterBuilder.build();

            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(metsDocumentURI))) {

                if (SC_OK == response.getStatusLine().getStatusCode()) {

                    resp.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName + "\"");
                    resp.setContentType("application/zip");

                    ZipDisseminator zipDisseminator = new ZipDisseminator();
                    zipDisseminator.disseminateZipForMets(
                            response.getEntity().getContent(),
                            resp.getOutputStream(),
                            fileFilter);

                    resp.setStatus(SC_OK);
                } else {
                    sendError(resp, SC_NOT_FOUND, "Cannot obtain METS document: " + metsDocumentURI.toASCIIString());
                }

            }

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

    private String getRequestParameterValue(ServletRequest request, String name, boolean required) throws MissingRequiredParameter {
        final String v = request.getParameter(name);
        if (required && (v == null || v.isEmpty())) {
            throw new MissingRequiredParameter("Missing parameter '" + REQUEST_PARAM_METS_URL + "'");
        }
        return v;
    }

}

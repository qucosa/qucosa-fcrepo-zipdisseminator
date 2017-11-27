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

import java.net.URL;

public class DocumentFile {
    private URL contentUrl;
    private boolean isUseArchive;
    private String checksumType;
    private String checksum;
    private String title;

    public URL getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(URL contentUrl) {
        this.contentUrl = contentUrl;
    }

    public boolean isUseArchive() {
        return isUseArchive;
    }

    public void setUseArchive(boolean useArchive) {
        isUseArchive = useArchive;
    }

    public String getChecksumType() {
        return checksumType;
    }

    public void setChecksumType(String checksumType) {
        this.checksumType = checksumType;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


}

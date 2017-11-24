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

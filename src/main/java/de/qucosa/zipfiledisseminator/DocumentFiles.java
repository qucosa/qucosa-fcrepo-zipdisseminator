package de.qucosa.zipfiledisseminator;

public class DocumentFiles {
    private String contentUrl;
    private boolean isUseArchive;
    private String checksumType;
    private String checksum;
    private String title;


    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
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

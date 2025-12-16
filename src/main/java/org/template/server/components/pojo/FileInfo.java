package org.template.server.components.pojo;

public class FileInfo {
    private long fileSize;

    private int fileChunks;

    private String uuid;

    public long getFileSize() {
        return fileSize;
    }

    public int getFileChunks() {
        return fileChunks;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFileName() {
        return fileName;
    }

    private String fileName;

    public FileInfo(long fileSize,int fileChunks,String uuid,String fileName){
        this.fileChunks = fileChunks;
        this.fileSize  =fileSize;
        this.uuid  = uuid;
        this.fileName = fileName;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileChunks(int fileChunks) {
        this.fileChunks = fileChunks;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}

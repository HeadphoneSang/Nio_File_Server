package org.template.server.components.handlers;

import org.template.server.components.HandlerContext;
import org.template.server.components.WritePromise;
import org.template.server.components.pojo.BufferPack;
import org.template.server.components.pojo.FileInfo;
import org.template.server.components.pojo.ObjPack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class FileUploadHandler extends SimpleHandler<BufferPack, ObjPack>{

    private final HashMap<String,FileInfo> UUID_Map = new HashMap<>();

    private final File saveDir;

    public File getSaveDir() {
        return saveDir;
    }

    public FileUploadHandler(String savePath){
        File savePathFile = new File(savePath);
        savePathFile.mkdirs();
        this.saveDir = savePathFile;
    }

    public FileUploadHandler(){
        this("./saved");
    }

    public boolean createFileIfNot(File tmpFile){
        if(tmpFile.exists())
            return false;
        try {
            return tmpFile.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 注册一个新的上传文件通道，记录这个文件的UUID，然后同时创建这个文件的本地对应文件：fileName.tmp
     * 如果这个fileName的文件已经存在了，返回false
     */
    public boolean registerFileUpload(FileInfo fileInfo){
        if (UUID_Map.containsKey(fileInfo.getUuid()))
            return false;
        File tarFile = new File(saveDir,fileInfo.getFileName());
        if (tarFile.exists())
            return false;
        String fileName = fileInfo.getFileName() + ".tmp";
        fileInfo.setFileName(fileName);
        UUID_Map.put(fileInfo.getUuid(),fileInfo);
        File tmpFile = new File(saveDir,fileName);
        boolean isNotExist = this.createFileIfNot(tmpFile);
        if(!isNotExist&&tmpFile.length()!=0){
            //文件上传过，但是没有上传完
            fileInfo.setFileSize(tmpFile.length()*-1);
            return false;
        }
        return isNotExist;
    }

    @Override
    public void write(HandlerContext ctx, WritePromise promise, ObjPack msg) {

    }

    @Override
    public void onRemoved(HandlerContext ctx) {

    }

    @Override
    public void onAdded(HandlerContext ctx) {
        ctx.enableWrite();
        ctx.enableRead();
    }

    @Override
    public void onDestroy(HandlerContext ctx) {

    }

    @Override
    public void channelRead0(HandlerContext ctx, BufferPack msg) {

    }
}

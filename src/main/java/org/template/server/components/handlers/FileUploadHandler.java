package org.template.server.components.handlers;

import org.template.server.components.internals.HandlerContext;
import org.template.server.components.internals.WritePromise;
import org.template.server.components.pojo.*;
import org.template.server.components.internals.pojo.FileReceiveContext;
import org.template.server.utils.EncodeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class FileUploadHandler extends SimpleHandler<FileChunkPack, ObjPack>{

    private final HashMap<String, FileReceiveContext> UUID_Map = new HashMap<>();

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
    public boolean registerFileUpload(FileReceiveContext fileReceiveContext){
        if (UUID_Map.containsKey(fileReceiveContext.getUuid()))  //正在上传
            return false;
        File tarFile = new File(saveDir, fileReceiveContext.getFileName());
        if (tarFile.exists())  //文件已经上传过了
            return false;
        if (FileReceiveContext.hasPersisted(fileReceiveContext.getUuid())){//上传过但是没有完
            fileReceiveContext.setFileSize(-1);
            unregisterFileUpload(fileReceiveContext);
            return false;
        }
        String tmpFileName = fileReceiveContext.getFileName() + ".tmp";
        UUID_Map.put(fileReceiveContext.getUuid(), fileReceiveContext);
        File tmpFile = new File(saveDir,tmpFileName);
        fileReceiveContext.setFileName(tmpFile.getName());
        this.createFileIfNot(tmpFile);
        initChannel(fileReceiveContext, tmpFile);
        return true;
    }

    private void initChannel(FileReceiveContext fileReceiveContext, File tmpFile) {
        fileReceiveContext.setTargetFile(tmpFile);
        fileReceiveContext.setTargetPath(Paths.get(tmpFile.toURI()));
        Path path = fileReceiveContext.getTargetPath();
        try {
            FileChannel fc = FileChannel.open(path, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            fileReceiveContext.setChannel(fc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void editFileUpload(FileReceiveContext fileReceiveContext){
        String fileName = fileReceiveContext.getFileName() + ".tmp";
        File tmpFile = new File(saveDir,fileName);
        fileReceiveContext.setFileName(tmpFile.getName());
        UUID_Map.put(fileReceiveContext.getUuid(), fileReceiveContext);
        initChannel(fileReceiveContext, tmpFile);
    }

    public boolean remTmpFile(FileReceiveContext fileReceiveContext){
        String fileName = fileReceiveContext.getFileName() +".tmp";
        File tmpFile = new File(saveDir,fileName);
        return tmpFile.delete();
    }

    public void unregisterFileUpload(FileReceiveContext fileReceiveContext){
        UUID_Map.remove(fileReceiveContext.getUuid());
        if (fileReceiveContext.getChannel()!=null)
            try{
                fileReceiveContext.close();
            }catch (IOException e){
                throw new RuntimeException(e);
            }
    }

    @Override
    public void write(HandlerContext ctx, WritePromise promise, ObjPack msg) {

    }

    @Override
    public void onRemoved(HandlerContext ctx) {

    }

    @Override
    public void onAdded(HandlerContext ctx) {
        ctx.disableWrite();
        ctx.enableRead();
    }

    @Override
    public void onDestroy(HandlerContext ctx) {
        for (FileReceiveContext context : UUID_Map.values())
            context.persistState();
        ctx.getLogger().info("持久化完成: "+UUID_Map.size());
    }

    @Override
    public void channelRead0(HandlerContext ctx, FileChunkPack fileChunk) {
        String uuid = fileChunk.getUuid();
        FileReceiveContext context = UUID_Map.get(uuid);
        try {
            ByteBuffer data = fileChunk.getData();
            data.mark();
            String localSha256 = EncodeUtils.sha256(data);
            data.reset();
            String remoteSha256 = fileChunk.getSha256();
            if (!localSha256.equals(remoteSha256)){
                ctx.executeTask((ctx1)->{
                    BufferPack rePack;
                    if (context.write(fileChunk)){
                        if (context.complete()) {
                            rePack = new UploadCompletePack(fileChunk);
                            context.flushMetaInfo();
                            unregisterFileUpload(context);
                        }
                        else
                            rePack = new UploadChunkAckPack(fileChunk);
                    }
                    else
                        rePack = new UploadChunkFinPack(fileChunk, UploadChunkFinPack.FinCode.CHUNK_EXISTED);
                    ctx1.writeAndFlush(rePack);
                });
            }else{
                BufferPack pack = new UploadChunkFinPack(fileChunk, UploadChunkFinPack.FinCode.CHUNK_INVALID);
                ctx.writeAndFlush(pack);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}

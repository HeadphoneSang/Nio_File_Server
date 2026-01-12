package org.template.server.components.internals.pojo;

import org.template.server.components.pojo.FileChunkPack;
import org.template.server.utils.EncodeUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

public class FileReceiveContext {

    private long fileSize;

    private int chunkSize;

    private int totalChunks;

    private final String uuid;

    private FileChannel channel;

    private String fileName;

    private BitSet receivedChunks;

    private File targetFile;

    private Path targetPath;

    private AtomicInteger curChunkPage = new AtomicInteger(0);

    private static final File persistDir;

    private static final HashSet<String> persistUuidsSet = new HashSet<>();

    static {
        Path rootPath = Paths.get(System.getProperty("user.dir"));
        persistDir = new File(rootPath.toAbsolutePath().resolve("state_caches").toString());
        loadPersist_Set();
    }
    static void loadPersist_Set(){
        File[] objs = persistDir.listFiles(((dir, name) -> name.endsWith(".obj")));
        if (objs == null)
            return;
        for (File objFile :objs){
            persistUuidsSet.add(objFile.getName().substring(0,objFile.getName().lastIndexOf(".")));
        }
    }

    public static void addPersist(String uuid){
        persistUuidsSet.add(uuid);
    }

    /**
     * 检查这个uuid是否持久化过
     * @param uuid id
     * @return 返回是否持久化过
     */
    public static boolean hasPersisted(String uuid){
        return persistUuidsSet.contains(uuid);
    }

    public void close() throws IOException {
        if (channel != null) {
            try {
                channel.force(true);  // 强制将缓冲区数据写入磁盘
            } finally {
                channel.close();
                channel = null;  // 清空引用，帮助GC回收
            }
        }
    }
    public FileChannel getChannel() {
        return channel;
    }

    public void setChannel(FileChannel channel) {
        this.channel = channel;
    }

    public File getTargetFile() {
        return targetFile;
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
    }

    public Path getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(Path targetPath) {
        this.targetPath = targetPath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public int totalChunks() {
        return totalChunks;
    }

    public String getUuid() {
        return uuid;
    }

    public String getFileName() {
        return fileName;
    }

    public FileReceiveContext(long fileSize,int chunkSize, int totalChunks, String uuid, String fileName){
        this.totalChunks = totalChunks;
        this.chunkSize = chunkSize;
        this.fileSize  =fileSize;
        this.uuid  = uuid;
        this.fileName = fileName;
        this.receivedChunks = new BitSet();
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * 向目标文件写入当前chunk，如果这个chunk没有写入且写入成功，返回true，否则返回false
     * @param pack 当前chunk的信息
     * @return 返回是否写入成功
     */
    public boolean write(FileChunkPack pack){
        int chunkId = pack.getChunkId();
        if (receivedChunks.get(chunkId))
            return false;
        long offset = (long) chunkId * this.chunkSize;
        ByteBuffer data = pack.getData();
        data.mark();
        String localSha256 = null;
        try {
            localSha256 = EncodeUtils.sha256(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        data.reset();
        try {
            channel.position(offset);
            while (data.hasRemaining())
                channel.write(data);
            receivedChunks.set(chunkId);
            if (this.receivedChunks.cardinality() % 5 == curChunkPage.get()){
                channel.force(false);
                curChunkPage.incrementAndGet();
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断当前文件是否上传完毕
     * @return 返回结果
     */
    public boolean complete(){
        return this.receivedChunks.cardinality() == this.totalChunks;
    }

    /**
     * 将原始信息写回磁盘，同时将原名更改
     */
    public void flushMetaInfo(){
        try {
            channel.force(true);  // 先强制刷入磁盘
            String tmpAbsPath = this.targetFile.getAbsolutePath();
            Path metaFile = Paths.get(tmpAbsPath.substring(0,tmpAbsPath.lastIndexOf(".")));
            Files.move(this.targetPath,metaFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 将当前元数据持久化到磁盘文件，id为uuid
     */
    public void persistState(){
        if (!persistDir.exists())
            persistDir.mkdirs();
        Path perFile = new File(persistDir,uuid+".obj").toPath();
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(Files.newOutputStream(perFile)))) {
            out.writeInt(totalChunks);  //4
            out.writeInt(chunkSize);  //4
            out.writeLong(fileSize);  //8
            byte[] bits = receivedChunks.toByteArray();
            out.writeInt(bits.length);  //4
            out.write(bits);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将当前obj的uuid对应的持久化文件载入到内存当中
     * @return 返回是否载入成功 (失败：根目录为空or目标文件不存在或被占用)
     */
    public static FileReceiveContext loadFromUUID(String uuid) {
        if (!persistDir.exists()) {
            persistDir.mkdirs();
            return null;
        }
        FileReceiveContext ctx = new FileReceiveContext(0,0,0,uuid,"");
        Path perFile = new File(persistDir,uuid+".obj").toPath();
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(perFile)))) {
            ctx.totalChunks = in.readInt();
            ctx.chunkSize = in.readInt();
            ctx.fileSize = in.readLong();
            int bitLen = in.readInt();
            byte[] bits = new byte[bitLen];
            in.readFully(bits);
            ctx.receivedChunks = BitSet.valueOf(bits);
        } catch (IOException e) {
            return null;
        }
        return ctx;
    }

    /**
     * 从目标上下文复制状态到当前上下文
     * @param context 目标上下文
     * @return 返回是否复制成功，失败则由于目标与当前元信息不匹配
     */
    public boolean copyStateFrom(FileReceiveContext context){
        if (context.totalChunks!=this.totalChunks)
            return false;
        this.receivedChunks = context.receivedChunks;
        this.curChunkPage = context.curChunkPage;
        return true;
    }

    /**
     * 获取当前已接收数据块状态的位图字节数组
     *
     * @return BitSet转换的字节数组，表示哪些数据块已经被成功接收
     *         字节数组采用小端序格式存储位图信息
     */
    public byte[] getBitMap(){
        return receivedChunks.toByteArray();
    }


    /**
     * 删除自己的持久化记录
     */
    public void remPersist(){
        persistUuidsSet.remove(getUuid());
        File perFile = new File(persistDir,getUuid()+".obj");
        synchronized (this){
            if (!perFile.exists())
                return;
            try {
                Files.delete(perFile.toPath());

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

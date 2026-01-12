import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileComparisonTest {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 比较两个文件内容是否完全一致
     * @param filePath1 第一个文件路径
     * @param filePath2 第二个文件路径
     * @return 如果文件内容一致返回true，否则返回false
     */
    public static boolean compareFilesByContent(String filePath1, String filePath2) {
        try (FileInputStream fis1 = new FileInputStream(filePath1);
             FileInputStream fis2 = new FileInputStream(filePath2);
             FileChannel channel1 = fis1.getChannel();
             FileChannel channel2 = fis2.getChannel()) {

            // 首先比较文件大小
            long size1 = channel1.size();
            long size2 = channel2.size();
            if (size1 != size2) {
                System.out.println("文件大小不一致: " + filePath1 + " (" + size1 + ") vs " +
                        filePath2 + " (" + size2 + ")");
                return false;
            }

            // 分块比较内容
            long position = 0;
            ByteBuffer buffer1 = ByteBuffer.allocate(BUFFER_SIZE);
            ByteBuffer buffer2 = ByteBuffer.allocate(BUFFER_SIZE);

            while (position < size1) {
                buffer1.clear();
                buffer2.clear();

                long remaining = size1 - position;
                int readSize = (int) Math.min(BUFFER_SIZE, remaining);

                // 读取第一文件
                channel1.position(position);
                int bytesRead1 = channel1.read(buffer1);
                buffer1.flip();

                // 读取第二文件
                channel2.position(position);
                int bytesRead2 = channel2.read(buffer2);
                buffer2.flip();

                // 比较读取的字节数
                if (bytesRead1 != bytesRead2) {
                    return false;
                }

                // 比较缓冲区内容
                if (!buffer1.equals(buffer2)) {
                    return false;
                }

                position += readSize;
            }

            return true;

        } catch (IOException e) {
            System.err.println("比较文件时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 通过SHA256哈希值比较两个文件内容是否一致
     * @param filePath1 第一个文件路径
     * @param filePath2 第二个文件路径
     * @return 如果文件内容一致返回true，否则返回false
     */
    public static boolean compareFilesByHash(String filePath1, String filePath2) {
        try {
            String hash1 = calculateSHA256(filePath1);
            String hash2 = calculateSHA256(filePath2);

            System.out.println("文件1 SHA256: " + hash1);
            System.out.println("文件2 SHA256: " + hash2);

            return hash1.equals(hash2);
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("计算文件哈希时发生错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 计算文件的SHA256哈希值
     */
    private static String calculateSHA256(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (FileInputStream fis = new FileInputStream(filePath);
             FileChannel channel = fis.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while (channel.read(buffer) > 0) {
                buffer.flip();
                digest.update(buffer);
                buffer.clear();
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * 主方法，用于测试
     */
    public static void main(String[] args) {
//        if (args.length != 2) {
//            System.out.println("使用方法: java FileComparisonTest <file1_path> <file2_path>");
//            return;
//        }

        String file1 = "D:\\pro\\JavaProject\\NIO-Server-template\\src\\main\\java\\stop_beauty.pdf.tmp";
        String file2 = "D:\\pro\\JavaProject\\NIO-Server-template\\src\\main\\java\\stop_beauty-o.pdf";

        System.out.println("开始比较文件: " + file1 + " 和 " + file2);

        // 内容比较
        boolean contentMatch = compareFilesByContent(file1, file2);
        System.out.println("内容比较结果: " + (contentMatch ? "一致" : "不一致"));

        // 哈希比较
        boolean hashMatch = compareFilesByHash(file1, file2);
        System.out.println("哈希比较结果: " + (hashMatch ? "一致" : "不一致"));

        System.out.println("总体比较结果: " + (contentMatch && hashMatch ? "文件完全一致" : "文件不一致"));
    }
}

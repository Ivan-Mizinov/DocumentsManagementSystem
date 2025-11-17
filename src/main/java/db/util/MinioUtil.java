package db.util;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MinioUtil {
    private static MinioClient minioClient;
    private static final String bucketName = "avatars";
    private static final String ENDPOINT = "http://localhost:9000";
    private static final String ACCESS_KEY = "admin";
    private static final String SECRET_KEY = "admin123";
    private static volatile MinioUtil instance;

    private MinioUtil() {
    }

    public static MinioUtil getInstance() {
        if (instance == null) {
            synchronized (MinioUtil.class) {
                if (instance == null) {
                    instance = new MinioUtil();
                    init();
                }
            }
        }
        return instance;
    }

    public static void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(ENDPOINT)
                    .credentials(ACCESS_KEY, SECRET_KEY)
                    .build();

            createBucketIfNotExists();
        } catch (RuntimeException e) {
            throw new RuntimeException("Ошибка инициализации MinIO: " + e.getMessage());
        }
    }

    private static void createBucketIfNotExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build());

            if (found) {
                System.out.println("Bucket " + bucketName + " already exists");
            } else {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
                System.out.println("Bucket " + bucketName + " created");
            }
        } catch (MinioException | InvalidKeyException | IOException | NoSuchAlgorithmException e) {
            System.err.println("Ошибка при проверке/создании бакета: " + e.getMessage());
        }
    }

    public void uploadAvatar(Long userId, InputStream inputStream, String contentType, String objectKey) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, inputStream.available(), -1)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка загрузки аватара: " + e.getMessage(), e);
        }
    }

    public static String getObjectUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.valueOf("GET"))
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения URL объекта: " + e.getMessage(), e);
        }
    }

    public void deleteAvatar(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            System.err.println("Ошибка удаления аватара: " + e.getMessage());
        }
    }

    public static boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getPresignedUrl(String objectKey, int expiresInSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectKey)
                    .expiry(expiresInSeconds)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка получения URL: " + e.getMessage(), e);
        }
    }
}

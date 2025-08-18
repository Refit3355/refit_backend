package com.refit.app.infra.file.s3;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region}")
    private String region;

    /**
     * 프로필 이미지 업로드 후 공개 URL 반환 (경로: profile/<UUID>.<ext>)
     */
    public String uploadProfile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            String original = file.getOriginalFilename();
            String ext = (original != null) ? FilenameUtils.getExtension(original) : null;

            if (ext == null || ext.isBlank()) {
                String guessed = URLConnection.guessContentTypeFromStream(file.getInputStream());
                if (guessed != null && guessed.toLowerCase(Locale.ROOT).contains("png")) {
                    ext = "png";
                } else if (guessed != null && guessed.toLowerCase(Locale.ROOT).contains("jpeg")) {
                    ext = "jpg";
                } else if (guessed != null && guessed.toLowerCase(Locale.ROOT).contains("gif")) {
                    ext = "gif";
                } else {
                    ext = "bin";
                }
            }

            String key = "profile/" + UUID.randomUUID() + "." + ext.toLowerCase(Locale.ROOT);

            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ) // 버킷 정책에 따라 무시될 수 있음
                    .build();

            s3.putObject(put, RequestBody.fromBytes(file.getBytes()));

            return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        } catch (IOException e) {
            throw new IllegalStateException("프로필 이미지 업로드 실패", e);
        }
    }
}

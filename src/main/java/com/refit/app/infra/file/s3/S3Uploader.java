package com.refit.app.infra.file.s3;

//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//import zippick.global.exception.ErrorCode;
//import zippick.global.exception.ZippickException;
//
//@Component
//@RequiredArgsConstructor
//public class S3Uploader {
//
//    private final S3Client s3Client;
//
//    @Value("${cloud.aws.s3.bucket}")
//    private String bucketName;
//
//    public String upload(String folder, MultipartFile file) {
//        try {
//            String key = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
//
//            PutObjectRequest putRequest = PutObjectRequest.builder()
//                    .bucket(bucketName)
//                    .key(key)
//                    .contentType(file.getContentType())
//                    .build();
//
//            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
//
//            return "https://" + bucketName + ".s3.ap-northeast-2.amazonaws.com/" + key;
//        } catch (IOException e) {
//            throw new ZippickException(ErrorCode.FILE_UPLOAD_FAIL, "S3 파일 업로드 실패: " + e.getMessage());
//        }
//    }
//}

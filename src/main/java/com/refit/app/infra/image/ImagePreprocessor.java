package com.refit.app.infra.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public final class ImagePreprocessor {

    private ImagePreprocessor() {
    }

    // ↓↓↓ 조정 포인트
    private static final int MAX_DIM = 720;
    private static final float JPEG_QUALITY = 0.6f;
    private static final boolean ALWAYS_RECOMPRESS = true;

    public static byte[] preprocess(byte[] imageBytes) {
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (src == null) {
                return imageBytes;
            }

            int orientation = readOrientationSafe(imageBytes);
            BufferedImage oriented = applyOrientation(src, orientation);

            BufferedImage scaled = scaleDownIfNeeded(oriented, MAX_DIM);

            // 텍스트 대비 강화(얇게만)
            BufferedImage enhanced = enhanceForText(scaled);

            return writeJpeg(enhanced, JPEG_QUALITY);
        } catch (Exception e) {
            // 실패 시에도 최소한 JPEG 보장
            try {
                return ensureJpeg(imageBytes);
            } catch (Exception ignore) {
                return imageBytes;
            }
        }
    }

    private static byte[] ensureJpeg(byte[] imageBytes) throws Exception {
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (src == null) {
            return imageBytes;
        }
        // 항상 JPEG 재인코딩(용량 안정화)
        return writeJpeg(convertToRGB(src), JPEG_QUALITY);
    }

    private static BufferedImage scaleDownIfNeeded(BufferedImage src, int maxDim) {
        int w = src.getWidth();
        int h = src.getHeight();
        int max = Math.max(w, h);
        if (max <= maxDim) {
            return convertToRGB(src);
        }
        double scale = (double) maxDim / (double) max;
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();
        return out;
    }

    private static BufferedImage convertToRGB(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    private static int readOrientationSafe(byte[] bytes) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(bytes));
            ExifIFD0Directory dir = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (dir != null && dir.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                return dir.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            }
        } catch (Exception ignore) {
        }
        return 1;
    }

    private static BufferedImage applyOrientation(BufferedImage img, int orientation) {
        AffineTransform tx = new AffineTransform();
        int w = img.getWidth(), h = img.getHeight();
        boolean transformNeeded = true;

        switch (orientation) {
            case 1 -> transformNeeded = false;
            case 2 -> {
                tx.scale(-1, 1);
                tx.translate(-w, 0);
            }
            case 3 -> {
                tx.translate(w, h);
                tx.rotate(Math.PI);
            }
            case 4 -> {
                tx.scale(1, -1);
                tx.translate(0, -h);
            }
            case 5 -> {
                tx.rotate(Math.PI / 2);
                tx.scale(1, -1);
            }
            case 6 -> {
                tx.translate(h, 0);
                tx.rotate(Math.PI / 2);
            }
            case 7 -> {
                tx.translate(h, 0);
                tx.rotate(Math.PI / 2);
                tx.scale(-1, 1);
            }
            case 8 -> {
                tx.translate(0, w);
                tx.rotate(-Math.PI / 2);
            }
            default -> transformNeeded = false;
        }

        if (!transformNeeded) {
            return convertToRGB(img);
        }

        int newW = (orientation >= 5 && orientation <= 8) ? h : w;
        int newH = (orientation >= 5 && orientation <= 8) ? w : h;
        BufferedImage out = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, tx, null);
        g.dispose();
        return out;
    }

    private static BufferedImage enhanceForText(BufferedImage src) {
        // 가벼운 그레이 + 히스토그램 스트레치 (CPU 가벼움)
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        Raster r = gray.getRaster();
        int w = gray.getWidth(), h = gray.getHeight();
        int min = 255, max = 0;
        int[] px = new int[1];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                r.getPixel(x, y, px);
                int v = px[0];
                if (v < min) {
                    min = v;
                }
                if (v > max) {
                    max = v;
                }
            }
        }
        if (max > min) {
            WritableRaster wr = gray.getRaster();
            double scale = 255.0 / (max - min);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    wr.getPixel(x, y, px);
                    int v = (int) Math.round((px[0] - min) * scale);
                    px[0] = (v < 0) ? 0 : (Math.min(255, v));
                    wr.setPixel(x, y, px);
                }
            }
        }
        return gray;
    }

    private static byte[] writeJpeg(BufferedImage img, float quality) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(96 * 1024);
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        writer.setOutput(new MemoryCacheImageOutputStream(baos));
        writer.write(null, new IIOImage(img, null, null), param);
        writer.dispose();
        return baos.toByteArray();
    }
}

package com.github.ws_ncip_pnpki.service;

import com.github.ws_ncip_pnpki.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {


    @Value("${file.upload.dir}")
    private String uploadDir;


    public String storeFile(MultipartFile file, Long userId, String subFolder) throws IOException {
        Path uploadPath = Paths.get(uploadDir, String.valueOf(userId), subFolder);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";

        String fileName = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File stored successfully: {}", filePath);
        return userId + "/" + subFolder + "/" + fileName;
    }

    public String storeFileV2(MultipartFile file, Long userId, String subFolder) throws IOException {

        Path uploadPath = Paths.get(uploadDir, String.valueOf(userId), subFolder);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                : "png";

        String newFileName = UUID.randomUUID() + "." + fileExtension;
        Path filePath = uploadPath.resolve(newFileName);

        // Handle PNG files - preserve original size and transparency
        if ("png".equals(fileExtension)) {
            BufferedImage original = ImageIO.read(file.getInputStream());

            // Ensure transparency is preserved/added
            BufferedImage transparentImage = ensureTransparency(original);

            // Save PNG with transparency
            ImageIO.write(transparentImage, "png", filePath.toFile());
        } else {
            // Save other file types normally
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Image file stored successfully: {}", filePath);

        return userId + "/" + subFolder + "/" + newFileName;
    }


    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(uploadDir, filePath);

        if (Files.exists(path)) {
            Files.delete(path);
            log.info("File deleted successfully: {}", path);
        }
    }

    public Path getFilePath(String filePath) {
        return Paths.get(uploadDir, filePath);
    }

    public File getFile(String filePath) {
        Path path = Paths.get(uploadDir, filePath);
        return path.toFile();
    }

    public boolean fileExists(String filePath) {
        return Files.exists(Paths.get(uploadDir, filePath));
    }

    public Resource loadFileAsResource(String filePath) {
        try {
            Path fullPath = Paths.get(uploadDir).resolve(filePath).normalize();
            Resource resource = new UrlResource(fullPath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filePath);
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found: " + filePath, ex);
        }
    }



    /**
     * Ensures the image has a transparent background.
     * If the image already has transparency, it's preserved.
     * If not, converts white/near-white pixels to transparent.
     */
    private BufferedImage ensureTransparency(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        // Create image with alpha channel
        BufferedImage transparent = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transparent.createGraphics();

        // High-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw original image
        g2d.drawImage(source, 0, 0, null);
        g2d.dispose();

        // Convert white/light pixels to transparent (optional - uncomment if needed)
        transparent = convertWhiteToTransparent(transparent, 240); // threshold: 240

        return transparent;
    }

    /**
     * Converts white or near-white pixels to transparent.
     *
     * @param image The source image
     * @param threshold RGB threshold (0-255). Pixels with R, G, B all >= threshold become transparent
     * @return Image with white pixels converted to transparent
     */
    private BufferedImage convertWhiteToTransparent(BufferedImage image, int threshold) {
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = image.getRGB(x, y);

                int alpha = (pixel >> 24) & 0xff;
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = pixel & 0xff;

                // If pixel is white/near-white, make it transparent
                if (red >= threshold && green >= threshold && blue >= threshold) {
                    result.setRGB(x, y, 0x00FFFFFF); // Fully transparent white
                } else {
                    result.setRGB(x, y, pixel); // Keep original pixel
                }
            }
        }

        return result;
    }
}

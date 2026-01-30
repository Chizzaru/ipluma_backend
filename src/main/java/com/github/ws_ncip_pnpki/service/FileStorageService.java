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

    public String storeFile(MultipartFile file, Long userId, String subFolder,
                            int targetWidth, int targetHeight) throws IOException {

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

        // If it's a PNG → resize WITH transparency
        if ("png".equals(fileExtension)) {
            BufferedImage original = ImageIO.read(file.getInputStream());

            // Resize while preserving transparency
            BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            // Save resized PNG
            ImageIO.write(resized, "png", filePath.toFile());
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
}

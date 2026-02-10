package com.github.ws_ncip_pnpki.validation;


import org.springframework.web.multipart.MultipartFile;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;

public class FileTypeValidator implements ConstraintValidator<ValidFileType, MultipartFile> {

    private List<String> allowedExtensions;
    private List<String> allowedMimeTypes;
    private boolean required;

    @Override
    public void initialize(ValidFileType constraintAnnotation) {
        this.allowedExtensions = Arrays.asList(constraintAnnotation.allowedExtensions());
        this.allowedMimeTypes = Arrays.asList(constraintAnnotation.allowedMimeTypes());
        this.required = constraintAnnotation.required();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        // If file is not required and not provided, it's valid
        if (!required && (file == null || file.isEmpty())) {
            return true;
        }

        // If file is required and not provided, it's invalid
        if (required && (file == null || file.isEmpty())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("File is required")
                    .addConstraintViolation();
            return false;
        }

        // Validate file name
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Invalid file name")
                    .addConstraintViolation();
            return false;
        }

        // Validate extension
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            String.format("Allowed file types: %s",
                                    String.join(", ", allowedExtensions)))
                    .addConstraintViolation();
            return false;
        }

        // Validate MIME type if specified
        if (!allowedMimeTypes.isEmpty() && file.getContentType() != null) {
            String mimeType = file.getContentType().toLowerCase();
            if (!allowedMimeTypes.contains(mimeType)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                                String.format("Invalid MIME type. Received: %s", mimeType))
                        .addConstraintViolation();
                return false;
            }
        }

        return true;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : filename.substring(lastDotIndex + 1);
    }
}
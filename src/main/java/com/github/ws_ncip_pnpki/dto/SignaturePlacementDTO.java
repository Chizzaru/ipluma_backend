package com.github.ws_ncip_pnpki.dto;

import com.github.ws_ncip_pnpki.service.PdfSigningService;

// Create this class in your controller or DTO package
public class SignaturePlacementDTO {
    private int pageNumber;
    private float x;
    private float y;
    private float width;
    private float height;
    private float rotation = 0; // Default value

    // Getters and setters
    public int getPageNumber() { return pageNumber; }
    public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }

    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }

    public float getRotation() { return rotation; }
    public void setRotation(float rotation) { this.rotation = rotation; }

    // Convert to service's SignaturePlacement
    public PdfSigningService.SignaturePlacement toSignaturePlacement() {
        return new PdfSigningService.SignaturePlacement(
                pageNumber, x, y, width, height, rotation
        );
    }
}
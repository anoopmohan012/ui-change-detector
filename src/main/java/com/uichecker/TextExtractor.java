package com.uichecker;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.image.BufferedImage;

public class TextExtractor {

    private static final ITesseract tesseract;

    static {
        tesseract = new Tesseract();
        tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
        tesseract.setLanguage("eng");
    }

    public static String extract(BufferedImage image) {
        try {
            return tesseract.doOCR(image).trim();
        } catch (TesseractException e) {
            return "";
        }
    }
}
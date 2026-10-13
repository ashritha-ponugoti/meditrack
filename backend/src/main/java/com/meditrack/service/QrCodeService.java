package com.meditrack.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates a QR code per batch that encodes the batch's id. Scanning it on
 * the Dispense page looks the batch up via GET /api/batches/{id} and
 * pre-fills the medicine name - removing manual entry error, which is the
 * actual root cause of most real-world dispensing mistakes.
 */
@Service
public class QrCodeService {

    private static final int SIZE = 300;

    public byte[] generateQrPng(Long batchId) {
        String payload = "MEDITRACK-BATCH:" + batchId;
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, SIZE, SIZE);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }
}

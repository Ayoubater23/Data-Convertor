package DataConvertor.service;


import DataConvertor.dao.FileDao;
import DataConvertor.model.FileEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {
    @Value("${file.upload-dir:${java.io.tmpdir}}")
    private String uploadDir;
    @Autowired
    private FileDao fileDao;
    private final ChatModel chatModel;

    public FileService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public void save(FileEntity file) {
        fileDao.save(file);
    }

    public Optional<FileEntity> getFileById(Long id) {
        return fileDao.getFileById(id);
    }

    public FileEntity convertAndSaveFile(MultipartFile file) {
        try {
            // Create upload directory if it doesn't exist
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Debug log
            System.out.println("Upload directory: " + directory.getAbsolutePath());
            System.out.println("Directory exists: " + directory.exists());
            System.out.println("Directory writable: " + directory.canWrite());

            // Generate unique filename
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(uploadDir, uniqueFileName);

            // Create a new FileEntity
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(originalFileName);
            fileEntity.setFileType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setFilePath(filePath.toString());
            fileEntity.setCreatedAt(LocalDateTime.now());

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Convert file content to text based on file type
            String textData = "";
            if (isPdf(file)) {
                textData = convertPdfToText(filePath.toString());
            } else if (isDocx(file)) {
                textData = convertDocxToText(filePath.toString());
            } else if (isImage(file)) {
                textData = convertImageToText(filePath.toString());
            }

            // Generate JSON response
            String jsonResponse = generateResponse(textData);
            fileEntity.setData(jsonResponse);

            // Save entity
            saveFileEntity(fileEntity);
            return fileEntity;

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    private boolean isPdf(MultipartFile file) {
        return "application/pdf".equals(file.getContentType());
    }

    private boolean isDocx(MultipartFile file) {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(file.getContentType());
    }

    private boolean isImage(MultipartFile file) {
        return "image/png".equals(file.getContentType()) || "image/jpeg".equals(file.getContentType());
    }

    public String convertPdfToText(String pdfFilePath) {
        StringBuilder textOutput = new StringBuilder();

        try (PDDocument document = PDDocument.load(new File(pdfFilePath))) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            String pdfText = pdfStripper.getText(document);
            textOutput.append(pdfText).append("\n");

            // Set up Tesseract for OCR
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
            String extractedText = tesseract.doOCR(new File(pdfFilePath));

            textOutput.append(extractedText);
        } catch (IOException | TesseractException e) {
            e.printStackTrace();
        }

        return textOutput.toString().trim(); // Added trim() to remove extra whitespace
    }

    public String convertDocxToText(String docxFilePath) {
        StringBuilder textOutput = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(docxFilePath)) {
            XWPFDocument document = new XWPFDocument(fis);
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                textOutput.append(paragraph.getText()).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return textOutput.toString().trim(); // Added trim() to remove extra whitespace
    }

    public String convertImageToText(String imageFilePath) {
        StringBuilder textOutput = new StringBuilder();

        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
            String extractedText = tesseract.doOCR(new File(imageFilePath));
            textOutput.append(extractedText);
        } catch (TesseractException e) {
            e.printStackTrace();
        }

        return textOutput.toString().trim(); // Added trim() to remove extra whitespace
    }

    private void saveFileEntity(FileEntity fileEntity) {
        if (fileDao != null) {
            fileDao.save(fileEntity);
        }
    }

    public List<FileEntity> getAllFiles() {
        return fileDao.getAllFiles();
    }

    public String generateResponse(String m) {
        String prompt = String.format("Convert the following text to very detailed pure JSON format. Remove all markdown formatting and code block indicators completely. Return the JSON object directly with no additional text, explanation, or formatting\\n\\n%s",m);
        try {
                String response = chatModel.call(prompt);
            return response.replaceAll("```json\\s*", "")
                    .replaceAll("```\\s*$", "")
                    .trim();
        }catch (Exception e){
            e.printStackTrace();
            return "error generating response";
        }
    }
}

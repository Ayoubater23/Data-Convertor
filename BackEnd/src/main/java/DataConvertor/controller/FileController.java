package DataConvertor.controller;

import DataConvertor.model.FileEntity;
import DataConvertor.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/fileConverter")
@CrossOrigin("http://localhost:3000")
public class FileController {
    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public List<FileEntity> getAllFiles() {
        return fileService.getAllFiles();
    }

    @PostMapping
    public void save(FileEntity file) {
        fileService.save(file);
    }

    @PostMapping("/upload")
    public ResponseEntity<FileEntity> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            FileEntity savedFileEntity = fileService.convertAndSaveFile(file);
            if (savedFileEntity != null) {
                return ResponseEntity.ok(savedFileEntity);
            }
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            e.printStackTrace(); // Add this for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new FileEntity()); // Return an empty entity with error details
        }
    }

    @PostMapping("/generate/{fileId}")
    public ResponseEntity<String> generateResponse(@PathVariable Long fileId) {
        Optional<FileEntity> fileEntity = fileService.getFileById(fileId);
        if (fileEntity.isPresent()) {
            // Get the converted text from the file entity and generate JSON
            String convertedText = fileEntity.get().getData();
            String jsonResponse = fileService.generateResponse(convertedText);
            return ResponseEntity.ok(jsonResponse);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/json/{fileId}")
    public ResponseEntity<String> getJsonData(@PathVariable Long fileId) {
        Optional<FileEntity> fileEntity = fileService.getFileById(fileId);
        if (fileEntity.isPresent() && fileEntity.get().getData() != null) {
            return ResponseEntity.ok(fileEntity.get().getData());
        }
        return ResponseEntity.notFound().build();
    }
}
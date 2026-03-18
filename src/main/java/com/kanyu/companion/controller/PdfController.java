package com.kanyu.companion.controller;

import com.kanyu.companion.service.PdfService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class PdfController {
    
    private final PdfService pdfService;
    
    @PostMapping("/upload")
    public ResponseEntity<PdfResponse> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        log.info("Uploading PDF for user: {}", userId);
        
        try {
            PdfService.PdfUploadResult result = pdfService.uploadAndProcess(file, userId);
            
            PdfResponse response = new PdfResponse();
            response.setSuccess(result.isSuccess());
            response.setFileName(result.getFileName());
            response.setTotalChunks(result.getTotalChunks());
            response.setSummary(result.getSummary());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Failed to upload PDF", e);
            PdfResponse response = new PdfResponse();
            response.setSuccess(false);
            response.setError(e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/question")
    public ResponseEntity<QuestionResponse> askQuestion(@RequestBody QuestionRequest request) {
        log.info("Answering PDF question for user: {}", request.getUserId());
        
        String answer = pdfService.answerQuestion(request.getQuestion(), request.getUserId());
        
        QuestionResponse response = new QuestionResponse();
        response.setSuccess(true);
        response.setAnswer(answer);
        
        return ResponseEntity.ok(response);
    }
    
    @Data
    public static class PdfResponse {
        private boolean success;
        private String fileName;
        private int totalChunks;
        private String summary;
        private String error;
    }
    
    @Data
    public static class QuestionRequest {
        private Long userId;
        private String question;
    }
    
    @Data
    public static class QuestionResponse {
        private boolean success;
        private String answer;
        private String error;
    }
}

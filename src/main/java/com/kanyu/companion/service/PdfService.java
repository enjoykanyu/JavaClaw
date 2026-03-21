package com.kanyu.companion.service;

import com.kanyu.companion.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {
    
    private final ChatModel chatModel;
    private final RagService ragService;
    
    private static final String UPLOAD_DIR = "/tmp/pdf_uploads";
    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 200;
    
    private static final String PDF_SUMMARY_PROMPT = """
        请分析以下PDF文档内容，生成结构化摘要。
        
        文档内容：
        %s
        
        请提供：
        1. 文档主题（一句话概括）
        2. 核心内容摘要（3-5个要点）
        3. 关键信息提取（重要数据、结论等）
        4. 建议阅读人群
        """;
    
    public PdfUploadResult uploadAndProcess(MultipartFile file, Long userId) throws IOException {
        log.info("Processing PDF upload for user: {}", userId);
        
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        file.transferTo(filePath.toFile());
        
        String content = extractText(filePath.toFile());
        
        List<String> chunks = splitIntoChunks(content);
        
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", file.getOriginalFilename());
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());
            
            ragService.indexDocument(chunks.get(i), userId, file.getOriginalFilename(), metadata);
        }
        
        String summary = generateSummary(content);
        
        PdfUploadResult result = new PdfUploadResult();
        result.setSuccess(true);
        result.setFileName(file.getOriginalFilename());
        result.setStoredPath(filePath.toString());
        result.setTotalChunks(chunks.size());
        result.setSummary(summary);
        
        return result;
    }
    
    public String extractText(File pdfFile) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    public List<String> splitIntoChunks(String content) {
        List<String> chunks = new ArrayList<>();
        
        if (content == null || content.isEmpty()) {
            return chunks;
        }
        
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());
            
            if (end < content.length()) {
                int lastPeriod = content.lastIndexOf('。', end);
                int lastNewline = content.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastPeriod, lastNewline);
                
                if (breakPoint > start) {
                    end = breakPoint + 1;
                }
            }
            
            chunks.add(content.substring(start, end).trim());
            start = end - CHUNK_OVERLAP;
            
            if (start < 0) start = 0;
        }
        
        return chunks;
    }
    
    public String generateSummary(String content) {
        try {
            String truncatedContent = content.length() > 4000 
                ? content.substring(0, 4000) + "..." 
                : content;
            
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的文档分析助手，擅长提取文档关键信息。"));
            messages.add(new UserMessage(String.format(PDF_SUMMARY_PROMPT, truncatedContent)));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }

        } catch (Exception e) {
            log.error("Failed to generate PDF summary", e);
        }

        return "无法生成摘要";
    }
    
    public String answerQuestion(String question, Long userId) {
        String context = ragService.queryWithContext(question, userId);
        
        if (context == null || context.isEmpty()) {
            return "抱歉，我没有找到相关的文档内容来回答这个问题。";
        }
        
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("""
                你是一个文档问答助手。请基于提供的参考资料回答问题。
                如果参考资料中没有相关信息，请诚实说明。
                回答要简洁明了，引用相关内容。
                """));
            messages.add(new UserMessage(String.format("""
                参考资料：
                %s
                
                问题：%s
                """, context, question)));
            
            Prompt prompt = new Prompt(messages);
            ChatResponse response = chatModel.call(prompt);
            
            if (response != null && response.getResult() != null) {
                return response.getResult().getOutput().getContent();
            }

        } catch (Exception e) {
            log.error("Failed to answer PDF question", e);
        }
        
        return "抱歉，处理问题时出现错误。";
    }
    
    public static class PdfUploadResult {
        private boolean success;
        private String fileName;
        private String storedPath;
        private int totalChunks;
        private String summary;
        private String error;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getStoredPath() { return storedPath; }
        public void setStoredPath(String storedPath) { this.storedPath = storedPath; }
        public int getTotalChunks() { return totalChunks; }
        public void setTotalChunks(int totalChunks) { this.totalChunks = totalChunks; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
}

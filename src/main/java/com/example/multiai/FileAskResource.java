package com.example.multiai;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

@Path("/ask-file")
public class FileAskResource {

    @Inject
    MultiAiService multiAiService;

    public static class AskFileResponse {
        public String claude;
        public String openai;
        public String perplexity;
        public String unified;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public AskFileResponse askWithFile(
            @RestForm("file") FileUpload file,
            @RestForm("question") String question
    ) {
        if (file == null) {
            throw new BadRequestException("file is required");
        }

        String fileText = extractFileText(file);
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are given the following file content (already converted to text):\n\n");
        prompt.append(fileText).append("\n\n");
        if (question != null && !question.isBlank()) {
            prompt.append("User question: ").append(question).append("\n");
        } else {
            prompt.append("Summarize and explain the most important information in this file.");
        }

        MultiAiService.MultiAiResult result = multiAiService.askAll(prompt.toString());

        AskFileResponse resp = new AskFileResponse();
        resp.claude = result.claude();
        resp.openai = result.openai();
        resp.perplexity = result.perplexity();
        resp.unified = result.unified();
        return resp;
    }

    private String extractFileText(FileUpload file) {
        String fileName = file.fileName() != null ? file.fileName().toLowerCase() : "";
        String contentType = file.contentType() != null ? file.contentType().toLowerCase() : "";

        boolean isExcel =
            fileName.endsWith(".xlsx") ||
            fileName.endsWith(".xls") ||
            contentType.contains("spreadsheet");

        try {
            if (isExcel) {
                return readExcel(file);
            } else {
                return readAsUtf8(file);
            }
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to read uploaded file: " + e.getMessage(), e);
        }
    }

    private String readAsUtf8(FileUpload file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.filePath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String readExcel(FileUpload file) throws IOException {
        try (InputStream in = Files.newInputStream(file.filePath())) {
            Workbook workbook;
            String name = file.fileName() != null ? file.fileName().toLowerCase() : "";
            if (name.endsWith(".xls")) {
                workbook = new HSSFWorkbook(in);
            } else {
                workbook = new XSSFWorkbook(in);
            }

            StringBuilder sb = new StringBuilder();
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                sb.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    boolean rowHasContent = false;
                    StringBuilder rowSb = new StringBuilder();
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.BLANK) {
                            rowSb.append(" | ");
                            continue;
                        }
                        rowHasContent = true;
                        String cellText = getCellText(cell);
                        rowSb.append(cellText).append(" | ");
                    }
                    if (rowHasContent) {
                        sb.append(rowSb).append("\n");
                    }
                }
                sb.append("\n");
            }
            workbook.close();
            return sb.length() == 0 ? "(empty Excel file)" : sb.toString();
        }
    }

    private String getCellText(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield Double.toString(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    yield Double.toString(cell.getNumericCellValue());
                }
            }
            case BLANK, _NONE, ERROR -> "";
        };
    }
}

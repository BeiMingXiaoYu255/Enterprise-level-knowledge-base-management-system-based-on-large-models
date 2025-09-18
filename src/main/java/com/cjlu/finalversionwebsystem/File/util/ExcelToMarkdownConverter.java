package com.cjlu.finalversionwebsystem.File.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel转Markdown格式工具类
 * 支持.xls和.xlsx格式的Excel文件转换为Markdown表格
 */
public class ExcelToMarkdownConverter {

    /**
     * 将Excel文件转换为Markdown文件
     * @param excelFilePath 源Excel文件路径
     * @param mdFilePath 目标Markdown文件路径
     * @throws IOException 当文件操作发生错误时抛出
     */
    public static void convert(String excelFilePath, String mdFilePath) throws IOException {
        convert(excelFilePath, mdFilePath, true);
    }

    /**
     * 将Excel文件转换为Markdown文件
     * @param excelFilePath 源Excel文件路径
     * @param mdFilePath 目标Markdown文件路径
     * @param includeSheetNames 是否包含工作表名称作为标题
     * @throws IOException 当文件操作发生错误时抛出
     */
    public static void convert(String excelFilePath, String mdFilePath, boolean includeSheetNames) throws IOException {
        // 读取Excel文件
        try (Workbook workbook = getWorkbook(excelFilePath);
             BufferedWriter writer = Files.newBufferedWriter(Paths.get(mdFilePath), StandardCharsets.UTF_8)) {

            // 处理每个工作表
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet == null) {
                    continue;
                }

                String sheetName = sheet.getSheetName();
                System.out.println("处理工作表: " + sheetName);

                // 如果需要，添加工作表名称作为标题
                if (includeSheetNames) {
                    writer.write("## " + sheetName + "\n\n");
                }

                // 将工作表转换为Markdown表格并写入文件
                String markdownTable = convertSheetToMarkdown(sheet);
                writer.write(markdownTable);
                writer.write("\n\n");
            }
        }

        System.out.println("Excel转换完成: " + mdFilePath);
    }

    /**
     * 根据文件扩展名获取相应的Workbook实例
     */
    private static Workbook getWorkbook(String filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            if (filePath.endsWith(".xlsx")) {
                return new XSSFWorkbook(inputStream);
            } else if (filePath.endsWith(".xls")) {
                return new HSSFWorkbook(inputStream);
            } else {
                throw new IllegalArgumentException("不支持的文件格式: " + filePath +
                        "，仅支持.xls和.xlsx格式");
            }
        }
    }

    /**
     * 将单个工作表转换为Markdown表格
     */
    private static String convertSheetToMarkdown(Sheet sheet) {
        // 收集表格数据
        List<List<String>> tableData = new ArrayList<>();
        int maxRowNum = sheet.getLastRowNum();

        // 找出最大列数
        int maxColumnNum = 0;
        for (int i = 0; i <= maxRowNum; i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getLastCellNum() > maxColumnNum) {
                maxColumnNum = row.getLastCellNum();
            }
        }

        // 读取单元格数据
        for (int i = 0; i <= maxRowNum; i++) {
            Row row = sheet.getRow(i);
            List<String> rowData = new ArrayList<>();

            if (row == null) {
                // 处理空行
                for (int j = 0; j < maxColumnNum; j++) {
                    rowData.add("");
                }
            } else {
                // 读取每行的单元格数据
                for (int j = 0; j < maxColumnNum; j++) {
                    Cell cell = row.getCell(j);
                    rowData.add(getCellValue(cell));
                }
            }

            tableData.add(rowData);
        }

        // 如果表格为空，返回空字符串
        if (tableData.isEmpty() || tableData.get(0).isEmpty()) {
            return "";
        }

        // 构建Markdown表格
        StringBuilder markdown = new StringBuilder();

        // 添加表头行
        List<String> headerRow = tableData.get(0);
        markdown.append("| ");
        for (String cellValue : headerRow) {
            markdown.append(escapeSpecialCharacters(cellValue)).append(" | ");
        }
        markdown.append("\n");

        // 添加分隔行
        markdown.append("| ");
        for (int j = 0; j < headerRow.size(); j++) {
            markdown.append("--- | ");
        }
        markdown.append("\n");

        // 添加数据行
        for (int i = 1; i < tableData.size(); i++) {
            List<String> dataRow = tableData.get(i);
            markdown.append("| ");
            for (String cellValue : dataRow) {
                markdown.append(escapeSpecialCharacters(cellValue)).append(" | ");
            }
            markdown.append("\n");
        }

        return markdown.toString();
    }

    /**
     * 获取单元格的值
     */
    private static String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        String cellValue;
        switch (cell.getCellType()) {
            case STRING:
                cellValue = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    cellValue = cell.getDateCellValue().toString();
                } else {
                    // 处理数字，避免科学计数法
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        cellValue = String.valueOf((long) numericValue);
                    } else {
                        cellValue = String.valueOf(numericValue);
                    }
                }
                break;
            case BOOLEAN:
                cellValue = String.valueOf(cell.getBooleanCellValue());
                break;
            case FORMULA:
                // 尝试获取公式计算结果
                try {
                    cellValue = String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    cellValue = cell.getStringCellValue();
                }
                break;
            default:
                cellValue = "";
        }

        return cellValue.trim();
    }

    /**
     * 转义Markdown中的特殊字符
     */
    private static String escapeSpecialCharacters(String text) {
        if (text == null) {
            return "";
        }
        // 转义竖线|，因为它在Markdown表格中有特殊含义
        return text.replace("|", "\\|")
                .replace("\n", " ")
                .replace("\r", "");
    }
}
    
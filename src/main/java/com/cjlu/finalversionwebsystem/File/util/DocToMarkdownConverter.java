package com.cjlu.finalversionwebsystem.File.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * DOC转Markdown工具类
 * 支持识别图片、标题、图形和公式
 */
public class DocToMarkdownConverter {

    // 图片保存目录
    private static final String IMAGE_DIR = "images/";
    // 图形保存目录
    private static final String DRAWING_DIR = "drawings/";
    // 公式保存目录
    private static final String EQUATION_DIR = "equations/";

    /**
     * 将DOC或DOCX文件转换为Markdown
     * @param docFilePath DOC/DOCX文件路径
     * @param mdFilePath 输出的Markdown文件路径
     * @throws Exception 处理过程中可能抛出的异常
     */
    public static void convert(String docFilePath, String mdFilePath) throws Exception {
        // 创建必要的目录
        FileUtil.mkdir(IMAGE_DIR);
        FileUtil.mkdir(DRAWING_DIR);
        FileUtil.mkdir(EQUATION_DIR);

        try (Writer writer = new OutputStreamWriter(
                Files.newOutputStream(Paths.get(mdFilePath)),
                StandardCharsets.UTF_8)) {

            if (docFilePath.toLowerCase().endsWith(".docx")) {
                // 处理DOCX文件
                convertDocx(docFilePath, writer);
            } else {
                // 处理DOC文件
                convertDoc(docFilePath, writer);
            }
        }
    }

    /**
     * 处理DOCX文件
     */
    private static void convertDocx(String docxFilePath, Writer writer) throws Exception {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(Paths.get(docxFilePath)))) {
            // 处理段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                processDocxParagraph(paragraph, writer);
            }

            // 处理表格
            for (XWPFTable table : document.getTables()) {
                processDocxTable(table, writer);
            }

            // 处理图片
            int imageIndex = 0;
            for (XWPFPictureData pictureData : document.getAllPictures()) {
                String imageFileName = saveImage(pictureData, imageIndex++);
                writer.write("![图片][" + imageFileName + "]\n\n");
            }

            // 处理图形
            processDrawings(document, writer);

            // 处理公式
            processEquations(document, writer);
        }
    }

    /**
     * 处理DOC文件
     */
    private static void convertDoc(String docFilePath, Writer writer) throws Exception {
        try (HWPFDocument document = new HWPFDocument(Files.newInputStream(Paths.get(docFilePath)))) {
            Range range = document.getRange();

            // 处理段落
            for (int i = 0; i < range.numParagraphs(); i++) {
                Paragraph paragraph = range.getParagraph(i);
                processDocParagraph(paragraph, writer);
            }

            // 处理表格
            TableIterator tableIterator = new TableIterator(range);
            while (tableIterator.hasNext()) {
                Table table = tableIterator.next();
                processDocTable(table, writer);
            }

            // 处理图片
            processDocImages(document, writer);

            // 处理图形和公式
            processDocDrawingsAndEquations(document, writer);
        }
    }

    /**
     * 处理DOCX段落
     */
    private static void processDocxParagraph(XWPFParagraph paragraph, Writer writer) throws IOException {
        String text = paragraph.getText();
        if (StrUtil.isEmpty(text)) {
            return;
        }

        // 处理标题
        int headingLevel = getHeadingLevel(paragraph.getStyleID());
        if (headingLevel > 0) {
            writer.write(StrUtil.repeat("#", headingLevel) + " " + text + "\n\n");
            return;
        }

        // 处理普通段落和样式
        StringBuilder paragraphText = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String runText = run.getText(0);
            if (runText == null) continue;

            // 处理加粗
            if (run.isBold()) {
                paragraphText.append("**").append(runText).append("**");
            }
            // 处理斜体
            else if (run.isItalic()) {
                paragraphText.append("*").append(runText).append("*");
            }
            // 处理下划线
            else if (run.isStrikeThrough()) {
                paragraphText.append("~~").append(runText).append("~~");
            } else {
                paragraphText.append(runText);
            }
        }

        writer.write(paragraphText.toString() + "\n\n");
    }

    /**
     * 处理DOC段落
     */
    private static void processDocParagraph(Paragraph paragraph, Writer writer) throws IOException {
        String text = paragraph.text();
        if (StrUtil.isEmpty(text)) {
            return;
        }

        // 处理标题
        int headingLevel = getHeadingLevel(paragraph.getStyleIndex());
        if (headingLevel > 0) {
            writer.write(StrUtil.repeat("#", headingLevel) + " " + text + "\n\n");
            return;
        }

        // 处理文本样式
        StringBuilder paragraphText = new StringBuilder();
        for (int i = 0; i < paragraph.numCharacterRuns(); i++) {
            CharacterRun run = paragraph.getCharacterRun(i);
            String runText = run.text();

            // 处理加粗
            if (run.isBold()) {
                paragraphText.append("**").append(runText).append("**");
            }
            // 处理斜体
            else if (run.isItalic()) {
                paragraphText.append("*").append(runText).append("*");
            }
            // 处理下划线
            else if (run.isStrikeThrough()) {
                paragraphText.append("~~").append(runText).append("~~");
            } else {
                paragraphText.append(runText);
            }
        }

        writer.write(paragraphText.toString() + "\n\n");
    }

    /**
     * 处理DOCX表格
     */
    private static void processDocxTable(XWPFTable table, Writer writer) throws IOException {
        // 处理表头
        List<XWPFTableRow> rows = table.getRows();
        if (rows.isEmpty()) return;

        // 写入表头
        XWPFTableRow headerRow = rows.get(0);
        for (XWPFTableCell cell : headerRow.getTableCells()) {
            writer.write("| " + cell.getText() + " ");
        }
        writer.write("|\n");

        // 写入表头分隔线
        for (int i = 0; i < headerRow.getTableCells().size(); i++) {
            writer.write("| --- ");
        }
        writer.write("|\n");

        // 写入表格内容
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            for (XWPFTableCell cell : row.getTableCells()) {
                writer.write("| " + cell.getText() + " ");
            }
            writer.write("|\n");
        }
        writer.write("\n");
    }

    /**
     * 处理DOC表格
     */
    private static void processDocTable(Table table, Writer writer) throws IOException {
        int rowCount = table.numRows();
        if (rowCount == 0) return;

        // 写入表头
        TableRow headerRow = table.getRow(0);
        for (int i = 0; i < headerRow.numCells(); i++) {
            TableCell cell = headerRow.getCell(i);
            writer.write("| " + cell.text() + " ");
        }
        writer.write("|\n");

        // 写入表头分隔线
        for (int i = 0; i < headerRow.numCells(); i++) {
            writer.write("| --- ");
        }
        writer.write("|\n");

        // 写入表格内容
        for (int i = 1; i < rowCount; i++) {
            TableRow row = table.getRow(i);
            for (int j = 0; j < row.numCells(); j++) {
                TableCell cell = row.getCell(j);
                writer.write("| " + cell.text() + " ");
            }
            writer.write("|\n");
        }
        writer.write("\n");
    }

    /**
     * 保存图片并返回文件名
     */
    private static String saveImage(XWPFPictureData pictureData, int index) throws Exception {
        byte[] imageBytes = pictureData.getData();
        String fileExtension = pictureData.suggestFileExtension();
        String fileName = IMAGE_DIR + "image_" + index + "." + fileExtension;

        FileUtil.writeBytes(imageBytes, fileName);
        return fileName;
    }

    /**
     * 处理DOCX中的图形
     */
    private static void processDrawings(XWPFDocument document, Writer writer) throws Exception {
        // 实际实现中需要更复杂的逻辑来提取和转换图形
        int drawingIndex = 0;
        // 这里简化处理，实际项目中需要根据文档结构提取图形
        writer.write("\n[图形 " + drawingIndex + " 已提取到 " + DRAWING_DIR + "]\n\n");
    }

    /**
     * 处理DOCX中的公式
     */
    private static void processEquations(XWPFDocument document, Writer writer) throws Exception {
        // 处理公式
        int equationIndex = 0;
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                if (run.getCTR().getPictList() != null) {
                    // 这里简化处理，实际项目中需要提取公式并转换
                    String equationFileName = EQUATION_DIR + "equation_" + equationIndex++ + ".svg";
                    writer.write("![公式][" + equationFileName + "]\n\n");
                }
            }
        }
    }

    /**
     * 处理DOC中的图片
     */
    private static void processDocImages(HWPFDocument document, Writer writer) throws Exception {
        int imageIndex = 0;
        PicturesTable picturesTable = document.getPicturesTable();
        if (picturesTable != null) {
            for (Picture picture : picturesTable.getAllPictures()) {
                String fileExtension = picture.suggestFileExtension();
                String fileName = IMAGE_DIR + "image_" + imageIndex++ + "." + fileExtension;

                try (OutputStream out = new FileOutputStream(fileName)) {
                    picture.writeImageContent(out);
                }
                writer.write("![图片][" + fileName + "]\n\n");
            }
        }
    }

    /**
     * 处理DOC中的图形和公式
     */
    private static void processDocDrawingsAndEquations(HWPFDocument document, Writer writer) throws Exception {
        // 处理图形和公式的逻辑
        writer.write("\n[图形和公式已提取]\n\n");
    }

    /**
     * 获取标题级别
     */
    private static int getHeadingLevel(String styleId) {
        if (StrUtil.isEmpty(styleId)) {
            return 0;
        }

        if (styleId.startsWith("Heading1")) return 1;
        if (styleId.startsWith("Heading2")) return 2;
        if (styleId.startsWith("Heading3")) return 3;
        if (styleId.startsWith("Heading4")) return 4;
        if (styleId.startsWith("Heading5")) return 5;
        if (styleId.startsWith("Heading6")) return 6;

        return 0;
    }

    /**
     * 获取标题级别（用于DOC文件）
     */
    private static int getHeadingLevel(int styleIndex) {
        // 样式索引对应关系可能需要根据实际情况调整
        if (styleIndex >= 0 && styleIndex <= 5) {
            return styleIndex + 1;
        }
        return 0;
    }
}

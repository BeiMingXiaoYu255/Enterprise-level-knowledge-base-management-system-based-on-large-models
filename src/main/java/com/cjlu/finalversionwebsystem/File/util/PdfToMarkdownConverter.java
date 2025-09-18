package com.cjlu.finalversionwebsystem.File.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PDF转Markdown工具类
 * 支持识别图片、表格、加粗文本、标题和代码块
 */
public class PdfToMarkdownConverter {

    // 图片保存目录
    private static final String IMAGE_DIR = "images/";

    // 常见的代码字体（等宽字体）
    private static final Set<String> CODE_FONTS = Set.of(
            "courier", "consolas", "monaco", "menlo", "andale mono",
            "lucida console", "monospace", "courier new"
    );

    // 文本块信息类，用于记录文本及其样式
    private static class TextBlock {
        String text;
        float x;
        float y;
        float fontSize;
        String fontName;
        boolean isBold;
        boolean isCode; // 新增：是否为代码
        int pageNumber;

        public TextBlock(String text, float x, float y, float fontSize, String fontName, int pageNumber) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.fontSize = fontSize;
            this.fontName = fontName;
            this.pageNumber = pageNumber;
            this.isBold = isFontBold(fontName);
            this.isCode = isCodeFont(fontName); // 新增：判断是否为代码字体
        }

        public String getText() { return text; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getFontSize() { return fontSize; }
        public String getFontName() { return fontName; }
        public boolean isBold() { return isBold; }
        public boolean isCode() { return isCode; } // 新增：获取是否为代码
        public int getPageNumber() { return pageNumber; }
    }

    /**
     * 判断字体是否为粗体
     */
    private static boolean isFontBold(String fontName) {
        if (StrUtil.isEmpty(fontName)) {
            return false;
        }
        fontName = fontName.toLowerCase();
        return fontName.contains("bold") || fontName.contains("heavy") || fontName.contains("black");
    }

    /**
     * 新增：判断字体是否为代码字体（等宽字体）
     */
    private static boolean isCodeFont(String fontName) {
        if (StrUtil.isEmpty(fontName)) {
            return false;
        }
        String lowerFontName = fontName.toLowerCase();
        // 检查字体名是否包含常见代码字体
        return CODE_FONTS.stream().anyMatch(lowerFontName::contains);
    }

    /**
     * 将PDF文件转换为Markdown文件
     * @param pdfFilePath PDF文件路径
     * @param mdFilePath  输出的Markdown文件路径
     * @throws IOException 处理过程中可能抛出的IO异常
     */
    public static void convert(String pdfFilePath, String mdFilePath) throws IOException {
        // 创建图片保存目录
        FileUtil.mkdir(IMAGE_DIR);

        try (PDDocument document = PDDocument.load(new File(pdfFilePath));
             Writer writer = new OutputStreamWriter(Files.newOutputStream(Paths.get(mdFilePath)), StandardCharsets.UTF_8)) {

            // 提取文本和样式信息
            List<TextBlock> textBlocks = extractTextWithStyle(document);

            // 提取图片
            extractImages(document);

            // 提取表格
            List<String> tableMarkdowns = extractTables(document, pdfFilePath);

            // 处理文本块并转换为Markdown
            processTextBlocks(textBlocks, tableMarkdowns, writer);
        }
    }

    /**
     * 提取文本及其样式信息
     */
    private static List<TextBlock> extractTextWithStyle(PDDocument document) throws IOException {
        List<TextBlock> textBlocks = new ArrayList<>();

        PDFTextStripper stripper = new PDFTextStripper() {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
                if (StrUtil.isNotEmpty(text) && !text.trim().isEmpty()) {
                    TextPosition firstPos = textPositions.get(0);
                    float x = firstPos.getXDirAdj();
                    float y = firstPos.getYDirAdj();
                    float fontSize = firstPos.getFontSizeInPt();
                    String fontName = firstPos.getFont().getName();
                    int pageNumber = getCurrentPageNo();

                    textBlocks.add(new TextBlock(text, x, y, fontSize, fontName, pageNumber));
                }
            }
        };

        stripper.setSortByPosition(true);
        stripper.getText(document);
        return textBlocks;
    }

    /**
     * 提取PDF中的图片
     */
    private static void extractImages(PDDocument document) throws IOException {
        int imageIndex = 0;

        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            Iterable<COSName> xObjectNames = resources.getXObjectNames();

            if (xObjectNames != null) {
                for (COSName name : xObjectNames) {
                    if (resources.isImageXObject(name)) {
                        PDImageXObject image = (PDImageXObject) resources.getXObject(name);
                        String imageFileName = IMAGE_DIR + "image_" + (imageIndex++) + "." + image.getSuffix();
                        BufferedImage bufferedImage = image.getImage();
                        ImageIO.write(bufferedImage, image.getSuffix(), new File(imageFileName));

                    }
                }
            }
        }
    }

    /**
     * 提取PDF中的表格并转换为Markdown格式
     */
    private static List<String> extractTables(PDDocument document, String pdfFilePath) throws IOException {
        List<String> tableMarkdowns = new ArrayList<>();
        ObjectExtractor extractor = new ObjectExtractor(document);

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            Page page = extractor.extract(i + 1);
            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
            List<Table> tables = sea.extract(page);

            for (Table table : tables) {
                // 转换表格为Markdown格式
                StringBuilder tableMd = new StringBuilder();

                // 提取表头
                List<List<RectangularTextContainer>> rows = table.getRows();
                if (!rows.isEmpty()) {
                    List<RectangularTextContainer> headerRow = rows.get(0);
                    tableMd.append(headerRow.stream()
                                    .map(cell -> "| " + cell.getText() + " ")
                                    .collect(Collectors.joining()))
                            .append("|\n");

                    // 添加表头分隔线
                    tableMd.append(headerRow.stream()
                                    .map(cell -> "| --- ")
                                    .collect(Collectors.joining()))
                            .append("|\n");
                }

                // 提取表格内容
                for (List<RectangularTextContainer> row : table.getRows()) {
                    tableMd.append(row.stream()
                                    .map(cell -> "| " + cell.getText() + " ")
                                    .collect(Collectors.joining()))
                            .append("|\n");
                }

                tableMarkdowns.add(tableMd.toString());
            }
        }

        extractor.close();
        return tableMarkdowns;
    }

    /**
     * 处理文本块并转换为Markdown格式，新增代码块处理
     */
    private static void processTextBlocks(List<TextBlock> textBlocks, List<String> tableMarkdowns, Writer writer) throws IOException {
        // 按页码和位置排序文本块
        textBlocks.sort((a, b) -> {
            if (a.getPageNumber() != b.getPageNumber()) {
                return Integer.compare(a.getPageNumber(), b.getPageNumber());
            }
            // 先按y坐标排序（从上到下），再按x坐标排序（从左到右）
            int yCompare = Float.compare(b.getY(), a.getY());
            return yCompare != 0 ? yCompare : Float.compare(a.getX(), b.getX());
        });

        // 分析字体大小，确定标题层级
        Set<Float> fontSizes = textBlocks.stream()
                .map(TextBlock::getFontSize)
                .collect(Collectors.toSet());
        List<Float> sortedSizes = new ArrayList<>(fontSizes);
        sortedSizes.sort(Collections.reverseOrder());

        // 处理文本块
        Float prevFontSize = null;
        boolean newParagraph = false;
        boolean inCodeBlock = false; // 新增：标记是否在代码块中
        float codeBlockStartX = 0;   // 新增：代码块起始X坐标，用于判断缩进

        for (TextBlock block : textBlocks) {
            String text = block.getText().replace("\r\n", " ").replace("\n", " ").trim();
            if (StrUtil.isEmpty(text)) {
                // 如果在代码块中，保留空行
                if (inCodeBlock) {
                    writer.write("\n");
                }
                newParagraph = true;
                continue;
            }

            // 处理标题（标题优先于代码块）
            int headerLevel = getHeaderLevel(block.getFontSize(), sortedSizes);
            if (headerLevel > 0) {
                // 如果正在代码块中，先关闭代码块
                if (inCodeBlock) {
                    writer.write("\n```\n");
                    inCodeBlock = false;
                }

                writer.write("\n" + StrUtil.repeat("#", headerLevel) + " " + text + "\n\n");
                prevFontSize = block.getFontSize();
                newParagraph = false;
                continue;
            }

            // 新增：处理代码块
            if (block.isCode()) {
                // 如果不在代码块中，开始一个新的代码块
                if (!inCodeBlock) {
                    // 确保与前一段落有间隔
                    if (prevFontSize != null) {
                        writer.write("\n");
                    }
                    writer.write("```\n");
                    inCodeBlock = true;
                    codeBlockStartX = block.getX(); // 记录代码块起始X坐标
                } else {
                    // 检查是否需要换行（不同行的内容）
                    float yDiff = Math.abs(block.getY() - prevFontSize);
                    if (yDiff > block.getFontSize() * 0.5) { // 超过半个字体高度视为新行
                        writer.write("\n");
                    }
                }

                // 处理代码缩进（根据与代码块起始位置的X坐标差）
                float indent = block.getX() - codeBlockStartX;
                if (indent > 1) { // 超过1单位的偏移视为缩进
                    int spaces = (int)(indent / 5); // 每5个单位转换为1个空格
                    writer.write(StrUtil.repeat(" ", Math.max(1, spaces)));
                }

                writer.write(text);
                prevFontSize = block.getY(); // 对于代码块，记录Y坐标用于换行判断
                newParagraph = false;
                continue;
            } else if (inCodeBlock) {
                // 如果当前不是代码块但之前在代码块中，关闭代码块
                writer.write("\n```\n\n");
                inCodeBlock = false;
                prevFontSize = null;
                newParagraph = true;
            }

            // 处理段落分隔
            if (prevFontSize != null && !prevFontSize.equals(block.getFontSize())) {
                writer.write("\n");
                newParagraph = true;
            } else if (newParagraph) {
                writer.write("\n");
                newParagraph = false;
            }

            // 处理粗体
            if (block.isBold()) {
                writer.write("**" + text + "**");
            } else {
                writer.write(text);
            }

            prevFontSize = block.getFontSize();
        }

        // 新增：如果结束时仍在代码块中，关闭代码块
        if (inCodeBlock) {
            writer.write("\n```\n");
        }

        // 添加提取的表格
        if (!tableMarkdowns.isEmpty()) {
            writer.write("\n\n");
            for (String table : tableMarkdowns) {
                writer.write(table + "\n\n");
            }
        }
    }

    /**
     * 根据字体大小确定标题层级
     */
    private static int getHeaderLevel(float fontSize, List<Float> sortedSizes) {
        // 只将最大的几种字体大小识别为标题
        if (sortedSizes.size() >= 5 && fontSize >= sortedSizes.get(4)) {
            int index = sortedSizes.indexOf(fontSize);
            return Math.min(index + 1, 6); // 最多支持到6级标题
        }
        return 0;
    }
}

package com.cjlu.finalversionwebsystem.File.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 图片文字提取与MD生成工具类
 * 将图片中的文字提取出来，并将图片和提取的文字写入Markdown文件
 */
public class ImageToMarkdownConverter {

    // Tesseract OCR 实例
    private static final ITesseract tesseract = new Tesseract();

    // 初始化Tesseract，设置数据目录和语言
    static {
        // 设置Tesseract数据目录（包含语言包）
        File tessDataFolder = LoadLibs.extractTessResources("tessdata");
        tesseract.setDatapath(tessDataFolder.getAbsolutePath());

        // 设置识别语言（中文+英文，需要对应的语言包）
        tesseract.setLanguage("chi_sim+eng");
    }

    /**
     * 处理单张图片：提取文字并生成MD文件
     * @param imagePath 图片路径
     * @param mdPath 生成的MD文件路径
     * @throws IOException IO异常
     * @throws TesseractException OCR识别异常
     */
    public static void processImage(String imagePath, String mdPath) throws IOException, TesseractException {
        // 提取图片文字
        String extractedText = extractTextFromImage(imagePath);

        // 处理提取的文字为半结构化格式
        String structuredText = structureText(extractedText);

        // 生成Markdown文件
        generateMarkdown(imagePath, structuredText, mdPath);
    }

    /**
     * 处理多张图片：提取文字并生成单个MD文件
     * @param imagePaths 图片路径列表
     * @param mdPath 生成的MD文件路径
     * @throws IOException IO异常
     * @throws TesseractException OCR识别异常
     */
    public static void processImages(List<String> imagePaths, String mdPath) throws IOException, TesseractException {
        try (FileWriter writer = new FileWriter(mdPath, StandardCharsets.UTF_8)) {
            writer.write("# 图片文字提取结果\n\n");

            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i);
                writer.write("## 图片 " + (i + 1) + "\n\n");

                // 写入图片
                String imageName = FileUtil.getName(imagePath);
                writer.write("![图片 " + (i + 1) + "](" + imagePath + ")\n\n");

                // 提取并写入文字
                String extractedText = extractTextFromImage(imagePath);
                String structuredText = structureText(extractedText);
                writer.write("### 提取的文字：\n\n");
                writer.write(structuredText + "\n\n");
                writer.write("---\n\n");
            }
        }
    }

    /**
     * 从图片中提取文字
     * @param imagePath 图片路径
     * @return 提取的文字
     * @throws IOException IO异常
     * @throws TesseractException OCR识别异常
     */
    private static String extractTextFromImage(String imagePath) throws IOException, TesseractException {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            throw new IOException("图片文件不存在: " + imagePath);
        }

        BufferedImage image = ImageIO.read(imageFile);
        if (image == null) {
            throw new IOException("无法读取图片文件: " + imagePath);
        }

        // 使用Tesseract进行OCR识别
        return tesseract.doOCR(image);
    }

    /**
     * 将提取的文字处理为半结构化格式
     * @param text 原始提取的文字
     * @return 半结构化的文字
     */
    private static String structureText(String text) {
        if (StrUtil.isEmpty(text)) {
            return "未能从图片中提取到文字";
        }

        // 简单的结构化处理：分段、识别可能的标题等
        StringBuilder structured = new StringBuilder();
        String[] paragraphs = text.split("\n\n");

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (StrUtil.isEmpty(paragraph)) {
                continue;
            }

            // 假设以数字加点开头的是列表项
            if (paragraph.matches("^\\d+\\..+")) {
                structured.append("- ").append(paragraph).append("\n");
            }
            // 假设全大写或结尾有冒号的是标题
            else if (paragraph.matches("^[A-Z0-9\\s:]+$") || paragraph.endsWith(":")) {
                structured.append("**").append(paragraph).append("**\n\n");
            }
            // 普通段落
            else {
                structured.append(paragraph).append("\n\n");
            }
        }

        return structured.toString();
    }

    /**
     * 生成包含图片和提取文字的Markdown文件
     * @param imagePath 图片路径
     * @param structuredText 结构化的文字
     * @param mdPath 生成的MD文件路径
     * @throws IOException IO异常
     */
    private static void generateMarkdown(String imagePath, String structuredText, String mdPath) throws IOException {
        String imageName = FileUtil.getName(imagePath);

        // 创建MD文件内容
        StringBuilder mdContent = new StringBuilder();
        mdContent.append("# 图片文字提取：").append(imageName).append("\n\n");
        mdContent.append("## 原始图片\n\n");
        mdContent.append("![").append(imageName).append("](").append(imagePath).append(")\n\n");
        mdContent.append("## 提取的文字内容\n\n");
        mdContent.append(structuredText);

        // 写入文件
        FileUtil.writeString(mdContent.toString(), mdPath, StandardCharsets.UTF_8);
    }

    /**
     * 设置Tesseract语言包
     * @param language 语言代码，如"chi_sim"（简体中文）、"eng"（英文）、"chi_sim+eng"（中英文）
     */
    public static void setTesseractLanguage(String language) {
        tesseract.setLanguage(language);
    }

    /**
     * 设置Tesseract数据目录（语言包所在目录）
     * @param dataPath 数据目录路径
     */
    public static void setTesseractDataPath(String dataPath) {
        tesseract.setDatapath(dataPath);
    }
}

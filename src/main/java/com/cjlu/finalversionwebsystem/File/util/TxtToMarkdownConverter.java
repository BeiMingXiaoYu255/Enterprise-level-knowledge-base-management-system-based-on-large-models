package com.cjlu.finalversionwebsystem.File.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * TXT文件转Markdown格式工具类
 */
public class TxtToMarkdownConverter {

    /**
     * 将TXT文件转换为Markdown文件
     * @param txtFilePath 源TXT文件路径
     * @param mdFilePath 目标Markdown文件路径
     * @throws IOException 当文件操作发生错误时抛出
     */
    public static void convert(String txtFilePath, String mdFilePath) throws IOException {
        // 读取TXT文件内容
        List<String> lines = Files.readAllLines(Paths.get(txtFilePath), StandardCharsets.UTF_8);

        // 处理每一行，转换为Markdown格式
        StringBuilder mdContent = new StringBuilder();
        boolean inCodeBlock = false;

        for (String line : lines) {
            // 处理空行
            if (line.trim().isEmpty()) {
                mdContent.append("\n");
                continue;
            }

            // 处理标题 (假设以多个#开头的是标题，或者以=====或-----分隔的是标题)
            String processedLine = processHeading(line);

            // 如果不是标题，处理列表
            if (processedLine.equals(line)) {
                processedLine = processList(line);
            }

            // 如果不是列表，处理粗体和斜体 (假设*包裹的是斜体，**包裹的是粗体)
            if (processedLine.equals(line)) {
                processedLine = processBoldAndItalic(line);
            }

            // 处理代码块 (假设以```开头和结尾)
            if (processedLine.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
            }

            // 如果在代码块中，不需要处理其他格式
            if (!inCodeBlock) {
                // 处理链接 (假设格式为 [文本](链接))
                processedLine = processLinks(processedLine);

                // 处理图片 (假设格式为 ![alt](图片路径))
                processedLine = processImages(processedLine);
            }

            mdContent.append(processedLine).append("\n");
        }

        // 写入Markdown文件
        Files.write(Paths.get(mdFilePath), mdContent.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 处理标题格式
     */
    private static String processHeading(String line) {
        // 处理以#开头的标题
        if (line.startsWith("#")) {
            return line;
        }

        // 处理以=====分隔的标题（一级标题）
        if (line.matches("^=+$")) {
            return "\n"; // 标题下方的分隔线，在Markdown中不需要
        }

        // 处理以-----分隔的标题（二级标题）
        if (line.matches("^-+$")) {
            return "\n"; // 标题下方的分隔线，在Markdown中不需要
        }

        // 假设以数字加点开头且后面有空格的可能是标题（如 "1. 标题"）
        if (line.matches("^\\d+\\.\\s+.+")) {
            // 判断是否是列表还是标题，这里简单处理为标题
            return "# " + line.replaceFirst("^\\d+\\.\\s+", "");
        }

        return line;
    }

    /**
     * 处理列表格式
     */
    private static String processList(String line) {
        // 处理无序列表（假设以*、+、-开头）
        if (line.matches("^[*+\\-]\\s+.+")) {
            return line;
        }

        // 处理有序列表（假设以数字.开头）
        if (line.matches("^\\d+\\.\\s+.+")) {
            return line;
        }

        // 处理嵌套列表（假设以制表符或多个空格开头）
        if (line.matches("^\\s+[*+\\-\\d.]\\s+.+")) {
            return line;
        }

        return line;
    }

    /**
     * 处理粗体和斜体
     */
    private static String processBoldAndItalic(String line) {
        // 处理粗体（假设以**包裹）
        line = line.replaceAll("\\*\\*(.+?)\\*\\*", "**$1**");

        // 处理斜体（假设以*包裹）
        line = line.replaceAll("\\*(.+?)\\*", "*$1*");

        return line;
    }

    /**
     * 处理链接
     */
    private static String processLinks(String line) {
        // 处理链接格式 [文本](链接)
        // 这里假设TXT中已经是类似格式，只是确保格式正确
        return line.replaceAll("\\[(.*?)\\]\\((.*?)\\)", "[$1]($2)");
    }

    /**
     * 处理图片
     */
    private static String processImages(String line) {
        // 处理图片格式 ![alt](路径)
        // 这里假设TXT中已经是类似格式，只是确保格式正确
        return line.replaceAll("!\\[(.*?)\\]\\((.*?)\\)", "![$1]($2)");
    }

}

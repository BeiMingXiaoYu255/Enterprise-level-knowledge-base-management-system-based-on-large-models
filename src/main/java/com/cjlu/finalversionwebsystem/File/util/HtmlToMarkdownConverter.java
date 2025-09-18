package com.cjlu.finalversionwebsystem.File.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * HTML转Markdown格式工具类
 * 需要依赖Jsoup库处理HTML解析
 */
public class HtmlToMarkdownConverter {

    /**
     * 将HTML文件转换为Markdown文件
     * @param htmlFilePath 源HTML文件路径
     * @param mdFilePath 目标Markdown文件路径
     * @throws IOException 当文件操作发生错误时抛出
     */
    public static void convert(String htmlFilePath, String mdFilePath) throws IOException {
        // 读取HTML文件内容
        String htmlContent = new String(Files.readAllBytes(Paths.get(htmlFilePath)), StandardCharsets.UTF_8);

        // 转换为Markdown
        String mdContent = convertHtmlToMarkdown(htmlContent);

        // 写入Markdown文件
        try (FileWriter writer = new FileWriter(mdFilePath, StandardCharsets.UTF_8)) {
            writer.write(mdContent);
        }
    }

    /**
     * 将HTML字符串转换为Markdown字符串
     * @param html HTML字符串
     * @return 转换后的Markdown字符串
     */
    public static String convertHtmlToMarkdown(String html) {
        // 使用Jsoup解析HTML
        Document doc = Jsoup.parse(html);

        // 处理<body>标签内的内容
        Element body = doc.body();
        if (body == null) {
            body = doc;
        }

        // 递归处理所有节点
        StringBuilder md = new StringBuilder();
        processNode(body, md, 0);

        // 清理多余的空行
        return cleanUpMarkdown(md.toString());
    }

    /**
     * 递归处理HTML节点
     */
    private static void processNode(Node node, StringBuilder md, int depth) {
        // 处理文本节点
        if (node instanceof TextNode) {
            String text = ((TextNode) node).text().trim();
            if (!text.isEmpty()) {
                md.append(escapeSpecialCharacters(text));
            }
            return;
        }

        // 处理元素节点
        if (!(node instanceof Element)) {
            return;
        }

        Element element = (Element) node;
        String tagName = element.tagName().toLowerCase();

        // 根据标签类型处理
        switch (tagName) {
            case "h1":
                md.append("\n# ").append(element.text()).append("\n\n");
                return;
            case "h2":
                md.append("\n## ").append(element.text()).append("\n\n");
                return;
            case "h3":
                md.append("\n### ").append(element.text()).append("\n\n");
                return;
            case "h4":
                md.append("\n#### ").append(element.text()).append("\n\n");
                return;
            case "h5":
                md.append("\n##### ").append(element.text()).append("\n\n");
                return;
            case "h6":
                md.append("\n###### ").append(element.text()).append("\n\n");
                return;
            case "p":
                processChildren(element, md, depth);
                md.append("\n\n");
                return;
            case "br":
                md.append("\n");
                return;
            case "hr":
                md.append("\n---\n\n");
                return;
            case "strong":
            case "b":
                md.append("**");
                processChildren(element, md, depth);
                md.append("**");
                return;
            case "em":
            case "i":
                md.append("*");
                processChildren(element, md, depth);
                md.append("*");
                return;
            case "code":
                md.append("`");
                processChildren(element, md, depth);
                md.append("`");
                return;
            case "pre":
                md.append("\n```\n");
                processChildren(element, md, depth);
                md.append("\n```\n\n");
                return;
            case "a":
                String href = element.attr("href");
                md.append("[");
                processChildren(element, md, depth);
                md.append("](").append(href).append(")");
                return;
            case "img":
                String src = element.attr("src");
                String alt = element.attr("alt");
                md.append("![").append(alt).append("](").append(src).append(")");
                return;
            case "ul":
                md.append("\n");
                processList(element, md, depth, false);
                md.append("\n");
                return;
            case "ol":
                md.append("\n");
                processList(element, md, depth, true);
                md.append("\n");
                return;
            case "li":
                // 列表项由父节点(ul/ol)处理
                break;
            case "blockquote":
                md.append("\n> ");
                processChildren(element, md, depth);
                md.append("\n\n");
                return;
            default:
                // 处理其他未明确指定的标签
                processChildren(element, md, depth);
        }
    }

    /**
     * 处理列表元素
     */
    private static void processList(Element listElement, StringBuilder md, int depth, boolean ordered) {
        Elements items = listElement.select("li");
        int index = 1;

        for (Element item : items) {
            // 添加列表前缀
            if (ordered) {
                md.append(index++).append(". ");
            } else {
                md.append("* ");
            }

            // 处理列表项内容
            processChildren(item, md, depth + 1);
            md.append("\n");
        }
    }

    /**
     * 处理子节点
     */
    private static void processChildren(Element parent, StringBuilder md, int depth) {
        for (Node child : parent.childNodes()) {
            processNode(child, md, depth);
        }
    }

    /**
     * 转义Markdown中的特殊字符
     */
    private static String escapeSpecialCharacters(String text) {
        return text.replaceAll("([*_`\\[\\]])", "\\\\$1");
    }

    /**
     * 清理Markdown中的多余空行和格式问题
     */
    private static String cleanUpMarkdown(String markdown) {
        // 合并多个空行为两个空行
        markdown = markdown.replaceAll("\n{3,}", "\n\n");
        // 移除开头和结尾的空行
        return markdown.trim() + "\n";
    }
}


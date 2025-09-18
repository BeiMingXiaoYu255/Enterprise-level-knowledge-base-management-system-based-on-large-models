package com.cjlu.finalversionwebsystem.File.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 视频字幕生成工具类
 * 功能：从视频中提取音频，将音频转换为文字，并生成字幕文件
 */
public class VideoSubtitleGenerator {

    // 临时文件目录
    private static final String TEMP_DIR = "temp_audio_files/";

    // 支持的视频格式
    private static final List<String> SUPPORTED_VIDEO_FORMATS = List.of(
            "mp4", "avi", "mov", "mkv", "flv", "wmv"
    );

    // 支持的字幕格式
    public enum SubtitleFormat {
        SRT, VTT, ASS
    }

    /**
     * 生成视频的字幕文件
     * @param videoFilePath 视频文件路径
     * @param outputDir 输出目录
     * @param format 字幕格式
     * @return 生成的字幕文件路径
     * @throws IOException 当文件操作失败时抛出
     * @throws InterruptedException 当外部进程被中断时抛出
     */
    public static String generateSubtitles(String videoFilePath, String outputDir, SubtitleFormat format)
            throws IOException, InterruptedException {
        // 验证视频文件
        validateVideoFile(videoFilePath);

        // 创建输出目录
        Files.createDirectories(Paths.get(outputDir));

        // 提取音频
        String audioFilePath = extractAudio(videoFilePath);

        try {
            // 将音频转换为文本片段（带时间戳）
            List<SubtitleSegment> segments = convertAudioToText(audioFilePath);

            // 生成字幕文件
            String outputFileName = createOutputFileName(videoFilePath, format);
            String outputFilePath = Paths.get(outputDir, outputFileName).toString();
            writeSubtitleFile(segments, outputFilePath, format);

            return outputFilePath;
        } finally {
            // 清理临时文件
            cleanupTempFiles(audioFilePath);
        }
    }

    /**
     * 验证视频文件是否有效
     */
    private static void validateVideoFile(String videoFilePath) throws IOException {
        Path videoPath = Paths.get(videoFilePath);

        // 检查文件是否存在
        if (!Files.exists(videoPath)) {
            throw new FileNotFoundException("视频文件不存在: " + videoFilePath);
        }

        // 检查文件是否可读
        if (!Files.isReadable(videoPath)) {
            throw new IOException("无法读取视频文件: " + videoFilePath);
        }

        // 检查文件格式是否支持
        String fileExtension = getFileExtension(videoFilePath);
        if (!SUPPORTED_VIDEO_FORMATS.contains(fileExtension.toLowerCase())) {
            throw new IOException("不支持的视频格式: " + fileExtension +
                    "，支持的格式: " + SUPPORTED_VIDEO_FORMATS);
        }
    }

    /**
     * 从视频中提取音频
     * 注意：此实现依赖FFmpeg工具，需要系统中安装FFmpeg
     */
    private static String extractAudio(String videoFilePath) throws IOException, InterruptedException {
        // 创建临时目录
        Files.createDirectories(Paths.get(TEMP_DIR));

        // 生成临时音频文件名
        String audioFileName = UUID.randomUUID().toString() + ".wav";
        String audioFilePath = Paths.get(TEMP_DIR, audioFileName).toString();

        // 使用FFmpeg提取音频
        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-i");
        command.add(videoFilePath);
        command.add("-vn"); // 不处理视频
        command.add("-acodec");
        command.add("pcm_s16le"); // 音频编码
        command.add("-ar");
        command.add("16000"); // 采样率
        command.add("-ac");
        command.add("1"); // 单声道
        command.add("-y"); // 覆盖现有文件
        command.add(audioFilePath);

        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        // 等待进程完成
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            // 读取错误输出
            String errorOutput = readStream(process.getInputStream());
            throw new IOException("提取音频失败，FFmpeg返回代码: " + exitCode + "\n" + errorOutput);
        }

        return audioFilePath;
    }

    /**
     * 将音频转换为带时间戳的文本片段
     * 注意：实际应用中需要集成语音识别API或库
     */
    private static List<SubtitleSegment> convertAudioToText(String audioFilePath) throws IOException {
        // 这里是语音识别的逻辑
        // 实际实现中可以集成:
        // 1. 本地语音识别库(如CMU Sphinx)
        // 2. 云服务API(如Google Cloud Speech-to-Text, AWS Transcribe等)

        // 以下是模拟数据，实际使用时需要替换为真实的语音识别实现
        List<SubtitleSegment> segments = new ArrayList<>();
        segments.add(new SubtitleSegment(1, 0, 5000, "大家好，欢迎观看这个视频。"));
        segments.add(new SubtitleSegment(2, 5000, 10000, "今天我们要介绍如何使用字幕生成工具。"));
        segments.add(new SubtitleSegment(3, 10000, 15000, "这个工具可以从视频中提取音频并转换为文字。"));

        return segments;
    }

    /**
     * 写入字幕文件
     */
    private static void writeSubtitleFile(List<SubtitleSegment> segments, String outputFilePath,
                                          SubtitleFormat format) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFilePath))) {
            switch (format) {
                case SRT:
                    writeSrtFile(segments, writer);
                    break;
                case VTT:
                    writeVttFile(segments, writer);
                    break;
                case ASS:
                    writeAssFile(segments, writer);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的字幕格式: " + format);
            }
        }
    }

    /**
     * 写入SRT格式字幕文件
     */
    private static void writeSrtFile(List<SubtitleSegment> segments, BufferedWriter writer) throws IOException {
        for (SubtitleSegment segment : segments) {
            // 序号
            writer.write(String.valueOf(segment.getIndex()));
            writer.newLine();

            // 时间戳
            writer.write(formatSrtTime(segment.getStartTime()) + " --> " +
                    formatSrtTime(segment.getEndTime()));
            writer.newLine();

            // 文本内容
            writer.write(segment.getText());
            writer.newLine();
            writer.newLine();
        }
    }

    /**
     * 写入WebVTT格式字幕文件
     */
    private static void writeVttFile(List<SubtitleSegment> segments, BufferedWriter writer) throws IOException {
        // VTT文件头部
        writer.write("WEBVTT");
        writer.newLine();
        writer.newLine();

        for (SubtitleSegment segment : segments) {
            // 时间戳
            writer.write(formatVttTime(segment.getStartTime()) + " --> " +
                    formatVttTime(segment.getEndTime()));
            writer.newLine();

            // 文本内容
            writer.write(segment.getText());
            writer.newLine();
            writer.newLine();
        }
    }

    /**
     * 写入ASS格式字幕文件
     */
    private static void writeAssFile(List<SubtitleSegment> segments, BufferedWriter writer) throws IOException {
        // ASS文件头部
        writer.write("[Script Info]");
        writer.newLine();
        writer.write("Title: Generated Subtitles");
        writer.newLine();
        writer.write("ScriptType: v4.00+");
        writer.newLine();
        writer.write("WrapStyle: 0");
        writer.newLine();
        writer.newLine();

        writer.write("[V4+ Styles]");
        writer.newLine();
        writer.write("Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding");
        writer.newLine();
        writer.write("Style: Default, Arial, 24, &H00FFFFFF, &H000000FF, &H00000000, &H00000000, 0, 0, 0, 0, 100, 100, 0, 0, 1, 2, 2, 2, 10, 10, 10, 0");
        writer.newLine();
        writer.newLine();

        writer.write("[Events]");
        writer.newLine();
        writer.write("Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text");
        writer.newLine();

        // 写入字幕内容
        for (SubtitleSegment segment : segments) {
            writer.write(String.format("Dialogue: 0,%s,%s,Default,,0,0,0,,%s",
                    formatAssTime(segment.getStartTime()),
                    formatAssTime(segment.getEndTime()),
                    segment.getText()));
            writer.newLine();
        }
    }

    /**
     * 格式化SRT时间（毫秒 -> HH:mm:ss,SSS）
     */
    private static String formatSrtTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long ms = milliseconds % 1000;

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, ms);
    }

    /**
     * 格式化VTT时间（毫秒 -> HH:mm:ss.SSS）
     */
    private static String formatVttTime(long milliseconds) {
        String srtTime = formatSrtTime(milliseconds);
        return srtTime.replace(',', '.');
    }

    /**
     * 格式化ASS时间（毫秒 -> H:MM:SS.cc）
     */
    private static String formatAssTime(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long centiseconds = (milliseconds % 1000) / 10;

        return String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds);
    }

    /**
     * 创建输出文件名
     */
    private static String createOutputFileName(String videoFilePath, SubtitleFormat format) {
        String baseName = new File(videoFilePath).getName();
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex != -1) {
            baseName = baseName.substring(0, dotIndex);
        }

        String extension;
        switch (format) {
            case SRT:
                extension = "srt";
                break;
            case VTT:
                extension = "vtt";
                break;
            case ASS:
                extension = "ass";
                break;
            default:
                extension = "srt";
        }

        return baseName + "." + extension;
    }

    /**
     * 清理临时文件
     */
    private static void cleanupTempFiles(String audioFilePath) {
        try {
            Files.deleteIfExists(Paths.get(audioFilePath));
        } catch (IOException e) {
            System.err.println("清理临时文件失败: " + e.getMessage());
        }
    }

    /**
     * 读取流内容
     */
    private static String readStream(InputStream inputStream) throws IOException {
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }

    /**
     * 获取文件扩展名
     */
    private static String getFileExtension(String filePath) {
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filePath.length() - 1) {
            return "";
        }
        return filePath.substring(dotIndex + 1);
    }

    /**
     * 字幕片段类，包含序号、开始时间、结束时间和文本内容
     */
    public static class SubtitleSegment {
        private final int index;
        private final long startTime; // 毫秒
        private final long endTime;   // 毫秒
        private final String text;

        public SubtitleSegment(int index, long startTime, long endTime, String text) {
            this.index = index;
            this.startTime = startTime;
            this.endTime = endTime;
            this.text = text;
        }

        public int getIndex() {
            return index;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public String getText() {
            return text;
        }
    }
}


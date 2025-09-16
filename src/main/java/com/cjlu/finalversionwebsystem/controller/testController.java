package com.cjlu.finalversionwebsystem.controller;

import com.cjlu.finalversionwebsystem.entity.KLB;
import com.cjlu.finalversionwebsystem.entity.Result;
import com.cjlu.finalversionwebsystem.mapper.KLBMapper;
import com.cjlu.finalversionwebsystem.utils.FileTestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/KLB")
@lombok.extern.slf4j.Slf4j
public class testController {

    @Autowired
    private KLBMapper klbMapper;

    @Autowired
    private FileTestUtil fileTestUtil;

    @PostMapping("/create")
    public Result createKLB(@RequestBody Map<String, Object> requsetbody) {
        try {
            String KLBName = (String) requsetbody.get("caseName");
            String KLBCreator = (String) requsetbody.get("caseCreater");
            String KLBdes = (String) requsetbody.get("caseDescription");

            KLB klb = new KLB();

            klb.setKLBName(KLBName);
            klb.setKLBCreator(KLBCreator);
            klb.setDescription(KLBdes);

            klbMapper.createKLBTable();
            klbMapper.creatNewKLB(klb);
            return Result.success("Knowledge Base created successfully");
        } catch (Exception e) {
            log.error("Failed to create Knowledge Base", e);
            return Result.error("Failed to create Knowledge Base: " + e.getMessage());
        }
    }

    @PostMapping("/search")
    public Result getKLBByCreator(@RequestBody Map<String,Object> requsetbody) {
        try {

            String KLBCreator = (String) requsetbody.get("caseCreater");

            List<KLB> klbs = klbMapper.selectKLBByKLBCreator(KLBCreator);
            if (klbs.isEmpty()) {
                return Result.error("No Knowledge Bases found for creator: " + KLBCreator);
            }
            return Result.success(klbs);
        } catch (Exception e) {
            log.error("Failed to get Knowledge Bases by creator", e);
            return Result.error("Failed to get Knowledge Bases by creator: " + e.getMessage());
        }
    }

    @GetMapping("/create-test-file")
    public Result createTestFile() {
        try {
            String content = "这是一个测试文件，用于测试文件聊天功能。\n\n" +
                    "文件内容包括：\n" +
                    "1. 人工智能技术的发展历程\n" +
                    "2. 机器学习的基本概念\n" +
                    "3. 深度学习的应用领域\n\n" +
                    "人工智能技术发展历程：\n" +
                    "人工智能（Artificial Intelligence，AI）是计算机科学的一个分支，它企图了解智能的实质，并生产出一种新的能以人类智能相似的方式做出反应的智能机器。\n\n" +
                    "机器学习基本概念：\n" +
                    "机器学习是人工智能的一个重要分支，它是一种通过算法使计算机系统能够自动学习和改进的技术。机器学习算法通过分析大量数据来识别模式，并使用这些模式来对新数据进行预测或决策。\n\n" +
                    "深度学习应用领域：\n" +
                    "深度学习是机器学习的一个子集，它使用多层神经网络来模拟人脑的工作方式。深度学习在图像识别、自然语言处理、语音识别等领域都有广泛的应用。\n\n" +
                    "主要应用包括：\n" +
                    "- 计算机视觉：图像分类、目标检测、人脸识别\n" +
                    "- 自然语言处理：机器翻译、文本分析、对话系统\n" +
                    "- 语音技术：语音识别、语音合成\n" +
                    "- 推荐系统：个性化推荐、内容过滤\n" +
                    "- 自动驾驶：环境感知、路径规划、决策控制\n\n" +
                    "这些技术正在改变我们的生活方式，为各行各业带来创新和效率提升。";

            fileTestUtil.createEncryptedTestFile("111.txt", content);
            return Result.success("测试文件111.txt创建成功");
        } catch (Exception e) {
            log.error("创建测试文件失败: {}", e.getMessage(), e);
            return Result.error("创建测试文件失败: " + e.getMessage());
        }
    }

}

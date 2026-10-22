package com.swu.gps.service;

import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.entity.RagDocument;
import com.swu.gps.model.request.AiChatRequest;
import com.swu.gps.model.response.AiChatResponse;
import com.swu.gps.repository.GuideLocationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiChatService {

    private final RagService ragService;
    private final GuideLocationRepository guideLocationRepository;
    private final DeepSeekClient deepSeekClient;
    private final CampusAiAnalyzer campusAiAnalyzer;
    private final AiQueryEnhancer queryEnhancer;
    private final int maxHistory;

    public AiChatService(RagService ragService,
                         GuideLocationRepository guideLocationRepository,
                         DeepSeekClient deepSeekClient,
                         CampusAiAnalyzer campusAiAnalyzer,
                         AiQueryEnhancer queryEnhancer,
                         @Value("${app.ai.max-history:16}") int maxHistory) {
        this.ragService = ragService;
        this.guideLocationRepository = guideLocationRepository;
        this.deepSeekClient = deepSeekClient;
        this.campusAiAnalyzer = campusAiAnalyzer;
        this.queryEnhancer = queryEnhancer;
        this.maxHistory = Math.max(4, maxHistory);
    }

    @Transactional(readOnly = true)
    public AiChatResponse chat(AiChatRequest request) {
        String question = request.message().trim();
        List<GuideLocation> locations = guideLocationRepository.findAll();

        CampusAiAnalyzer.Intent intent = campusAiAnalyzer.detectIntent(question);
        String expandedQuery = queryEnhancer.expandForRetrieval(question, intent, locations);
        List<RagDocument> docs = mergeDocuments(
                ragService.retrieveForQuery(question),
                ragService.retrieveForQuery(expandedQuery)
        );
        GuideLocation matched = queryEnhancer.matchLocation(question, locations);

        if (!deepSeekClient.isConfigured()) {
            CampusAiAnalyzer.AnalysisResult local = campusAiAnalyzer.analyze(question, docs, locations);
            return new AiChatResponse(local.reply(), local.sources());
        }

        List<String> sources = queryEnhancer.mergeSources(docs, matched);
        String retrievalBrief = queryEnhancer.buildRetrievalBrief(question, intent, docs, matched);
        String systemPrompt = buildWiseSystemPrompt(docs, buildLocationContext(locations), retrievalBrief);

        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.ChatMessage("system", systemPrompt));
        appendHistory(messages, request);
        messages.add(new DeepSeekClient.ChatMessage("user", question));

        try {
            String reply = deepSeekClient.chat(messages);
            return new AiChatResponse(reply, sources);
        } catch (Exception ex) {
            CampusAiAnalyzer.AnalysisResult fallback = campusAiAnalyzer.analyze(question, docs, locations);
            String note = "\n\n（云端「" + deepSeekClient.getModel() + "」暂不可用，已切换本地知识库分析）";
            return new AiChatResponse(fallback.reply() + note, fallback.sources());
        }
    }

    private void appendHistory(List<DeepSeekClient.ChatMessage> messages, AiChatRequest request) {
        if (request.history() == null || request.history().isEmpty()) {
            return;
        }
        int skip = Math.max(0, request.history().size() - maxHistory);
        request.history().stream()
                .skip(skip)
                .filter(item -> item.role() != null && item.content() != null)
                .filter(item -> "user".equalsIgnoreCase(item.role()) || "assistant".equalsIgnoreCase(item.role()))
                .forEach(item -> messages.add(new DeepSeekClient.ChatMessage(
                        item.role().toLowerCase(),
                        item.content().trim()
                )));
    }

    private List<RagDocument> mergeDocuments(List<RagDocument> a, List<RagDocument> b) {
        Map<Long, RagDocument> map = new LinkedHashMap<>();
        if (a != null) {
            a.forEach(d -> map.put(d.getId(), d));
        }
        if (b != null) {
            b.forEach(d -> map.putIfAbsent(d.getId(), d));
        }
        return new ArrayList<>(map.values());
    }

    private String buildWiseSystemPrompt(List<RagDocument> docs, String locationContext, String retrievalBrief) {
        String modelHint = deepSeekClient.isReasonerModel()
                ? "你当前运行在深度推理模式，请充分思考后再给出精炼结论。"
                : "请像专业 AI 助手一样先理解再回答，逻辑清晰、信息准确。";

        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是「西南大学智畅游」首席 AI 校园向导，具备深度理解、推理与归纳能力，服务西南大学师生与访客。
                """);
        sb.append(modelHint).append("\n\n");
        sb.append("""
                ## 你的智慧标准（对标高质量 AI 助手）
                - **听懂言外之意**：处理口语、省略、多轮对话中的「它/那里/第一个」等指代，结合对话历史理解真实需求。
                - **先结论后展开**：首段直接回答核心；再分点论证，避免空泛套话。
                - **会分析会比较**：路线题对比步行/校车/校外交通；美食题可按距离/场景分类；校史题突出时间线与要点。
                - **灵活与严谨并重**：当用户询问校园事务时，优先依据下方知识库与讲解点；遇到无依据的专有政策/票价时不编造。当用户询问与校园无关的通用问题（如写代码、聊天、百科常识等）时，请直接调用你的通用大语言模型知识库热情解答，不要提“知识库没有”这件事。
                - **可执行建议**：结尾给出 1～2 条用户立刻能做的下一步（如：在地图搜索某讲解点、查看官方公众号）。

                ## 回答前请在内心完成（不要输出冗长推理过程）
                1. 用户真正问题是什么？与上文有何关联？
                2. 哪些知识库/讲解点相关？如何组织答案最有帮助？
                3. 是否需要澄清？若问题仅为数字或极短词，友好引导并举例。

                ## 输出格式
                - 简体中文，专业亲切
                - 关键信息用 **加粗**
                - 路线类：**校外到达** / **校内到达** 分开说明
                - 适当使用分点，单条回答建议 150～500 字（复杂问题可更长）

                ## 当前检索分析
                """);
        sb.append(retrievalBrief).append("\n");

        sb.append("\n## 知识库（回答时必须优先引用）\n");
        if (docs.isEmpty()) {
            sb.append("（注：当前检索未命中校园专有知识。如果用户问的是通用百科或日常聊天，请直接用你的通用知识自然作答；如果是校园具体事务且你确实不知道，再说明暂无该信息）\n");
        } else {
            for (RagDocument doc : docs) {
                sb.append("### ").append(doc.getTitle())
                        .append(" [").append(doc.getCategory().name()).append("]\n")
                        .append(trimContent(doc.getContent(), 1500))
                        .append("\n\n");
            }
        }
        sb.append("## 校园讲解点（地点、坐标、简介）\n").append(locationContext);
        return sb.toString();
    }

    private String buildLocationContext(List<GuideLocation> locations) {
        if (locations.isEmpty()) {
            return "（暂无讲解点数据）";
        }
        StringBuilder sb = new StringBuilder();
        for (GuideLocation loc : locations) {
            sb.append("- **").append(loc.getName()).append("**");
            if (loc.getAddress() != null && !loc.getAddress().trim().isEmpty()) {
                sb.append(" | 地址：").append(loc.getAddress());
            }
            sb.append(" | 坐标：").append(loc.getLatitude()).append(",").append(loc.getLongitude());
            if (loc.getDescription() != null && !loc.getDescription().trim().isEmpty()) {
                sb.append(" | ").append(trimContent(loc.getDescription(), 180));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String trimContent(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim().replace("\r\n", "\n");
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "…";
    }
}

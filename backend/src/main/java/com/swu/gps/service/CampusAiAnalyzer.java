package com.swu.gps.service;

import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.entity.RagCategory;
import com.swu.gps.model.entity.RagDocument;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class CampusAiAnalyzer {

    public record AnalysisResult(String reply, List<String> sources) {
    }

    public enum Intent {
        GREETING, UNCLEAR, HISTORY, NEWS, FOOD, BUS, ROUTE, LOCATION, GENERAL
    }

    public AnalysisResult analyze(String question, List<RagDocument> docs, List<GuideLocation> locations) {
        String q = question == null ? "" : question.trim();
        Intent intent = detectIntent(q);
        List<String> sources = new ArrayList<>();

        if (intent == Intent.GREETING) {
            return new AnalysisResult(buildGreeting(), Collections.emptyList());
        }
        if (intent == Intent.UNCLEAR) {
            return new AnalysisResult(buildClarifyPrompt(q), Collections.emptyList());
        }

        GuideLocation matched = matchLocation(q, locations);
        if (matched != null) {
            intent = Intent.LOCATION;
        }

        StringBuilder reply = new StringBuilder();
        reply.append("**问题类型**：").append(intentLabel(intent)).append("\n\n");

        switch (intent) {
            case LOCATION:
                sources.add(matched.getName());
                reply.append(buildLocationAnswer(matched));
                appendRelatedRag(reply, docs, EnumSet.of(RagCategory.ROUTE, RagCategory.BUS), sources);
                break;
            case ROUTE:
            case BUS:
                if (matched != null) {
                    sources.add(matched.getName());
                    reply.append(buildLocationAnswer(matched)).append("\n");
                }
                appendRagSection(reply, docs, sources, "路线与交通参考");
                if (matched == null && locations != null && !locations.isEmpty()) {
                    reply.append("\n**校内讲解点**（可在地图中搜索导航）：\n");
                    locations.stream().limit(6).forEach(loc -> reply.append("• ")
                            .append(loc.getName()).append("\n"));
                }
                break;
            case FOOD:
                appendRagSection(reply, docs, sources, "美食与周边");
                break;
            case HISTORY:
                appendRagSection(reply, docs, sources, "校史与文化");
                break;
            case NEWS:
                appendRagSection(reply, docs, sources, "校园资讯");
                break;
            default:
                reply.append("**综合分析**\n");
                appendRagSection(reply, docs, sources, "相关知识要点");
                if (docs.size() >= 2) {
                    reply.append("\n**归纳**：以上条目可结合您的具体问题理解；配置 DeepSeek 后将获得更深度的推理回答。\n");
                }
                break;
        }

        if (sources.isEmpty() && docs.isEmpty()) {
            reply.append("\n暂未检索到匹配知识。配置后端 `app.deepseek.api-key` 后可启用深度推理；也可尝试：\n")
                    .append("• 「西南大学校史」\n")
                    .append("• 「怎么去中心图书馆」\n")
                    .append("• 「北碚周边有什么好吃的」\n")
                    .append("• 「校车怎么坐」");
        } else {
            reply.append("\n\n**建议**：在首页地图搜索讲解点名称，可查看位置并规划步行路线。");
        }

        return new AnalysisResult(reply.toString().trim(), sources.stream().distinct().collect(Collectors.toList()));
    }

    public Intent detectIntent(String q) {
        if (q.isEmpty()) {
            return Intent.UNCLEAR;
        }
        if (q.length() <= 2 && q.matches("[\\d\\p{Punct}\\s]+")) {
            return Intent.UNCLEAR;
        }
        if (matchesAny(q, "你好", "您好", "hi", "hello", "在吗")) {
            return Intent.GREETING;
        }
        if (matchesAny(q, "校史", "历史", "创办", "建校", "沿革", "校训")) {
            return Intent.HISTORY;
        }
        if (matchesAny(q, "资讯", "新闻", "通知", "活动", "讲座", "开放日")) {
            return Intent.NEWS;
        }
        if (matchesAny(q, "美食", "好吃", "餐厅", "火锅", "小面", "小吃", "吃饭", "外卖")) {
            return Intent.FOOD;
        }
        if (matchesAny(q, "校车", "摆渡", "公交", "地铁", "6号线", "轨道交通", "坐车")) {
            return Intent.BUS;
        }
        if (matchesAny(q, "怎么去", "怎么走", "如何去", "路线", "导航", "前往", "到达", "到")) {
            return Intent.ROUTE;
        }
        return Intent.GENERAL;
    }

    private GuideLocation matchLocation(String q, List<GuideLocation> locations) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        String lower = q.toLowerCase(Locale.ROOT);
        GuideLocation best = null;
        int bestLen = 0;
        for (GuideLocation loc : locations) {
            String name = loc.getName();
            if (name == null || name.trim().isEmpty()) {
                continue;
            }
            if (lower.contains(name.toLowerCase(Locale.ROOT)) && name.length() > bestLen) {
                best = loc;
                bestLen = name.length();
            }
        }
        return best;
    }

    private void appendRagSection(StringBuilder reply, List<RagDocument> docs, List<String> sources, String title) {
        if (docs == null || docs.isEmpty()) {
            reply.append("**").append(title).append("**\n暂无匹配条目，管理员可在 Web「AI 知识库」补充。\n");
            return;
        }
        reply.append("**").append(title).append("**\n");
        for (RagDocument doc : docs) {
            sources.add(doc.getTitle());
            reply.append("• **").append(doc.getTitle()).append("**：")
                    .append(trim(doc.getContent(), 400))
                    .append("\n");
        }
    }

    private void appendRelatedRag(StringBuilder reply, List<RagDocument> docs,
                                  Set<RagCategory> categories, List<String> sources) {
        if (docs == null) {
            return;
        }
        List<RagDocument> related = docs.stream()
                .filter(d -> categories.contains(d.getCategory()))
                .limit(2)
                .collect(Collectors.toList());
        if (!related.isEmpty()) {
            appendRagSection(reply, related, sources, "相关交通信息");
        }
    }

    private String buildLocationAnswer(GuideLocation loc) {
        StringBuilder sb = new StringBuilder();
        sb.append("**目的地分析**：").append(loc.getName()).append("\n\n");
        if (loc.getAddress() != null && !loc.getAddress().trim().isEmpty()) {
            sb.append("• **地址**：").append(loc.getAddress()).append("\n");
        }
        sb.append("• **坐标**：").append(loc.getLatitude()).append(", ").append(loc.getLongitude()).append("\n");
        if (loc.getDescription() != null && !loc.getDescription().trim().isEmpty()) {
            sb.append("• **简介**：").append(trim(loc.getDescription(), 300)).append("\n");
        }
        sb.append("• **到达建议**：校内可步行或乘坐校车；校外可从轨道交通 6 号线北碚站换乘公交至对应校门，再在地图中导航至该点。");
        return sb.toString();
    }

    private String buildGreeting() {
        return "您好！我是西南大学智畅游 AI 助手。\n\n" +
                "我可以帮您**分析**并回答：\n" +
                "1. 校史与文化\n" +
                "2. 校园资讯与活动\n" +
                "3. 周边美食推荐\n" +
                "4. 校车与公共交通\n" +
                "5. 校内地点怎么走\n\n" +
                "请用完整句子提问，例如：「西南大学什么时候成立的？」「怎么去中心图书馆？」";
    }

    private String buildClarifyPrompt(String q) {
        return String.format(
                "您好，您发送的「%s」比较简短，我还无法判断具体需求。\n\n" +
                        "**您可以这样问我：**\n" +
                        "1. 校史 — 「西南大学是怎么发展起来的？」\n" +
                        "2. 资讯 — 「最近校园有什么活动？」\n" +
                        "3. 美食 — 「学校附近有什么好吃的？」\n" +
                        "4. 交通 — 「校内校车怎么坐？」\n" +
                        "5. 路线 — 「怎么去中心图书馆？」\n\n" +
                        "直接输入完整问题，或点击下方快捷问题即可。",
                escapeFormat(q)
        );
    }

    private String intentLabel(Intent intent) {
        switch (intent) {
            case HISTORY:
                return "校史文化";
            case NEWS:
                return "校园资讯";
            case FOOD:
                return "美食推荐";
            case BUS:
                return "校车与公交";
            case ROUTE:
                return "路线规划";
            case LOCATION:
                return "校内地点";
            case GREETING:
                return "问候";
            case UNCLEAR:
                return "待澄清";
            default:
                return "综合咨询";
        }
    }

    private boolean matchesAny(String q, String... keywords) {
        String lower = q.toLowerCase(Locale.ROOT);
        for (String kw : keywords) {
            if (lower.contains(kw.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String trim(String text, int max) {
        if (text == null) {
            return "";
        }
        String t = text.trim().replace("\r\n", "\n");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private String escapeFormat(String s) {
        return s.replace("%", "%%");
    }
}

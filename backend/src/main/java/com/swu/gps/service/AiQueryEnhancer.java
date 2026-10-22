package com.swu.gps.service;

import com.swu.gps.model.entity.GuideLocation;
import com.swu.gps.model.entity.RagDocument;
import com.swu.gps.service.CampusAiAnalyzer.Intent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AiQueryEnhancer {

    public String expandForRetrieval(String question, Intent intent, List<GuideLocation> locations) {
        Set<String> terms = new LinkedHashSet<>();
        String q = question == null ? "" : question.trim();
        if (!q.trim().isEmpty()) {
            terms.add(q);
        }
        switch (intent) {
            case HISTORY:
                terms.addAll(Arrays.asList("西南大学", "校史", "历史", "建校", "双一流"));
                break;
            case NEWS:
                terms.addAll(Arrays.asList("校园", "资讯", "活动", "通知"));
                break;
            case FOOD:
                terms.addAll(Arrays.asList("美食", "餐饮", "北碚", "天生路", "小吃"));
                break;
            case BUS:
                terms.addAll(Arrays.asList("校车", "公交", "地铁", "6号线", "北碚站", "交通"));
                break;
            case ROUTE:
            case LOCATION:
                terms.addAll(Arrays.asList("路线", "怎么去", "导航", "步行", "校车"));
                break;
            default:
                break;
        }
        if (locations != null) {
            String lower = q.toLowerCase(Locale.ROOT);
            for (GuideLocation loc : locations) {
                if (loc.getName() != null && !loc.getName().trim().isEmpty()
                        && lower.contains(loc.getName().toLowerCase(Locale.ROOT))) {
                    terms.add(loc.getName());
                    if (loc.getAddress() != null) {
                        terms.add(loc.getAddress());
                    }
                }
            }
        }
        return String.join(" ", terms);
    }

    public String buildRetrievalBrief(String question, Intent intent,
                                      List<RagDocument> docs, GuideLocation matched) {
        StringBuilder sb = new StringBuilder();
        sb.append("- 用户问题：").append(question).append("\n");
        sb.append("- 意图判断：").append(intentLabel(intent)).append("\n");
        if (matched != null) {
            sb.append("- 匹配讲解点：").append(matched.getName());
            sb.append("（").append(matched.getLatitude()).append(", ").append(matched.getLongitude()).append("）\n");
        }
        if (docs != null && !docs.isEmpty()) {
            sb.append("- 命中知识库 ").append(docs.size()).append(" 条：")
                    .append(docs.stream().map(RagDocument::getTitle).collect(Collectors.joining("、")))
                    .append("\n");
        } else {
            sb.append("- 知识库：未命中强相关条目，请结合讲解点与通识谨慎推理\n");
        }
        return sb.toString();
    }

    public GuideLocation matchLocation(String question, List<GuideLocation> locations) {
        if (locations == null || question == null) {
            return null;
        }
        String lower = question.toLowerCase(Locale.ROOT);
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

    public List<String> mergeSources(List<RagDocument> docs, GuideLocation matched) {
        List<String> sources = new ArrayList<>();
        if (docs != null) {
            docs.stream().map(RagDocument::getTitle).forEach(sources::add);
        }
        if (matched != null) {
            sources.add(matched.getName());
        }
        return sources.stream().distinct().collect(Collectors.toList());
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
                return "问候寒暄";
            case UNCLEAR:
                return "意图待澄清";
            default:
                return "综合咨询";
        }
    }
}

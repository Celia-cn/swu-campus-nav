package com.example.gps.util;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 将景点讲解文案拆成段落，供详情页分段展示。
 */
public final class SpotDetailTextUtil {

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[。！？!?])\\s*");

    private SpotDetailTextUtil() {
    }

    @NonNull
    public static List<String> splitParagraphs(@Nullable String raw) {
        if (TextUtils.isEmpty(raw)) {
            return new ArrayList<>();
        }
        String normalized = raw.replace("\r\n", "\n").trim();
        Set<String> ordered = new LinkedHashSet<>();

        if (normalized.contains("\n\n")) {
            for (String block : normalized.split("\\n\\n+")) {
                addBlock(ordered, block);
            }
        } else if (normalized.contains("\n")) {
            for (String line : normalized.split("\\n+")) {
                addBlock(ordered, line);
            }
        } else {
            addBlock(ordered, normalized);
        }

        if (ordered.size() == 1) {
            String only = ordered.iterator().next();
            if (only.length() > 120 && only.contains("。")) {
                ordered.clear();
                for (String sentence : SENTENCE_END.split(only)) {
                    addBlock(ordered, sentence);
                }
            }
        }

        return new ArrayList<>(ordered);
    }

    private static void addBlock(@NonNull Set<String> target, @Nullable String block) {
        if (block == null) {
            return;
        }
        String trimmed = block.trim();
        if (!trimmed.isEmpty()) {
            target.add(trimmed);
        }
    }
}

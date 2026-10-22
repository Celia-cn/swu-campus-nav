package com.example.gps.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gps.R;
import com.example.gps.network.ApiClient;
import com.example.gps.network.ApiService;
import com.example.gps.network.dto.AiChatDto;
import com.example.gps.network.dto.AiChatReq;
import com.example.gps.network.dto.ApiResponse;
import com.example.gps.storage.SessionManager;
import com.example.gps.ui.adapter.AiMessageAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AiChatActivity extends AppCompatActivity {

    private static final Gson GSON = new Gson();
    private static final Type API_TYPE = new TypeToken<ApiResponse<AiChatDto>>() {}.getType();

    private AiMessageAdapter adapter;
    private ApiService apiService;
    private EditText etInput;
    private MaterialButton btnSend;
    private boolean sending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!SessionManager.from(this).isLoggedIn()) {
            Toast.makeText(this, R.string.toast_feedback_need_login, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_ai_chat);
        apiService = ApiClient.service(this);
        applyWindowInsets();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView rvMessages = findViewById(R.id.rvMessages);
        adapter = new AiMessageAdapter();
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        etInput = findViewById(R.id.etInput);
        btnSend = findViewById(R.id.btnSend);

        adapter.add(new AiMessageAdapter.Message(false, getString(R.string.ai_chat_welcome)));
        setupSuggestionChips();

        btnSend.setOnClickListener(v -> sendMessage());
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void applyWindowInsets() {
        View content = findViewById(R.id.aiChatRoot);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        View bottomArea = findViewById(R.id.chatBottomArea);
        if (content == null || toolbar == null || bottomArea == null) {
            return;
        }

        final int toolbarPadTop = toolbar.getPaddingTop();
        final int toolbarPadBottom = toolbar.getPaddingBottom();
        final int bottomPadBottom = bottomArea.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    toolbarPadTop + bars.top,
                    toolbar.getPaddingRight(),
                    toolbarPadBottom
            );
            bottomArea.setPadding(
                    bottomArea.getPaddingLeft(),
                    bottomArea.getPaddingTop(),
                    bottomArea.getPaddingRight(),
                    bottomPadBottom + bars.bottom
            );
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private void setupSuggestionChips() {
        ChipGroup group = findViewById(R.id.chipSuggestions);
        addChip(group, R.string.ai_chat_chip_history, "西南大学校史与发展概况");
        addChip(group, R.string.ai_chat_chip_food, "北碚校区周边有什么好吃的？");
        addChip(group, R.string.ai_chat_chip_bus, "校内校车和校外公交怎么坐？");
        addChip(group, R.string.ai_chat_chip_route, "怎么去中心图书馆？");
    }

    private void addChip(ChipGroup group, int labelRes, String query) {
        Chip chip = new Chip(this);
        chip.setText(getString(labelRes));
        chip.setCheckable(false);
        chip.setChipBackgroundColorResource(R.color.surface_soft);
        chip.setOnClickListener(v -> {
            etInput.setText(query);
            sendMessage();
        });
        group.addView(chip);
    }

    private void sendMessage() {
        if (sending) {
            return;
        }
        String text = etInput.getText() == null ? "" : etInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        etInput.setText("");
        adapter.add(new AiMessageAdapter.Message(true, text));
        scrollToBottom();

        List<AiChatReq.ChatMessageItem> history = buildHistoryExcludingLast();
        sending = true;
        setSendingUi(true);
        adapter.add(new AiMessageAdapter.Message(false, getString(R.string.ai_chat_typing_deep), true, null));
        scrollToBottom();

        apiService.aiChat(new AiChatReq(text, history)).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AiChatDto>> call,
                                   @NonNull Response<ApiResponse<AiChatDto>> response) {
                sending = false;
                setSendingUi(false);
                removeTypingBubble();

                ApiResponse<AiChatDto> body = response.body();
                if (body == null && !response.isSuccessful()) {
                    body = parseErrorBody(response);
                }
                if (body == null || body.code != 0 || body.data == null || TextUtils.isEmpty(body.data.reply)) {
                    String msg = body != null && !TextUtils.isEmpty(body.message)
                            ? body.message
                            : getString(R.string.ai_chat_failed);
                    adapter.add(new AiMessageAdapter.Message(false, msg));
                    scrollToBottom();
                    return;
                }
                adapter.add(new AiMessageAdapter.Message(false, body.data.reply, false, body.data.sources));
                scrollToBottom();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AiChatDto>> call, @NonNull Throwable t) {
                sending = false;
                setSendingUi(false);
                removeTypingBubble();
                String hint = t.getMessage() != null ? t.getMessage() : "";
                adapter.add(new AiMessageAdapter.Message(false,
                        getString(R.string.ai_chat_failed) + "\n（" + hint + "，请检查网络与后端地址）"));
                scrollToBottom();
            }
        });
    }

    private void removeTypingBubble() {
        List<AiMessageAdapter.Message> items = adapter.getItems();
        if (!items.isEmpty() && items.get(items.size() - 1).typing) {
            adapter.removeLast();
        }
    }

    private ApiResponse<AiChatDto> parseErrorBody(Response<ApiResponse<AiChatDto>> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            return GSON.fromJson(response.errorBody().string(), API_TYPE);
        } catch (IOException ignored) {
            return null;
        }
    }

    private List<AiChatReq.ChatMessageItem> buildHistoryExcludingLast() {
        List<AiChatReq.ChatMessageItem> history = new java.util.ArrayList<>();
        List<AiMessageAdapter.Message> items = adapter.getItems();
        int end = Math.max(0, items.size() - 1);
        String welcome = getString(R.string.ai_chat_welcome);
        for (int i = 0; i < end; i++) {
            AiMessageAdapter.Message item = items.get(i);
            if (item.typing) {
                continue;
            }
            if (welcome.equals(item.text) && !item.fromUser) {
                continue;
            }
            history.add(new AiChatReq.ChatMessageItem(
                    item.fromUser ? "user" : "assistant",
                    item.text
            ));
        }
        return history;
    }

    private void setSendingUi(boolean busy) {
        btnSend.setEnabled(!busy);
        etInput.setEnabled(!busy);
        findViewById(R.id.chipSuggestions).setEnabled(!busy);
    }

    private void scrollToBottom() {
        RecyclerView rv = findViewById(R.id.rvMessages);
        int count = adapter.getItemCount();
        if (count > 0) {
            rv.post(() -> rv.smoothScrollToPosition(count - 1));
        }
    }

}

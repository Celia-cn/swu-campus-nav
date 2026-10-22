package com.example.gps.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gps.R;
import com.example.gps.network.dto.LocationDto;
import com.example.gps.storage.SessionManager;

import java.util.Locale;

/**
 * 自动 / 手动讲解音频触发：TTS 播报与 MediaPlayer 播放景点音频。
 */
public class AudioTriggerService implements TextToSpeech.OnInitListener {

    public interface Host {
        void showToast(@NonNull String message);
    }

    public interface NarrationProvider {
        @NonNull
        String build(@NonNull LocationDto location);
    }

    private final Context context;
    private final SessionManager session;
    private final Host host;
    @Nullable
    private NarrationProvider narrationProvider;
    @Nullable
    private TextToSpeech tts;
    private boolean ttsReady;
    @Nullable
    private MediaPlayer mediaPlayer;
    private long currentGuideLocationId = -1L;
    private boolean guidePlaying;

    public AudioTriggerService(@NonNull Context context,
                               @NonNull SessionManager session,
                               @NonNull Host host) {
        this.context = context.getApplicationContext();
        this.session = session;
        this.host = host;
        tts = new TextToSpeech(context, this);
    }

    public void setNarrationProvider(@Nullable NarrationProvider provider) {
        this.narrationProvider = provider;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && tts != null) {
            int result = tts.setLanguage(Locale.CHINESE);
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        guidePlaying = false;
                    }

                    @Override
                    public void onError(String utteranceId) {
                        guidePlaying = false;
                    }
                });
            }
        } else {
            ttsReady = false;
        }
    }

    public void playGuide(@NonNull LocationDto location, boolean auto) {
        if (auto) {
            host.showToast(context.getString(R.string.toast_auto_play_prefix, location.name));
        }
        currentGuideLocationId = location.id;
        boolean hasCustomAudio = location.audioUrl != null && !location.audioUrl.trim().isEmpty();
        if (hasCustomAudio) {
            stopTts();
            playAudio(resolveMediaUrl(location.audioUrl.trim()));
            return;
        }
        String script = buildGuideNarration(location);
        if (ttsReady && tts != null && !TextUtils.isEmpty(script)) {
            guidePlaying = true;
            String utteranceId = "guide_" + location.id;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(script, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                tts.speak(script, TextToSpeech.QUEUE_FLUSH, null);
            }
            return;
        }
        guidePlaying = false;
    }

    /** 停止当前讲解（TTS 与录音）。 */
    public void stopGuide() {
        stopTts();
        stopAudio();
        guidePlaying = false;
    }

    public boolean isGuidePlayingFor(long locationId) {
        return guidePlaying && currentGuideLocationId == locationId;
    }

    private void stopTts() {
        if (tts != null) {
            tts.stop();
        }
        guidePlaying = false;
    }

    public void playAudio(@NonNull String url) {
        playAudio(url, currentGuideLocationId);
    }

    private void playAudio(@NonNull String url, long locationId) {
        stopAudio();
        currentGuideLocationId = locationId;
        guidePlaying = true;
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build());
        mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        mediaPlayer.setOnCompletionListener(mp -> guidePlaying = false);
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            guidePlaying = false;
            host.showToast(context.getString(R.string.toast_audio_failed));
            return true;
        });
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            guidePlaying = false;
            host.showToast(context.getString(R.string.toast_audio_url_invalid));
        }
    }

    public void stopAll() {
        stopGuide();
    }

    public void stopAudio() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void shutdown() {
        stopAll();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ttsReady = false;
    }

    @NonNull
    private String resolveMediaUrl(@NonNull String raw) {
        String value = raw.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        String path = value.startsWith("/") ? value.substring(1) : value;
        return session.getBaseUrl() + path;
    }

    @NonNull
    private String buildGuideNarration(@NonNull LocationDto location) {
        if (narrationProvider != null) {
            return narrationProvider.build(location);
        }
        String name = location.name == null ? "该景点" : location.name.trim();
        String desc = location.description == null ? "" : location.description.trim();
        if (desc.isEmpty()) {
            return "欢迎来到" + name + "。";
        }
        if (desc.length() > 180) {
            desc = desc.substring(0, 180) + "…";
        }
        return "欢迎来到" + name + "。" + desc;
    }
}

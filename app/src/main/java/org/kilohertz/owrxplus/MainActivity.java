package org.kilohertz.owrxplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final String OWRX_URL = "https://sdr.kilohertz021.org/";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;

    private WebView webView;
    private TextView frequencyText;
    private TextView statusText;
    private LinearLayout controlPanel;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean controlsVisible = true;
    private final Runnable statusPoller = new Runnable() {
        @Override
        public void run() {
            pollReceiverStatus();
            uiHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        configureWebView(webView);

        setContentView(createLayout());

        if (savedInstanceState == null) {
            webView.loadUrl(OWRX_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }

        requestNotificationsIfNeeded();
        startKeepAliveService();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView view) {
        WebSettings settings = view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                startStatusPolling();
            }
        });
        view.setWebChromeClient(new WebChromeClient());
        view.setKeepScreenOn(true);
        view.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    private View createLayout() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        root.addView(createTopOverlay(), topOverlayParams());

        controlPanel = createControlPanel();
        root.addView(controlPanel, bottomPanelParams());
        return root;
    }

    private LinearLayout createTopOverlay() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(6));
        bar.setBackground(panelBackground(0xCC08151B, dp(0)));

        frequencyText = new TextView(this);
        frequencyText.setText("OWRX+");
        frequencyText.setTextColor(Color.WHITE);
        frequencyText.setTextSize(20);
        frequencyText.setTypeface(Typeface.DEFAULT_BOLD);
        frequencyText.setSingleLine(true);
        bar.addView(frequencyText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        statusText = new TextView(this);
        statusText.setText("Connecting");
        statusText.setTextColor(0xFFB7E4CE);
        statusText.setTextSize(12);
        statusText.setGravity(Gravity.END);
        statusText.setSingleLine(true);
        bar.addView(statusText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button toggle = createButton("Hide");
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControls();
            }
        });
        bar.addView(toggle, new LinearLayout.LayoutParams(dp(74), dp(38)));
        return bar;
    }

    private LinearLayout createControlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(8), dp(8), dp(10));
        panel.setBackground(panelBackground(0xDD08151B, dp(10)));

        panel.addView(buttonRow(
                control("Tune -", "if (typeof tuneBySteps==='function') tuneBySteps(-1);"),
                control("Tune +", "if (typeof tuneBySteps==='function') tuneBySteps(1);"),
                control("Step -", stepScript(-1)),
                control("Step +", stepScript(1)),
                control("Mute", "if (window.UI && typeof UI.toggleMute==='function') UI.toggleMute();"),
                control("Reload", "location.reload();")
        ));

        panel.addView(buttonRow(
                control("Rx", panelScript("openwebrx-panel-receiver")),
                control("Status", panelScript("openwebrx-panel-status")),
                control("Log", panelScript("openwebrx-panel-log")),
                control("Zoom -", "if (typeof zoomOutOneStep==='function') zoomOutOneStep();"),
                control("Zoom +", "if (typeof zoomInOneStep==='function') zoomInOneStep();"),
                control("WF Auto", "if (window.Waterfall && typeof Waterfall.setAutoRange==='function') Waterfall.setAutoRange();")
        ));

        return panel;
    }

    private HorizontalScrollView buttonRow(Button... buttons) {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        for (Button button : buttons) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(86), dp(42));
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(button, params);
        }

        scrollView.addView(row);
        return scrollView;
    }

    private Button control(String label, final String script) {
        Button button = createButton(label);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runReceiverScript(script);
            }
        });
        return button;
    }

    private Button createButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setBackground(panelBackground(0xEE12313C, dp(8)));
        return button;
    }

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        controlPanel.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
    }

    private String stepScript(int direction) {
        String indexChange = direction < 0
                ? "Math.max(0,s.selectedIndex-1)"
                : "Math.min(s.options.length-1,s.selectedIndex+1)";
        return "var s=document.getElementById('openwebrx-tuning-step-listbox');"
                + "if(s){s.selectedIndex=" + indexChange + ";"
                + "s.dispatchEvent(new Event('change'));}";
    }

    private String panelScript(String panelId) {
        return "var b=document.querySelector('[data-toggle-panel=\"" + panelId + "\"]');"
                + "if(b){b.click();}";
    }

    private void runReceiverScript(String script) {
        if (webView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        webView.evaluateJavascript("(function(){" + script + "})()", null);
    }

    private void startStatusPolling() {
        uiHandler.removeCallbacks(statusPoller);
        uiHandler.post(statusPoller);
    }

    private void pollReceiverStatus() {
        if (webView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        webView.evaluateJavascript(
                "(function(){"
                        + "var f=document.querySelector('.webrx-actual-freq');"
                        + "var m=document.querySelector('#openwebrx-smeter-db');"
                        + "var c=document.querySelector('#openwebrx-clock-utc');"
                        + "return JSON.stringify({"
                        + "freq:f&&f.textContent?f.textContent.trim():'OWRX+',"
                        + "meter:m&&m.textContent?m.textContent.trim():'',"
                        + "clock:c&&c.textContent?c.textContent.trim():''"
                        + "});"
                        + "})()",
                value -> updateReceiverStatus(value)
        );
    }

    private void updateReceiverStatus(String rawValue) {
        String value = unquoteJsString(rawValue);
        String freq = extractJsonValue(value, "freq");
        String meter = extractJsonValue(value, "meter");
        String clock = extractJsonValue(value, "clock");

        if (freq.length() > 0) {
            frequencyText.setText(freq);
        }

        StringBuilder status = new StringBuilder();
        if (meter.length() > 0) {
            status.append(meter);
        }
        if (clock.length() > 0) {
            if (status.length() > 0) {
                status.append("  ");
            }
            status.append(clock);
        }
        statusText.setText(status.length() > 0 ? status.toString() : "Running");
    }

    private String unquoteJsString(String rawValue) {
        if (rawValue == null || rawValue.equals("null")) {
            return "";
        }

        String value = rawValue;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private String extractJsonValue(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int start = json.indexOf(needle);
        if (start < 0) {
            return "";
        }
        start += needle.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            return "";
        }
        return json.substring(start, end).replace("\\\"", "\"").trim();
    }

    private FrameLayout.LayoutParams topOverlayParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(56)
        );
        params.gravity = Gravity.TOP;
        return params;
    }

    private FrameLayout.LayoutParams bottomPanelParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM;
        params.setMargins(dp(8), 0, dp(8), dp(8));
        return params;
    }

    private GradientDrawable panelBackground(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), 0x6631D27C);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST
            );
        }
    }

    private void startKeepAliveService() {
        Intent intent = new Intent(this, PlaybackKeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacks(statusPoller);
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}

package org.kilohertz.owrxplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String DEFAULT_RECEIVER_NAME = "kilohertz_sdr";
    private static final String DEFAULT_RECEIVER_URL = "https://sdr.kilohertz021.org/";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1001;
    private static final String PREFS = "signaldeck";
    private static final String PREF_RECEIVER_NAME = "receiver_name";
    private static final String PREF_RECEIVER_URL = "receiver_url";

    private WebView webView;
    private TextView brandText;
    private TextView frequencyText;
    private TextView statusText;
    private LinearLayout controlPanel;
    private FrameLayout receiverDrawer;
    private LinearLayout receiverListView;
    private EditText receiverSearch;
    private ReceiverCatalog receiverCatalog;
    private ReceiverInfo currentReceiver;
    private final List<ReceiverInfo> receivers = new ArrayList<ReceiverInfo>();
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

        receiverCatalog = new ReceiverCatalog();
        currentReceiver = loadSavedReceiver();

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        configureWebView(webView);

        setContentView(createLayout());

        if (savedInstanceState == null) {
            webView.loadUrl(currentReceiver.url);
        } else {
            webView.restoreState(savedInstanceState);
        }

        requestNotificationsIfNeeded();
        startKeepAliveService();
        seedFallbackReceivers();
        refreshReceivers();
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

        receiverDrawer = createReceiverDrawer();
        receiverDrawer.setVisibility(View.GONE);
        root.addView(receiverDrawer, drawerParams());
        return root;
    }

    private LinearLayout createTopOverlay() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(6));
        bar.setBackground(panelBackground(0xD9081116, 0, 0x5531D27C));

        LinearLayout textStack = new LinearLayout(this);
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.setGravity(Gravity.CENTER_VERTICAL);

        brandText = new TextView(this);
        brandText.setText("SignalDeck  |  " + currentReceiver.name);
        brandText.setTextColor(0xFFB7E4CE);
        brandText.setTextSize(12);
        brandText.setSingleLine(true);
        textStack.addView(brandText);

        frequencyText = new TextView(this);
        frequencyText.setText("Connecting");
        frequencyText.setTextColor(Color.WHITE);
        frequencyText.setTextSize(21);
        frequencyText.setTypeface(Typeface.DEFAULT_BOLD);
        frequencyText.setSingleLine(true);
        textStack.addView(frequencyText);

        bar.addView(textStack, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        statusText = new TextView(this);
        statusText.setText("OpenWebRX compatible");
        statusText.setTextColor(0xFFB7E4CE);
        statusText.setTextSize(11);
        statusText.setGravity(Gravity.END);
        statusText.setSingleLine(true);
        bar.addView(statusText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button list = createButton("List");
        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReceiverDrawer();
            }
        });
        bar.addView(list, new LinearLayout.LayoutParams(dp(64), dp(38)));

        Button toggle = createButton("Hide");
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleControls();
            }
        });
        bar.addView(toggle, new LinearLayout.LayoutParams(dp(64), dp(38)));
        return bar;
    }

    private LinearLayout createControlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(panelBackground(0xEA081116, dp(14), 0x6631D27C));

        LinearLayout deck = new LinearLayout(this);
        deck.setGravity(Gravity.CENTER);
        deck.setOrientation(LinearLayout.HORIZONTAL);

        deck.addView(sideColumn(
                control("Rx", panelScript("openwebrx-panel-receiver")),
                control("Status", panelScript("openwebrx-panel-status")),
                control("Log", panelScript("openwebrx-panel-log"))
        ), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TuningKnobView knob = new TuningKnobView(this);
        knob.setListener(new TuningKnobView.Listener() {
            @Override
            public void onTick(int direction) {
                tune(direction);
            }

            @Override
            public void onCenterTap() {
                cycleTuningStep();
            }
        });
        LinearLayout.LayoutParams knobParams = new LinearLayout.LayoutParams(dp(168), dp(168));
        knobParams.setMargins(dp(8), 0, dp(8), 0);
        deck.addView(knob, knobParams);

        deck.addView(sideColumn(
                control("Mute", "if (window.UI && typeof UI.toggleMute==='function') UI.toggleMute();"),
                control("Zoom", "if (typeof zoomInOneStep==='function') zoomInOneStep();"),
                control("Auto", "if (window.Waterfall && typeof Waterfall.setAutoRange==='function') Waterfall.setAutoRange();")
        ), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        panel.addView(deck);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER);
        bottom.addView(control("Step", stepScript(1)), smallButtonParams());
        bottom.addView(control("Zoom -", "if (typeof zoomOutOneStep==='function') zoomOutOneStep();"), smallButtonParams());
        bottom.addView(control("Reload", "location.reload();"), smallButtonParams());
        panel.addView(bottom);

        return panel;
    }

    private LinearLayout sideColumn(Button first, Button second, Button third) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        column.addView(first, columnButtonParams());
        column.addView(second, columnButtonParams());
        column.addView(third, columnButtonParams());
        return column;
    }

    private FrameLayout createReceiverDrawer() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xCC000000);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(12), dp(12), dp(12), dp(12));
        sheet.setBackground(panelBackground(0xF20A1217, dp(16), 0x8831D27C));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("OpenWebRX receivers");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button refresh = createButton("Refresh");
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshReceivers();
            }
        });
        header.addView(refresh, new LinearLayout.LayoutParams(dp(92), dp(40)));

        Button close = createButton("Close");
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiverDrawer.setVisibility(View.GONE);
            }
        });
        header.addView(close, new LinearLayout.LayoutParams(dp(82), dp(40)));
        sheet.addView(header);

        TextView note = new TextView(this);
        note.setText("Independent client. Opens compatible OpenWebRX endpoints from rx-tx.info.");
        note.setTextColor(0xFFB7E4CE);
        note.setTextSize(12);
        note.setPadding(0, dp(4), 0, dp(8));
        sheet.addView(note);

        receiverSearch = new EditText(this);
        receiverSearch.setSingleLine(true);
        receiverSearch.setHint("Search country, city, callsign...");
        receiverSearch.setHintTextColor(0x77FFFFFF);
        receiverSearch.setTextColor(Color.WHITE);
        receiverSearch.setTextSize(14);
        receiverSearch.setPadding(dp(10), 0, dp(10), 0);
        receiverSearch.setBackground(panelBackground(0xFF101C22, dp(8), 0x4431D27C));
        receiverSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderReceivers();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        sheet.addView(receiverSearch, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
        ));

        ScrollView scrollView = new ScrollView(this);
        receiverListView = new LinearLayout(this);
        receiverListView.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(receiverListView);
        sheet.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(dp(10), dp(70), dp(10), dp(12));
        overlay.addView(sheet, params);
        return overlay;
    }

    private void seedFallbackReceivers() {
        receivers.clear();
        receivers.addAll(receiverCatalog.fallbackReceivers());
        renderReceivers();
    }

    private void refreshReceivers() {
        if (receiverListView != null) {
            receiverListView.removeAllViews();
            receiverListView.addView(messageView("Loading OpenWebRX receivers from rx-tx.info..."));
        }

        receiverCatalog.loadOpenWebRxReceivers(new ReceiverCatalog.Callback() {
            @Override
            public void onLoaded(List<ReceiverInfo> loadedReceivers, String source) {
                receivers.clear();
                receivers.add(currentReceiver);
                for (ReceiverInfo receiver : loadedReceivers) {
                    if (!containsUrl(receivers, receiver.url)) {
                        receivers.add(receiver);
                    }
                }
                renderReceivers();
                statusText.setText(receivers.size() + " receivers");
            }

            @Override
            public void onError(List<ReceiverInfo> fallback, String message) {
                receivers.clear();
                receivers.addAll(fallback);
                renderReceivers();
                statusText.setText("Catalog fallback");
            }
        });
    }

    private boolean containsUrl(List<ReceiverInfo> list, String url) {
        for (ReceiverInfo receiver : list) {
            if (receiver.url.equalsIgnoreCase(url)) {
                return true;
            }
        }
        return false;
    }

    private void renderReceivers() {
        if (receiverListView == null) {
            return;
        }

        receiverListView.removeAllViews();
        String filter = receiverSearch == null ? "" : receiverSearch.getText().toString().toLowerCase(Locale.US).trim();
        int shown = 0;
        for (final ReceiverInfo receiver : receivers) {
            if (!matchesFilter(receiver, filter)) {
                continue;
            }
            receiverListView.addView(receiverRow(receiver));
            shown++;
            if (shown >= 300) {
                receiverListView.addView(messageView("Showing first 300 matches. Narrow search for more."));
                break;
            }
        }

        if (shown == 0) {
            receiverListView.addView(messageView("No matching OpenWebRX receivers."));
        }
    }

    private boolean matchesFilter(ReceiverInfo receiver, String filter) {
        if (filter.length() == 0) {
            return true;
        }
        String haystack = (receiver.name + " " + receiver.subtitle() + " " + receiver.url).toLowerCase(Locale.US);
        return haystack.contains(filter);
    }

    private View receiverRow(final ReceiverInfo receiver) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(panelBackground(
                receiver.url.equals(currentReceiver.url) ? 0xFF18383E : 0xFF0E1A20,
                dp(8),
                receiver.url.equals(currentReceiver.url) ? 0xAA31D27C : 0x3331D27C
        ));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(5), 0, dp(5));
        row.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setText(receiver.name);
        name.setTextColor(Color.WHITE);
        name.setTextSize(15);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(name);

        TextView meta = new TextView(this);
        meta.setText(receiver.subtitle());
        meta.setTextColor(0xFFB7E4CE);
        meta.setTextSize(12);
        row.addView(meta);

        TextView url = new TextView(this);
        url.setText(receiver.url);
        url.setTextColor(0x99FFFFFF);
        url.setTextSize(11);
        row.addView(url);

        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectReceiver(receiver);
            }
        });
        return row;
    }

    private TextView messageView(String message) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(0xFFB7E4CE);
        view.setTextSize(13);
        view.setPadding(dp(8), dp(12), dp(8), dp(12));
        return view;
    }

    private void selectReceiver(ReceiverInfo receiver) {
        currentReceiver = receiver;
        saveReceiver(receiver);
        brandText.setText("SignalDeck  |  " + receiver.name);
        frequencyText.setText("Loading");
        statusText.setText(receiver.subtitle());
        receiverDrawer.setVisibility(View.GONE);
        webView.loadUrl(receiver.url);
    }

    private ReceiverInfo loadSavedReceiver() {
        SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        return new ReceiverInfo(
                preferences.getString(PREF_RECEIVER_NAME, DEFAULT_RECEIVER_NAME),
                preferences.getString(PREF_RECEIVER_URL, DEFAULT_RECEIVER_URL),
                "JN95wg",
                "RS",
                "Novi Sad",
                45.267,
                19.833
        );
    }

    private void saveReceiver(ReceiverInfo receiver) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(PREF_RECEIVER_NAME, receiver.name)
                .putString(PREF_RECEIVER_URL, receiver.url)
                .apply();
    }

    private void toggleReceiverDrawer() {
        receiverDrawer.setVisibility(receiverDrawer.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
        if (receiverDrawer.getVisibility() == View.VISIBLE) {
            renderReceivers();
        }
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
        button.setPadding(dp(3), 0, dp(3), 0);
        button.setBackground(panelBackground(0xFF12313C, dp(8), 0x5531D27C));
        return button;
    }

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        controlPanel.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
    }

    private void tune(int direction) {
        runReceiverScript("if (typeof tuneBySteps==='function') tuneBySteps(" + direction + ");");
    }

    private void cycleTuningStep() {
        runReceiverScript(stepScript(1));
    }

    private String stepScript(int direction) {
        String indexChange = direction < 0
                ? "Math.max(0,s.selectedIndex-1)"
                : "(s.selectedIndex+1)%s.options.length";
        return "var s=document.getElementById('openwebrx-tuning-step-listbox');"
                + "if(s&&s.options.length){s.selectedIndex=" + indexChange + ";"
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
                        + "var s=document.getElementById('openwebrx-tuning-step-listbox');"
                        + "var step=s&&s.options[s.selectedIndex]?s.options[s.selectedIndex].text:'';"
                        + "return JSON.stringify({"
                        + "freq:f&&f.textContent?f.textContent.trim():'',"
                        + "meter:m&&m.textContent?m.textContent.trim():'',"
                        + "clock:c&&c.textContent?c.textContent.trim():'',"
                        + "step:step"
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
        String step = extractJsonValue(value, "step");

        if (freq.length() > 0) {
            frequencyText.setText(freq);
        }

        StringBuilder status = new StringBuilder();
        if (step.length() > 0) {
            status.append(step);
        }
        if (meter.length() > 0) {
            if (status.length() > 0) {
                status.append("  ");
            }
            status.append(meter);
        }
        if (clock.length() > 0) {
            if (status.length() > 0) {
                status.append("  ");
            }
            status.append(clock);
        }
        if (status.length() > 0) {
            statusText.setText(status.toString());
        }
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

    private LinearLayout.LayoutParams columnButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(42)
        );
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        return params;
    }

    private LinearLayout.LayoutParams smallButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(40), 1);
        params.setMargins(dp(4), dp(8), dp(4), 0);
        return params;
    }

    private FrameLayout.LayoutParams topOverlayParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(58)
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

    private FrameLayout.LayoutParams drawerParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
    }

    private GradientDrawable panelBackground(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
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
        if (receiverDrawer != null && receiverDrawer.getVisibility() == View.VISIBLE) {
            receiverDrawer.setVisibility(View.GONE);
        } else if (webView.canGoBack()) {
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

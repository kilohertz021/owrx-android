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
import android.view.MotionEvent;
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
    private static final int COLOR_PANEL = 0xEA07121B;
    private static final int COLOR_PANEL_STRONG = 0xF207121B;
    private static final int COLOR_BUTTON = 0xFF10283A;
    private static final int COLOR_ROW = 0xFF0B1A25;
    private static final int COLOR_ROW_SELECTED = 0xFF123046;
    private static final int COLOR_TEXT_ICE = 0xFFD7F6FF;
    private static final int COLOR_TEXT_MUTED = 0xFF9FB5C3;
    private static final int COLOR_BORDER = 0x668EDCFF;
    private static final int COLOR_BORDER_STRONG = 0xAA9FEAFF;

    private WebView webView;
    private TextView brandText;
    private TextView utcText;
    private TextView frequencyText;
    private LinearLayout controlPanel;
    private LinearLayout collapsedPanel;
    private FrameLayout receiverDrawer;
    private LinearLayout receiverListView;
    private EditText receiverSearch;
    private TuningKnobView tuningKnob;
    private ReceiverCatalog receiverCatalog;
    private ReceiverInfo currentReceiver;
    private final List<ReceiverInfo> receivers = new ArrayList<ReceiverInfo>();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private boolean deckExpanded = true;
    private float deckTouchStartY;
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
                injectSignalDeckSkin();
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

        root.addView(createNativeArrowBlocker(), nativeArrowBlockerParams());

        controlPanel = createControlPanel();
        root.addView(controlPanel, bottomPanelParams());

        collapsedPanel = createCollapsedPanel();
        collapsedPanel.setVisibility(View.GONE);
        root.addView(collapsedPanel, collapsedPanelParams());

        receiverDrawer = createReceiverDrawer();
        receiverDrawer.setVisibility(View.GONE);
        root.addView(receiverDrawer, drawerParams());
        return root;
    }

    private LinearLayout createDeckHeader() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(6), 0, dp(6), dp(8));

        LinearLayout textStack = new LinearLayout(this);
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.setGravity(Gravity.CENTER_VERTICAL);

        brandText = new TextView(this);
        brandText.setText("SignalDeck  |  " + currentReceiver.name);
        brandText.setTextColor(COLOR_TEXT_ICE);
        brandText.setTextSize(12);
        brandText.setSingleLine(true);
        textStack.addView(brandText);

        utcText = new TextView(this);
        utcText.setText("");
        utcText.setTextColor(COLOR_TEXT_MUTED);
        utcText.setTextSize(10);
        utcText.setSingleLine(true);
        textStack.addView(utcText);

        frequencyText = new TextView(this);
        frequencyText.setText("Connecting");
        frequencyText.setTextColor(Color.WHITE);
        frequencyText.setTextSize(21);
        frequencyText.setTypeface(Typeface.DEFAULT_BOLD);
        frequencyText.setSingleLine(true);
        textStack.addView(frequencyText);

        bar.addView(textStack, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return bar;
    }

    private LinearLayout createControlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(panelBackground(COLOR_PANEL, dp(8), COLOR_BORDER));
        panel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    deckTouchStartY = event.getRawY();
                    return true;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (event.getRawY() - deckTouchStartY > dp(36)) {
                        setDeckExpanded(false);
                        return true;
                    }
                }
                return false;
            }
        });

        panel.addView(createDeckHeader());

        LinearLayout deck = new LinearLayout(this);
        deck.setGravity(Gravity.CENTER);
        deck.setOrientation(LinearLayout.HORIZONTAL);

        tuningKnob = new TuningKnobView(this);
        tuningKnob.setListener(new TuningKnobView.Listener() {
            @Override
            public void onTick(int direction) {
                tune(direction);
            }

            @Override
            public void onCenterTap() {
                cycleTuningStep();
            }
        });
        LinearLayout.LayoutParams knobParams = new LinearLayout.LayoutParams(0, dp(164), 1);
        knobParams.setMargins(dp(8), 0, dp(8), 0);
        deck.addView(tuningKnob, knobParams);

        deck.addView(sideColumn(
                receiverPanelButton(),
                receiverListButton(),
                control("Zoom +", "if (typeof zoomInOneStep==='function') zoomInOneStep();"),
                control("Zoom -", "if (typeof zoomOutOneStep==='function') zoomOutOneStep();")
        ), new LinearLayout.LayoutParams(dp(118), LinearLayout.LayoutParams.WRAP_CONTENT));

        panel.addView(deck);

        return panel;
    }

    private LinearLayout createCollapsedPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(10), dp(8), dp(10), dp(8));
        panel.setBackground(panelBackground(COLOR_PANEL, dp(8), COLOR_BORDER));
        panel.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    deckTouchStartY = event.getRawY();
                    return true;
                }
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (deckTouchStartY - event.getRawY() > dp(24)) {
                        setDeckExpanded(true);
                        return true;
                    }
                }
                return false;
            }
        });

        TextView title = new TextView(this);
        title.setText("SignalDeck");
        title.setTextColor(Color.WHITE);
        title.setTextSize(14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        panel.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button receivers = createButton("SDRs");
        receivers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReceiverDrawer();
            }
        });
        panel.addView(receivers, new LinearLayout.LayoutParams(dp(98), dp(40)));

        Button deck = createButton("Deck");
        deck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDeckExpanded(true);
            }
        });
        panel.addView(deck, new LinearLayout.LayoutParams(dp(72), dp(40)));
        return panel;
    }

    private LinearLayout sideColumn(Button... buttons) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        for (Button button : buttons) {
            column.addView(button, columnButtonParams());
        }
        return column;
    }

    private View createNativeArrowBlocker() {
        View blocker = new View(this);
        blocker.setBackgroundColor(Color.TRANSPARENT);
        blocker.setClickable(true);
        blocker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideNativeOpenWebRxPanels();
            }
        });
        return blocker;
    }

    private FrameLayout createReceiverDrawer() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xCC000000);

        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(dp(12), dp(12), dp(12), dp(12));
        sheet.setBackground(panelBackground(COLOR_PANEL_STRONG, dp(8), COLOR_BORDER_STRONG));

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
        note.setTextColor(COLOR_TEXT_MUTED);
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
        receiverSearch.setBackground(panelBackground(COLOR_ROW, dp(6), COLOR_BORDER));
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
        params.setMargins(dp(10), dp(10), dp(10), dp(12));
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
                updateDeckMeta(receivers.size() + " receivers");
            }

            @Override
            public void onError(List<ReceiverInfo> fallback, String message) {
                receivers.clear();
                receivers.addAll(fallback);
                renderReceivers();
                updateDeckMeta("Catalog fallback");
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
                receiver.url.equals(currentReceiver.url) ? COLOR_ROW_SELECTED : COLOR_ROW,
                dp(6),
                receiver.url.equals(currentReceiver.url) ? COLOR_BORDER_STRONG : COLOR_BORDER
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
        meta.setTextColor(COLOR_TEXT_MUTED);
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
        view.setTextColor(COLOR_TEXT_MUTED);
        view.setTextSize(13);
        view.setPadding(dp(8), dp(12), dp(8), dp(12));
        return view;
    }

    private void selectReceiver(ReceiverInfo receiver) {
        currentReceiver = receiver;
        saveReceiver(receiver);
        brandText.setText("SignalDeck  |  " + receiver.name);
        frequencyText.setText("Loading");
        if (utcText != null) {
            utcText.setText("");
        }
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

    private Button receiverListButton() {
        Button button = createButton("SDRs");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReceiverDrawer();
            }
        });
        return button;
    }

    private Button receiverPanelButton() {
        Button button = createButton("Receiver");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReceiverPanel();
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
        button.setBackground(panelBackground(COLOR_BUTTON, dp(6), COLOR_BORDER));
        return button;
    }

    private void setDeckExpanded(boolean expanded) {
        deckExpanded = expanded;
        controlPanel.setVisibility(deckExpanded ? View.VISIBLE : View.GONE);
        collapsedPanel.setVisibility(deckExpanded ? View.GONE : View.VISIBLE);
    }

    private void tune(int direction) {
        runReceiverScript("if (typeof tuneBySteps==='function') tuneBySteps(" + direction + ");");
    }

    private void cycleTuningStep() {
        runReceiverScript(stepScript(1));
    }

    private void toggleReceiverPanel() {
        runReceiverScript(
                "var panel=document.getElementById('openwebrx-panel-receiver');"
                        + "var visible=panel&&getComputedStyle(panel).display!=='none'&&panel.getClientRects().length>0;"
                        + "if(panel&&visible){panel.style.display='none';}"
                        + "else if(panel){panel.removeAttribute('hidden');"
                        + "panel.style.display='block';panel.style.visibility='visible';panel.style.opacity='1';}"
                        + "else{var b=document.querySelector('[data-toggle-panel=\"openwebrx-panel-receiver\"]');if(b){b.click();}}"
                        + "var panels=document.querySelectorAll('[id^=\"openwebrx-panel-\"]');"
                        + "for(var i=0;i<panels.length;i++){if(panels[i].id!=='openwebrx-panel-receiver'){panels[i].style.display='none';}}"
        );
    }

    private void hideNativeOpenWebRxPanels() {
        runReceiverScript(
                "var panels=document.querySelectorAll('[id^=\"openwebrx-panel-\"]');"
                        + "for(var i=0;i<panels.length;i++){if(panels[i].id!=='openwebrx-panel-receiver'){panels[i].style.display='none';}}"
                        + "var nodes=document.body?document.body.querySelectorAll('*'):[];"
                        + "for(var j=0;j<nodes.length;j++){var text=(nodes[j].textContent||'').replace(/\\s+/g,' ').trim().toLowerCase();"
                        + "if(text==='antena'||text==='antenna'||text.indexOf('autor:')===0||text.indexOf('author:')===0){nodes[j].style.display='none';}}"
        );
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

    private void injectSignalDeckSkin() {
        if (webView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        webView.evaluateJavascript(signalDeckSkinScriptV2(), null);
    }

    private String signalDeckSkinScriptV2() {
        return "(function(){"
                + "if(window.__signalDeckSkinInstalled){return;}"
                + "window.__signalDeckSkinInstalled=true;"
                + "document.documentElement.classList.add('signaldeck-skin');"
                + "var css='"
                + ".signaldeck-skin body{background:#050b12!important;}"
                + ".signaldeck-skin .webrx-top-container{background:linear-gradient(180deg,rgba(5,13,21,.95),rgba(5,13,21,.48))!important;box-shadow:0 10px 28px rgba(0,0,0,.3)!important;}"
                + ".signaldeck-skin .webrx-top-bar{min-height:54px!important;padding:6px 10px 12px!important;align-items:center!important;gap:8px!important;}"
                + ".signaldeck-skin .webrx-top-logo{display:none!important;}"
                + ".signaldeck-skin .webrx-rx-avatar{width:42px!important;height:42px!important;border-radius:4px!important;opacity:.92!important;border:1px solid rgba(159,234,255,.46)!important;box-shadow:0 0 22px rgba(120,214,255,.18),0 8px 18px rgba(0,0,0,.28)!important;}"
                + ".signaldeck-skin .webrx-rx-texts{min-width:0!important;max-width:calc(100vw - 76px)!important;}"
                + ".signaldeck-skin .webrx-rx-title{font-size:15px!important;line-height:17px!important;margin:0!important;color:#edf8ff!important;white-space:nowrap!important;overflow:hidden!important;text-overflow:ellipsis!important;letter-spacing:.2px!important;}"
                + ".signaldeck-skin .webrx-rx-desc{font-size:11px!important;line-height:13px!important;color:#9fb5c3!important;white-space:nowrap!important;overflow:hidden!important;text-overflow:ellipsis!important;}"
                + ".signaldeck-skin .openwebrx-main-buttons{display:none!important;}"
                + ".signaldeck-skin #pstt{display:none!important;}"
                + ".signaldeck-skin [id^=openwebrx-panel-]:not(#openwebrx-panel-receiver){display:none!important;pointer-events:none!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle{display:none!important;pointer-events:none!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle:before{content:\\'\\';position:absolute!important;left:50%!important;top:4px!important;margin-left:-8px!important;width:0!important;height:0!important;border-left:8px solid transparent!important;border-right:8px solid transparent!important;border-top:9px solid #d7f6ff!important;filter:drop-shadow(0 0 7px rgba(159,234,255,.78))!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle:after{content:\\'RECEIVER\\';position:absolute!important;left:0!important;right:0!important;bottom:1px!important;text-align:center!important;color:rgba(215,246,255,.72)!important;font:700 7px/8px sans-serif!important;letter-spacing:.8px!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle.sd-pull{transform:translateY(9px)!important;transition:transform .1s ease-out!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver{position:fixed!important;left:0!important;right:0!important;top:126px!important;width:auto!important;max-width:none!important;height:34vh!important;max-height:34vh!important;overflow-y:auto!important;overflow-x:hidden!important;z-index:80!important;border-radius:4px!important;background:rgba(7,18,27,.98)!important;border:1px solid rgba(159,234,255,.5)!important;box-shadow:0 0 30px rgba(120,214,255,.16),0 18px 42px rgba(0,0,0,.5)!important;padding:10px 12px 12px!important;backdrop-filter:blur(8px)!important;overscroll-behavior:contain!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver:before{content:\\'Receiver\\';position:sticky;top:-10px;z-index:2;display:block;margin:-10px -12px 8px;padding:10px 12px 8px;color:#edf8ff;background:rgba(7,18,27,.99);border-bottom:1px solid rgba(159,234,255,.24);font:700 13px/18px sans-serif;letter-spacing:.4px;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-panel-line{margin:6px 0!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver select,.signaldeck-skin #openwebrx-panel-receiver input{border-radius:4px!important;background:#091824!important;color:#edf8ff!important;border:1px solid rgba(159,234,255,.28)!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-button{border-radius:4px!important;background:rgba(16,40,58,.92)!important;border:1px solid rgba(159,234,255,.38)!important;color:#fff!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-section-divider{border-radius:3px!important;background:rgba(5,13,21,.72)!important;color:#d7f6ff!important;padding:6px 8px!important;margin-top:8px!important;border-color:rgba(159,234,255,.32)!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-modes .openwebrx-button{min-height:34px!important;font-weight:700!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver [data-signaldeck-hidden=true],.signaldeck-skin #openwebrx-panel-receiver [id*=settings],.signaldeck-skin #openwebrx-panel-receiver [id*=display],.signaldeck-skin #openwebrx-panel-receiver [class*=settings],.signaldeck-skin #openwebrx-panel-receiver [class*=display]{display:none!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-hint{transform:translateX(18px)!important;transition:transform .12s ease-out!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-up-hint{transform:translateY(-24px)!important;transition:transform .12s ease-out!important;}"
                + "@media (orientation:landscape){.signaldeck-skin #signaldeck-receiver-handle{top:78px!important;}.signaldeck-skin #openwebrx-panel-receiver{top:104px!important;height:42vh!important;max-height:42vh!important;}}"
                + "';"
                + "var style=document.createElement('style');style.id='signaldeck-skin-style';style.textContent=css;document.head.appendChild(style);"
                + "function receiverToggle(){return document.querySelector('[data-toggle-panel=\"openwebrx-panel-receiver\"]');}"
                + "function receiverVisible(panel){if(!panel){return false;}var s=getComputedStyle(panel);return s.display!=='none'&&s.visibility!=='hidden'&&panel.getClientRects().length>0;}"
                + "function showReceiver(){var panel=document.getElementById('openwebrx-panel-receiver');if(panel){panel.removeAttribute('hidden');panel.style.display='block';panel.style.visibility='visible';panel.style.opacity='1';hideReceiverSections();return;}var toggle=receiverToggle();if(toggle){toggle.click();}}"
                + "function hideReceiver(){var panel=document.getElementById('openwebrx-panel-receiver');if(panel){panel.style.display='none';return;}var toggle=receiverToggle();if(toggle){toggle.click();}}"
                + "function hideForeignPanels(){var panels=document.querySelectorAll('[id^=\"openwebrx-panel-\"]');for(var i=0;i<panels.length;i++){if(panels[i].id!=='openwebrx-panel-receiver'){panels[i].style.display='none';panels[i].style.pointerEvents='none';}}}"
                + "function ownText(el){var out='';for(var i=0;i<el.childNodes.length;i++){if(el.childNodes[i].nodeType===3){out+=el.childNodes[i].nodeValue+' ';}}return out.replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "function hideReceiverSections(){var panel=document.getElementById('openwebrx-panel-receiver');if(!panel){return;}var nodes=panel.querySelectorAll('*');for(var i=0;i<nodes.length;i++){var text=(ownText(nodes[i])||nodes[i].textContent||'').replace(/[.:>-]/g,' ').replace(/\\s+/g,' ').trim().toLowerCase();if(text.indexOf('settings')>=0||text.indexOf('display')>=0){var section=nodes[i].closest('.openwebrx-section,.openwebrx-panel-section,.openwebrx-panel-line,fieldset,details')||nodes[i];section.style.display='none';section.setAttribute('data-signaldeck-hidden','true');var node=section.nextElementSibling;while(node){var nextText=(ownText(node)||node.textContent||'').replace(/[.:>-]/g,' ').replace(/\\s+/g,' ').trim().toLowerCase();if(nextText.indexOf('controls')>=0||nextText.indexOf('modes')>=0){break;}if(nextText.indexOf('settings')>=0||nextText.indexOf('display')>=0||node.getAttribute('data-signaldeck-hidden')==='true'){node.style.display='none';node.setAttribute('data-signaldeck-hidden','true');node=node.nextElementSibling;continue;}break;}}}}"
                + "function hideNativeImageExpander(){var nodes=document.body?document.body.querySelectorAll('*'):[];for(var i=0;i<nodes.length;i++){var el=nodes[i];if(el.id==='signaldeck-receiver-handle'){continue;}var r=el.getBoundingClientRect();if(!r||r.width<=0||r.height<=0){continue;}var text=(ownText(el)||'').trim().toLowerCase();var center=Math.abs((r.left+r.right)/2-window.innerWidth/2);if(center<82&&r.width>=22&&r.width<=124&&r.height>=12&&r.height<=62&&r.top>76&&r.top<window.innerHeight*.48&&text.length===0){el.style.display='none';el.style.pointerEvents='none';el.setAttribute('data-signaldeck-hidden','true');}if((text==='antena'||text==='antenna'||text.indexOf('autor:')===0||text.indexOf('author:')===0)&&r.top>70&&r.top<window.innerHeight*.48){el.style.display='none';el.setAttribute('data-signaldeck-hidden','true');}}}"
                + "hideForeignPanels();hideReceiverSections();hideNativeImageExpander();"
                + "new MutationObserver(function(){hideForeignPanels();hideReceiverSections();hideNativeImageExpander();}).observe(document.body,{childList:true,subtree:true});"
                + "})();";
    }

    private String signalDeckSkinScript() {
        return "(function(){"
                + "if(window.__signalDeckSkinInstalled){return;}"
                + "window.__signalDeckSkinInstalled=true;"
                + "document.documentElement.classList.add('signaldeck-skin');"
                + "var css='"
                + ".signaldeck-skin body{background:#061014!important;}"
                + ".signaldeck-skin .webrx-top-container{background:linear-gradient(180deg,rgba(4,14,18,.92),rgba(4,14,18,.42))!important;box-shadow:0 8px 24px rgba(0,0,0,.22)!important;}"
                + ".signaldeck-skin .webrx-top-container:after{content:\\'swipe down for receiver\\';position:absolute;right:12px;bottom:4px;color:rgba(183,228,206,.7);font:600 9px/11px sans-serif;letter-spacing:.2px;pointer-events:none;}"
                + ".signaldeck-skin .webrx-top-bar{min-height:54px!important;padding:6px 10px 12px!important;align-items:center!important;gap:8px!important;}"
                + ".signaldeck-skin .webrx-top-logo{display:none!important;}"
                + ".signaldeck-skin .webrx-rx-avatar{width:42px!important;height:42px!important;border-radius:12px!important;opacity:.92!important;border:1px solid rgba(49,210,124,.42)!important;box-shadow:0 8px 18px rgba(0,0,0,.25)!important;}"
                + ".signaldeck-skin .webrx-rx-texts{min-width:0!important;max-width:calc(100vw - 76px)!important;}"
                + ".signaldeck-skin .webrx-rx-title{font-size:15px!important;line-height:17px!important;margin:0!important;color:#eafff7!important;white-space:nowrap!important;overflow:hidden!important;text-overflow:ellipsis!important;letter-spacing:.2px!important;}"
                + ".signaldeck-skin .webrx-rx-desc{font-size:11px!important;line-height:13px!important;color:#93bab7!important;white-space:nowrap!important;overflow:hidden!important;text-overflow:ellipsis!important;}"
                + ".signaldeck-skin .openwebrx-main-buttons{display:none!important;}"
                + ".signaldeck-skin #pstt{display:none!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver{position:fixed!important;left:8px!important;right:8px!important;top:118px!important;width:auto!important;max-width:none!important;height:34vh!important;max-height:34vh!important;overflow-y:auto!important;overflow-x:hidden!important;z-index:80!important;border-radius:18px!important;background:rgba(4,28,36,.97)!important;border:1px solid rgba(49,210,124,.5)!important;box-shadow:0 18px 42px rgba(0,0,0,.46)!important;padding:10px 12px 12px!important;backdrop-filter:blur(8px)!important;overscroll-behavior:contain!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver:before{content:\\'Receiver  ·  swipe up to close\\';position:sticky;top:-10px;z-index:2;display:block;margin:-10px -12px 8px;padding:10px 12px 8px;color:#dfffee;background:rgba(4,28,36,.98);border-bottom:1px solid rgba(49,210,124,.26);font:700 13px/18px sans-serif;letter-spacing:.4px;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-panel-line{margin:6px 0!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver select,.signaldeck-skin #openwebrx-panel-receiver input{border-radius:10px!important;background:#071820!important;color:#f2fffb!important;border:1px solid rgba(49,210,124,.25)!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-button{border-radius:11px!important;background:rgba(12,49,60,.9)!important;border:1px solid rgba(49,210,124,.36)!important;color:#fff!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-section-divider{border-radius:10px!important;background:rgba(3,18,24,.7)!important;color:#bfffe5!important;padding:6px 8px!important;margin-top:8px!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-modes .openwebrx-button{min-height:34px!important;font-weight:700!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-hint{transform:translateX(18px)!important;transition:transform .12s ease-out!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-up-hint{transform:translateY(-24px)!important;transition:transform .12s ease-out!important;}"
                + "@media (orientation:landscape){.signaldeck-skin #openwebrx-panel-receiver{top:86px!important;height:42vh!important;max-height:42vh!important;}}"
                + "';"
                + "var style=document.createElement('style');style.id='signaldeck-skin-style';style.textContent=css;document.head.appendChild(style);"
                + "function receiverToggle(){return document.querySelector('[data-toggle-panel=\"openwebrx-panel-receiver\"]');}"
                + "function receiverVisible(panel){if(!panel){return false;}var s=getComputedStyle(panel);return s.display!=='none'&&s.visibility!=='hidden'&&panel.offsetParent!==null;}"
                + "function showReceiver(){var panel=document.getElementById('openwebrx-panel-receiver');if(receiverVisible(panel)){return;}var toggle=receiverToggle();if(toggle){toggle.click();}else if(panel){panel.style.display='block';}}"
                + "function hideReceiver(){var panel=document.getElementById('openwebrx-panel-receiver');if(!receiverVisible(panel)){return;}var toggle=receiverToggle();if(toggle){toggle.click();}else if(panel){panel.style.display='none';}}"
                + "var top=document.querySelector('.webrx-top-container');"
                + "if(top){var tsx=0,tsy=0;top.addEventListener('touchstart',function(e){if(!e.touches||!e.touches.length){return;}tsx=e.touches[0].clientX;tsy=e.touches[0].clientY;},{passive:true});"
                + "top.addEventListener('touchend',function(e){var t=(e.changedTouches&&e.changedTouches.length)?e.changedTouches[0]:null;if(!t){return;}var dx=Math.abs(t.clientX-tsx);var dy=t.clientY-tsy;if(dy>58&&dx<90){showReceiver();}},{passive:true});}"
                + "var receiver=document.getElementById('openwebrx-panel-receiver');"
                + "if(receiver){var sx=0,sy=0;receiver.addEventListener('touchstart',function(e){if(!e.touches||!e.touches.length){return;}sx=e.touches[0].clientX;sy=e.touches[0].clientY;receiver.classList.remove('sd-swipe-hint');},{passive:true});"
                + "receiver.addEventListener('touchmove',function(e){if(!e.touches||!e.touches.length){return;}var dx=e.touches[0].clientX-sx;var dy=e.touches[0].clientY-sy;var ady=Math.abs(dy);receiver.classList.toggle('sd-swipe-hint',dx>28&&ady<48);receiver.classList.toggle('sd-swipe-up-hint',dy<-28&&Math.abs(dx)<80);},{passive:true});"
                + "receiver.addEventListener('touchend',function(e){var touch=(e.changedTouches&&e.changedTouches.length)?e.changedTouches[0]:null;if(!touch){return;}var dx=touch.clientX-sx;var dy=touch.clientY-sy;receiver.classList.remove('sd-swipe-hint');receiver.classList.remove('sd-swipe-up-hint');if((dx>78&&Math.abs(dy)<58)||(dy<-72&&Math.abs(dx)<95)){hideReceiver();}},{passive:true});}"
                + "})();";
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
                        + "var raw=f?((f.innerText||f.textContent||'')+''):'';"
                        + "raw=raw.replace(/\\s+/g,' ').trim();"
                        + "var mhz=raw.match(/(\\d+[,.]\\d+)\\s*M\\s*H\\s*z/i);"
                        + "var khz=raw.match(/(\\d+[,.]\\d+)\\s*k\\s*H\\s*z/i);"
                        + "var hz=raw.match(/(\\d{5,})\\s*H\\s*z/i);"
                        + "var freq=mhz?(mhz[1].replace(',','.')+' MHz'):(khz?(khz[1].replace(',','.')+' kHz'):(hz?((parseFloat(hz[1])/1000000).toFixed(4)+' MHz'):raw));"
                        + "return JSON.stringify({"
                        + "freq:freq,"
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
            frequencyText.setText(cleanFrequency(freq));
        }
        if (tuningKnob != null) {
            tuningKnob.setStepLabel(step);
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
        if (status.length() > 0) {
            brandText.setText("SignalDeck  |  " + currentReceiver.name + "  |  " + status);
        } else {
            brandText.setText("SignalDeck  |  " + currentReceiver.name);
        }
        if (utcText != null) {
            utcText.setText(clock.length() > 0 ? clock : "");
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
                dp(40)
        );
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        return params;
    }

    private String cleanFrequency(String frequency) {
        String value = frequency.replace(',', '.').replaceAll("\\s+", " ").trim();
        java.util.regex.Matcher mhz = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*M\\s*H\\s*z", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (mhz.find()) {
            return mhz.group(1) + " MHz";
        }

        java.util.regex.Matcher khz = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*k\\s*H\\s*z", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (khz.find()) {
            return khz.group(1) + " kHz";
        }

        java.util.regex.Matcher hz = java.util.regex.Pattern
                .compile("(\\d{5,})\\s*H\\s*z", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(value);
        if (hz.find()) {
            try {
                return String.format(Locale.US, "%.4f MHz", Double.parseDouble(hz.group(1)) / 1000000.0);
            } catch (NumberFormatException ignored) {
            }
        }

        return value.length() > 16 ? value.substring(0, 16).trim() : value;
    }

    private FrameLayout.LayoutParams bottomPanelParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM;
        params.setMargins(0, 0, 0, 0);
        return params;
    }

    private FrameLayout.LayoutParams collapsedPanelParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.BOTTOM;
        params.setMargins(0, 0, 0, 0);
        return params;
    }

    private FrameLayout.LayoutParams nativeArrowBlockerParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(108), dp(46));
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, dp(82), 0, 0);
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
        drawable.setCornerRadius(dp(4));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void updateDeckMeta(String value) {
        if (brandText != null) {
            brandText.setText("SignalDeck  |  " + currentReceiver.name + "  |  " + value);
        }
        if (utcText != null) {
            utcText.setText("");
        }
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

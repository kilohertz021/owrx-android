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

    private WebView webView;
    private TextView brandText;
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

        bar.addView(textStack, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f));

        Button list = createButton("Receivers");
        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleReceiverDrawer();
            }
        });
        bar.addView(list, new LinearLayout.LayoutParams(dp(96), dp(38)));

        Button toggle = createButton("Min");
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDeckExpanded(false);
            }
        });
        bar.addView(toggle, new LinearLayout.LayoutParams(dp(58), dp(38)));
        return bar;
    }

    private LinearLayout createControlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(panelBackground(0xEA081116, dp(14), 0x6631D27C));
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
        panel.setBackground(panelBackground(0xEA081116, dp(14), 0x6631D27C));
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

        Button receivers = createButton("Receivers");
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

    private LinearLayout sideColumn(Button first, Button second) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER);
        column.addView(first, columnButtonParams());
        column.addView(second, columnButtonParams());
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

        webView.evaluateJavascript(signalDeckSkinScript(), null);
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
                + ".signaldeck-skin #openwebrx-panel-receiver{border-radius:20px!important;background:rgba(4,28,36,.96)!important;border:1px solid rgba(49,210,124,.48)!important;box-shadow:0 18px 42px rgba(0,0,0,.46)!important;padding:12px!important;backdrop-filter:blur(8px)!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver:before{content:\\'Receiver  ·  swipe up to close\\';display:block;margin:0 0 8px;color:#dfffee;font:700 13px/18px sans-serif;letter-spacing:.4px;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-panel-line{margin:6px 0!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver select,.signaldeck-skin #openwebrx-panel-receiver input{border-radius:10px!important;background:#071820!important;color:#f2fffb!important;border:1px solid rgba(49,210,124,.25)!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-button{border-radius:11px!important;background:rgba(12,49,60,.9)!important;border:1px solid rgba(49,210,124,.36)!important;color:#fff!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-section-divider{border-radius:10px!important;background:rgba(3,18,24,.7)!important;color:#bfffe5!important;padding:6px 8px!important;margin-top:8px!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-modes .openwebrx-button{min-height:34px!important;font-weight:700!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-hint{transform:translateX(18px)!important;transition:transform .12s ease-out!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-up-hint{transform:translateY(-18px)!important;transition:transform .12s ease-out!important;}"
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
        if (clock.length() > 0) {
            if (status.length() > 0) {
                status.append("  ");
            }
            status.append(clock);
        }
        if (status.length() > 0) {
            brandText.setText("SignalDeck  |  " + currentReceiver.name + "  |  " + status);
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
                dp(56)
        );
        params.setMargins(dp(3), dp(6), dp(3), dp(6));
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
        params.setMargins(dp(8), 0, dp(8), dp(8));
        return params;
    }

    private FrameLayout.LayoutParams collapsedPanelParams() {
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

    private void updateDeckMeta(String value) {
        if (brandText != null) {
            brandText.setText("SignalDeck  |  " + currentReceiver.name + "  |  " + value);
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

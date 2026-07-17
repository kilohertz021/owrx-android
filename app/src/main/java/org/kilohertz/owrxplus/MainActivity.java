package org.kilohertz.owrxplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
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
import android.view.WindowInsets;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
    private FrameLayout rootLayout;
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
    private int emptyStatusTicks;
    private int statusLogTicks;
    private int safeTopInset;
    private int safeBottomInset;
    private final StringBuilder debugLog = new StringBuilder();
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

        webView = createConfiguredWebView();

        setContentView(createLayout());

        if (savedInstanceState == null) {
            loadReceiverUrl(currentReceiver.url);
        } else {
            webView.restoreState(savedInstanceState);
        }

        requestNotificationsIfNeeded();
        startKeepAliveService();
        seedFallbackReceivers();
        refreshReceivers();
    }

    private WebView createConfiguredWebView() {
        WebView view = new WebView(this);
        view.setBackgroundColor(Color.BLACK);
        configureWebView(view);
        return view;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        view.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                logDebug("page started " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if ("about:blank".equals(url)) {
                    return;
                }
                logDebug("page finished " + url);
                injectSignalDeckSkin();
                logWebViewSnapshot("page-finished");
                startStatusPolling();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && request != null && error != null) {
                    logDebug("web error main=" + request.isForMainFrame()
                            + " code=" + error.getErrorCode()
                            + " desc=" + error.getDescription()
                            + " url=" + request.getUrl());
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request != null && errorResponse != null) {
                    logDebug("http error main=" + request.isForMainFrame()
                            + " status=" + errorResponse.getStatusCode()
                            + " url=" + request.getUrl());
                }
            }
        });
        view.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                if (consoleMessage != null) {
                    logDebug("console " + consoleMessage.messageLevel()
                            + " " + consoleMessage.sourceId()
                            + ":" + consoleMessage.lineNumber()
                            + " " + consoleMessage.message());
                }
                return super.onConsoleMessage(consoleMessage);
            }
        });
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
        rootLayout = root;
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
        installSafeAreaHandling(root);
        return root;
    }

    private void installSafeAreaHandling(FrameLayout root) {
        safeTopInset = dp(14);
        safeBottomInset = dp(10);
        applySafeInsets();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    int top = Math.max(dp(14), insets.getSystemWindowInsetTop());
                    int bottom = Math.max(dp(10), insets.getSystemWindowInsetBottom());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && insets.getDisplayCutout() != null) {
                        top = Math.max(top, insets.getDisplayCutout().getSafeInsetTop() + dp(8));
                        bottom = Math.max(bottom, insets.getDisplayCutout().getSafeInsetBottom() + dp(8));
                    }
                    safeTopInset = top;
                    safeBottomInset = bottom;
                    applySafeInsets();
                    return insets;
                }
            });
            root.requestApplyInsets();
        }
    }

    private void applySafeInsets() {
        if (webView != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            );
            params.setMargins(0, safeTopInset, 0, 0);
            webView.setLayoutParams(params);
        }
        if (controlPanel != null) {
            controlPanel.setLayoutParams(bottomPanelParams());
        }
        if (collapsedPanel != null) {
            collapsedPanel.setLayoutParams(collapsedPanelParams());
        }
        if (receiverDrawer != null) {
            receiverDrawer.setLayoutParams(drawerParams());
        }
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
        brandText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                copyDebugLogToClipboard();
                return true;
            }
        });
        textStack.addView(brandText);

        utcText = new TextView(this);
        utcText.setText("");
        utcText.setTextColor(COLOR_TEXT_MUTED);
        utcText.setTextSize(10);
        utcText.setSingleLine(true);
        textStack.addView(utcText);

        bar.addView(textStack, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return bar;
    }

    private LinearLayout createControlPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
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
                    if (event.getRawY() - deckTouchStartY > dp(36)) {
                        setDeckExpanded(false);
                        return true;
                    }
                }
                return false;
            }
        });

        panel.addView(createDeckHeader());

        RelativeLayout deck = new RelativeLayout(this);

        LinearLayout buttons = sideColumn(
                receiverPanelButton(),
                receiverListButton(),
                control("Zoom +", "if (typeof zoomInOneStep==='function') zoomInOneStep();"),
                control("Zoom -", "if (typeof zoomOutOneStep==='function') zoomOutOneStep();"),
                mobileRangeControl("SQ"),
                mobileRangeControl("NR")
        );
        buttons.setId(View.generateViewId());

        RelativeLayout leftArea = new RelativeLayout(this);
        RelativeLayout.LayoutParams leftParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        leftParams.addRule(RelativeLayout.LEFT_OF, buttons.getId());
        leftParams.setMargins(0, 0, dp(8), 0);
        deck.addView(leftArea, leftParams);

        frequencyText = new TextView(this);
        frequencyText.setText("Connecting");
        frequencyText.setTextColor(Color.WHITE);
        frequencyText.setTextSize(21);
        frequencyText.setTypeface(Typeface.DEFAULT_BOLD);
        frequencyText.setSingleLine(true);
        frequencyText.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams frequencyParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                dp(34)
        );
        frequencyParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        frequencyParams.setMargins(dp(6), 0, dp(6), 0);
        leftArea.addView(frequencyText, frequencyParams);

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
        RelativeLayout.LayoutParams knobParams = new RelativeLayout.LayoutParams(dp(152), dp(152));
        knobParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
        knobParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        knobParams.setMargins(0, dp(18), 0, 0);
        leftArea.addView(tuningKnob, knobParams);

        RelativeLayout.LayoutParams buttonParams = new RelativeLayout.LayoutParams(dp(136), RelativeLayout.LayoutParams.WRAP_CONTENT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        buttonParams.setMargins(0, 0, 0, 0);
        deck.addView(buttons, buttonParams);

        panel.addView(deck, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(198)
        ));

        return panel;
    }

    private LinearLayout createCollapsedPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(10), 0, dp(10), 0);
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
                    setDeckExpanded(true);
                    return true;
                }
                return false;
            }
        });

        TextView title = new TextView(this);
        title.setText("^  Deck");
        title.setTextColor(Color.WHITE);
        title.setTextSize(12);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setSingleLine(true);
        title.setGravity(Gravity.CENTER);
        title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDeckExpanded(true);
            }
        });
        panel.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        return panel;
    }

    private LinearLayout sideColumn(View... controls) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.TOP);
        for (View control : controls) {
            column.addView(control, columnControlParams(control instanceof Button));
        }
        return column;
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
        addPinnedReceivers();
        addReceivers(receiverCatalog.fallbackReceivers());
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
                addPinnedReceivers();
                addReceivers(loadedReceivers);
                renderReceivers();
                updateDeckMeta(receivers.size() + " receivers");
            }

            @Override
            public void onError(List<ReceiverInfo> fallback, String message) {
                receivers.clear();
                addPinnedReceivers();
                addReceivers(fallback);
                renderReceivers();
                updateDeckMeta("Catalog fallback");
            }
        });
    }

    private void addPinnedReceivers() {
        addReceiver(homeReceiver());
        if (!isHomeReceiver(currentReceiver)) {
            addReceiver(currentReceiver);
        }
    }

    private void addReceivers(List<ReceiverInfo> newReceivers) {
        for (ReceiverInfo receiver : newReceivers) {
            addReceiver(receiver);
        }
    }

    private void addReceiver(ReceiverInfo receiver) {
        if (receiver != null && !containsUrl(receivers, receiver.url)) {
            receivers.add(receiver);
        }
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

        ReceiverInfo home = homeReceiver();
        receiverListView.addView(receiverRow(home));
        shown++;

        for (final ReceiverInfo receiver : receivers) {
            if (isHomeReceiver(receiver)) {
                continue;
            }
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

        if (shown == 1 && filter.length() > 0) {
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

    private ReceiverInfo homeReceiver() {
        return new ReceiverInfo(
                DEFAULT_RECEIVER_NAME,
                DEFAULT_RECEIVER_URL,
                "JN95wg",
                "RS",
                "Novi Sad",
                45.267,
                19.833
        );
    }

    private boolean isHomeReceiver(ReceiverInfo receiver) {
        return receiver != null && DEFAULT_RECEIVER_URL.equalsIgnoreCase(receiver.url);
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
        logDebug("select receiver name=" + receiver.name + " url=" + receiver.url);
        brandText.setText("SignalDeck  |  " + receiver.name);
        frequencyText.setText("Loading");
        emptyStatusTicks = 0;
        statusLogTicks = 0;
        if (utcText != null) {
            utcText.setText("");
        }
        receiverDrawer.setVisibility(View.GONE);
        loadReceiverUrl(receiver.url);
    }

    private void loadReceiverUrl(final String url) {
        uiHandler.removeCallbacks(statusPoller);
        logDebug("load receiver url=" + url);
        final WebView oldWebView = webView;
        WebView freshWebView = createConfiguredWebView();
        webView = freshWebView;

        if (rootLayout != null) {
            if (oldWebView != null) {
                rootLayout.removeView(oldWebView);
            }
            rootLayout.addView(freshWebView, 0, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            applySafeInsets();
        }

        if (oldWebView != null) {
            oldWebView.stopLoading();
            oldWebView.loadUrl("about:blank");
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    oldWebView.destroy();
                }
            }, 250);
        }

        freshWebView.loadUrl(url);
        logWebViewSnapshot("after-loadUrl");
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

    private LinearLayout mobileRangeControl(final String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(5), 0, dp(3), 0);
        row.setBackground(panelBackground(COLOR_BUTTON, dp(6), COLOR_BORDER));

        TextView title = new TextView(this);
        title.setText(label);
        title.setTextColor(Color.WHITE);
        title.setTextSize(11);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(dp(24), LinearLayout.LayoutParams.MATCH_PARENT));

        SeekBar seekBar = new SeekBar(this);
        seekBar.setMax(100);
        seekBar.setProgress(50);
        seekBar.setPadding(0, 0, 0, 0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setReceiverRange(label, progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                setReceiverRange(label, seekBar.getProgress());
            }
        });
        row.addView(seekBar, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return row;
    }

    private void setReceiverRange(String label, int percent) {
        runReceiverScript(rangeControlScript(label, percent));
    }

    private String clickControlScript(String label) {
        return "var wanted='" + label.toLowerCase(Locale.US) + "';"
                + "function own(el){var out='';for(var i=0;i<el.childNodes.length;i++){if(el.childNodes[i].nodeType===3){out+=el.childNodes[i].nodeValue+' ';}}return out.replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "var nodes=document.querySelectorAll('button,.openwebrx-button,[role=button],label,span,div');"
                + "var clicked=false;"
                + "for(var i=0;i<nodes.length;i++){var t=own(nodes[i]);if(t===wanted){nodes[i].click();clicked=true;break;}}"
                + "console.log('SignalDeck control '+wanted+' clicked='+clicked);";
    }

    private String rangeControlScript(String label, int percent) {
        String wanted = label.toLowerCase(Locale.US);
        int safePercent = Math.max(0, Math.min(100, percent));
        return "var wanted='" + wanted + "';var pct=" + safePercent + ";"
                + "function own(el){var out='';for(var i=0;i<el.childNodes.length;i++){if(el.childNodes[i].nodeType===3){out+=el.childNodes[i].nodeValue+' ';}}return out.replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "function norm(el){return ((own(el)||el.textContent||'')+'').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "function hasToken(text){return text.split(/\\s+/).indexOf(wanted)>=0;}"
                + "function ranges(root){return root?root.querySelectorAll('input[type=range],input[type=number],input:not([type]),[role=slider]'):[];}"
                + "function setHashParam(key,value){var raw=(location.hash||'').replace(/^#/,'');var parts=raw?raw.split(','):[];var found=false;for(var i=0;i<parts.length;i++){if(parts[i].split('=')[0]===key){parts[i]=key+'='+value;found=true;break;}}if(!found){parts.push(key+'='+value);}location.hash=parts.join(',');}"
                + "if(wanted==='sq'){var sql=Math.round(-120+(pct*120/100));setHashParam('sql',sql);try{window.dispatchEvent(new HashChangeEvent('hashchange'));}catch(e){window.dispatchEvent(new Event('hashchange'));}console.log('SignalDeck range sq pct='+pct+' sql='+sql);}"
                + "var roots=[];"
                + "if(wanted==='nr'){roots.push(document.getElementById('openwebrx-panel-nr'));roots.push(document.querySelector('[id*=\"nr\" i]'));}"
                + "if(wanted==='sq'){roots.push(document.getElementById('openwebrx-panel-receiver'));roots.push(document.getElementById('openwebrx-panel-volume'));}"
                + "roots.push(document.getElementById('openwebrx-panel-receiver'));roots.push(document);"
                + "var target=null;var base=null;"
                + "for(var r=0;r<roots.length&&!target;r++){var panel=roots[r];if(!panel){continue;}var direct=ranges(panel);if(wanted==='nr'&&panel.id==='openwebrx-panel-nr'&&direct.length){target=direct[0];break;}var nodes=panel.querySelectorAll('.openwebrx-panel-line,button,.openwebrx-button,label,span,div');"
                + "for(var i=0;i<nodes.length&&!target;i++){var t=own(nodes[i]);if(!t){t=norm(nodes[i]);}if(hasToken(t)){base=nodes[i].closest('.openwebrx-panel-line')||nodes[i].parentElement||nodes[i];var rs=ranges(base);if(rs.length){target=rs[0];break;}var n=base.nextElementSibling;var guard=0;while(n&&guard++<4&&!target){var nt=norm(n);if(nt.indexOf('modes')>=0||nt.indexOf('settings')>=0||nt.indexOf('display')>=0){break;}rs=ranges(n);if(rs.length){target=rs[0];break;}n=n.nextElementSibling;}}}"
                + "}"
                + "if(target&&target.tagName&&target.tagName.toLowerCase()==='input'){var min=parseFloat(target.min);var max=parseFloat(target.max);if(isNaN(min)){min=0;}if(isNaN(max)||max===min){max=100;}var value=min+(max-min)*pct/100;target.value=value;target.dispatchEvent(new Event('input',{bubbles:true}));target.dispatchEvent(new Event('change',{bubbles:true}));console.log('SignalDeck range '+wanted+' pct='+pct+' value='+value+' min='+min+' max='+max);}"
                + "else if(target){target.setAttribute('aria-valuenow',pct);target.dispatchEvent(new Event('input',{bubbles:true}));target.dispatchEvent(new Event('change',{bubbles:true}));console.log('SignalDeck range '+wanted+' aria pct='+pct);}"
                + "else if(wanted!=='sq'){console.log('SignalDeck range '+wanted+' target not found');}";
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
        button.setTextSize(11);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(2), 0, dp(2), 0);
        button.setBackground(panelBackground(COLOR_BUTTON, dp(6), COLOR_BORDER));
        return button;
    }

    private void setDeckExpanded(boolean expanded) {
        deckExpanded = expanded;
        controlPanel.setVisibility(deckExpanded ? View.VISIBLE : View.GONE);
        collapsedPanel.setVisibility(deckExpanded ? View.GONE : View.VISIBLE);
    }

    private void tune(int direction) {
        runReceiverScript(tuneScript(direction));
    }

    private String tuneScript(int direction) {
        int safeDirection = direction < 0 ? -1 : 1;
        return "var dir=" + safeDirection + ";"
                + "function hashParts(){return (location.hash||'').replace(/^#/,'').split(',').filter(Boolean);}"
                + "function getHashParam(key){var p=hashParts();for(var i=0;i<p.length;i++){var x=p[i].split('=');if(x[0]===key){return x.slice(1).join('=');}}return null;}"
                + "function setHashParam(key,value){var p=hashParts();var found=false;for(var i=0;i<p.length;i++){if(p[i].split('=')[0]===key){p[i]=key+'='+value;found=true;break;}}if(!found){p.push(key+'='+value);}location.hash=p.join(',');}"
                + "function textFreqHz(){var el=document.querySelector('.webrx-actual-freq');var t=((el&&el.textContent)||'').replace(',', '.').trim().toLowerCase();var m=t.match(/([0-9]+(?:\\.[0-9]+)?)\\s*(ghz|mhz|khz|hz)/);if(!m){return NaN;}var n=parseFloat(m[1]);var u=m[2];var mult=u==='ghz'?1000000000:(u==='mhz'?1000000:(u==='khz'?1000:1));return Math.round(n*mult);}"
                + "var freq=parseInt(getHashParam('freq'),10);if(isNaN(freq)||freq<=0){freq=textFreqHz();}"
                + "var s=document.getElementById('openwebrx-tuning-step-listbox');var step=s?parseFloat(s.value):NaN;if(isNaN(step)||step<=0){step=1000;}"
                + "if(!isNaN(freq)&&freq>0){var next=Math.max(0,Math.round(freq+dir*step));setHashParam('freq',next);try{window.dispatchEvent(new HashChangeEvent('hashchange'));}catch(e){window.dispatchEvent(new Event('hashchange'));}console.log('SignalDeck tune hash freq='+next+' step='+step+' dir='+dir);}"
                + "else if(typeof tuneBySteps==='function'){tuneBySteps(dir);console.log('SignalDeck tune fallback tuneBySteps dir='+dir);}"
                + "else{console.log('SignalDeck tune failed: no freq and no tuneBySteps');}";
    }

    private void cycleTuningStep() {
        runReceiverScript(stepScript(1));
    }

    private void toggleReceiverPanel() {
        runReceiverScript(
                "var root=document.documentElement;"
                        + "var panel=document.getElementById('openwebrx-panel-receiver');"
                        + "if(panel){panel.removeAttribute('hidden');panel.style.display='block';panel.style.visibility='visible';}"
                        + "if(root.classList.contains('sd-receiver-open')){root.classList.remove('sd-receiver-open');}"
                        + "else{root.classList.add('sd-receiver-open');}"
                        + "var panels=document.querySelectorAll('[id^=\"openwebrx-panel-\"]');"
                        + "for(var i=0;i<panels.length;i++){var id=(panels[i].id||'').toLowerCase();if(id!=='openwebrx-panel-receiver'&&/(log|status|map|files|settings|help)/.test(id)){panels[i].style.display='none';}}"
        );
    }

    private String stepScript(int direction) {
        String indexChange = direction < 0
                ? "Math.max(0,s.selectedIndex-1)"
                : "(s.selectedIndex+1)%s.options.length";
        return "window.__signalDeckStepTouched=true;"
                + "var s=document.getElementById('openwebrx-tuning-step-listbox');"
                + "if(s&&s.options.length){s.selectedIndex=" + indexChange + ";"
                + "s.dispatchEvent(new Event('change'));}";
    }

    private void runReceiverScript(String script) {
        if (webView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        webView.evaluateJavascript("(function(){" + script + "})()", null);
    }

    private void logDebug(String message) {
        String line = String.format(Locale.US, "%1$tH:%1$tM:%1$tS %2$s", new java.util.Date(), message);
        debugLog.append(line).append('\n');
        if (debugLog.length() > 32000) {
            debugLog.delete(0, debugLog.length() - 24000);
        }
        android.util.Log.d("SignalDeck", line);
    }

    private void copyDebugLogToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) {
            Toast.makeText(this, "Clipboard unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        String text = debugLog.length() == 0 ? "SignalDeck debug log is empty" : debugLog.toString();
        clipboard.setPrimaryClip(ClipData.newPlainText("SignalDeck debug log", text));
        Toast.makeText(this, "SignalDeck debug log copied", Toast.LENGTH_LONG).show();
    }

    private void logWebViewSnapshot(final String reason) {
        if (webView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }
        webView.evaluateJavascript(
                "(function(){"
                        + "function d(el){if(!el){return null;}var r=el.getBoundingClientRect();return {tag:el.tagName,id:el.id||'',cls:(el.className||'')+'',txt:((el.innerText||el.textContent||'')+'').replace(/\\s+/g,' ').trim().slice(0,80),x:Math.round(r.left),y:Math.round(r.top),w:Math.round(r.width),h:Math.round(r.height),bg:getComputedStyle(el).backgroundColor,disp:getComputedStyle(el).display,vis:getComputedStyle(el).visibility};}"
                        + "var cx=Math.floor(window.innerWidth/2),cy=Math.floor(window.innerHeight/2);"
                        + "var center=document.elementFromPoint(cx,cy);"
                        + "var top=document.elementFromPoint(cx,Math.min(window.innerHeight-1,90));"
                        + "var wf=document.querySelector('canvas,#webrx-waterfall,.webrx-waterfall,.waterfall');"
                        + "var panels=[];var ps=document.querySelectorAll('[id^=\"openwebrx-panel-\"]');for(var i=0;i<ps.length;i++){var s=getComputedStyle(ps[i]);if(s.display!=='none'&&s.visibility!=='hidden'){panels.push(ps[i].id+':'+Math.round(ps[i].getBoundingClientRect().width)+'x'+Math.round(ps[i].getBoundingClientRect().height));}}"
                        + "return JSON.stringify({url:location.href,title:document.title,ready:document.readyState,bodyBg:getComputedStyle(document.body).backgroundColor,htmlBg:getComputedStyle(document.documentElement).backgroundColor,viewport:window.innerWidth+'x'+window.innerHeight,center:d(center),top:d(top),waterfall:d(wf),panels:panels});"
                        + "})()",
                value -> logDebug("snapshot " + reason + " " + unquoteJsString(value))
        );
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
                + ".signaldeck-skin [data-signaldeck-native-meter=true]{display:none!important;pointer-events:none!important;visibility:hidden!important;}"
                + ".signaldeck-skin #openwebrx-panel-log,.signaldeck-skin #openwebrx-panel-status,.signaldeck-skin #openwebrx-panel-map,.signaldeck-skin #openwebrx-panel-files,.signaldeck-skin #openwebrx-panel-settings,.signaldeck-skin #openwebrx-panel-help{display:none!important;pointer-events:none!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle{display:none!important;pointer-events:none!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle:before{content:\\'\\';position:absolute!important;left:50%!important;top:4px!important;margin-left:-8px!important;width:0!important;height:0!important;border-left:8px solid transparent!important;border-right:8px solid transparent!important;border-top:9px solid #d7f6ff!important;filter:drop-shadow(0 0 7px rgba(159,234,255,.78))!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle:after{content:\\'RECEIVER\\';position:absolute!important;left:0!important;right:0!important;bottom:1px!important;text-align:center!important;color:rgba(215,246,255,.72)!important;font:700 7px/8px sans-serif!important;letter-spacing:.8px!important;}"
                + ".signaldeck-skin #signaldeck-receiver-handle.sd-pull{transform:translateY(9px)!important;transition:transform .1s ease-out!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver{position:fixed!important;left:auto!important;right:0!important;top:96px!important;width:min(88vw,400px)!important;max-width:88vw!important;height:54vh!important;max-height:54vh!important;overflow-y:auto!important;overflow-x:hidden!important;z-index:80!important;border-radius:4px 0 0 4px!important;background:rgba(7,18,27,.98)!important;border:1px solid rgba(159,234,255,.5)!important;border-right:0!important;box-shadow:0 0 30px rgba(120,214,255,.16),-18px 18px 42px rgba(0,0,0,.5)!important;padding:10px 12px 12px!important;backdrop-filter:blur(8px)!important;overscroll-behavior:contain!important;transform:translateX(108%)!important;opacity:0!important;pointer-events:none!important;transition:transform .18s ease-out,opacity .18s ease-out!important;}"
                + ".signaldeck-skin.sd-receiver-open #openwebrx-panel-receiver{transform:translateX(0)!important;opacity:1!important;pointer-events:auto!important;}"
                + ".signaldeck-skin #signaldeck-receiver-tab{position:fixed!important;right:0!important;top:36vh!important;width:24px!important;height:104px!important;z-index:79!important;display:flex!important;align-items:center!important;justify-content:center!important;border-radius:4px 0 0 4px!important;border:1px solid rgba(159,234,255,.55)!important;border-right:0!important;background:rgba(7,18,27,.86)!important;color:#d7f6ff!important;box-shadow:0 0 18px rgba(120,214,255,.2),-8px 8px 24px rgba(0,0,0,.32)!important;font:800 9px/10px sans-serif!important;letter-spacing:.8px!important;writing-mode:vertical-rl!important;text-orientation:mixed!important;text-transform:uppercase!important;opacity:.88!important;pointer-events:auto!important;transition:opacity .16s ease-out,transform .16s ease-out!important;}"
                + ".signaldeck-skin #signaldeck-receiver-tab:before{content:\\'‹\\';font:900 18px/18px sans-serif!important;margin-bottom:6px!important;color:#9feaff!important;}"
                + ".signaldeck-skin.sd-receiver-open #signaldeck-receiver-tab{opacity:.24!important;transform:translateX(18px)!important;pointer-events:none!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver:before{content:\\'Receiver\\';position:sticky;top:-10px;z-index:2;display:block;margin:-10px -12px 8px;padding:10px 12px 8px;color:#edf8ff;background:rgba(7,18,27,.99);border-bottom:1px solid rgba(159,234,255,.24);font:700 13px/18px sans-serif;letter-spacing:.4px;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-panel-line{margin:6px 0!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver select,.signaldeck-skin #openwebrx-panel-receiver input{border-radius:4px!important;background:#091824!important;color:#edf8ff!important;border:1px solid rgba(159,234,255,.28)!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-button{border-radius:4px!important;background:rgba(16,40,58,.92)!important;border:1px solid rgba(159,234,255,.38)!important;color:#fff!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-section-divider{border-radius:3px!important;background:rgba(5,13,21,.72)!important;color:#d7f6ff!important;padding:6px 8px!important;margin-top:8px!important;border-color:rgba(159,234,255,.32)!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver .openwebrx-modes .openwebrx-button{min-height:34px!important;font-weight:700!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver [data-signaldeck-dig-row=true]{display:flex!important;align-items:center!important;gap:7px!important;margin-top:7px!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver [data-signaldeck-dig-row=true] .openwebrx-button{width:56px!important;min-width:56px!important;min-height:32px!important;height:32px!important;padding:0!important;line-height:30px!important;font-size:13px!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver [data-signaldeck-dig-row=true] select{flex:1!important;min-width:0!important;height:32px!important;}"
                + ".signaldeck-skin #signaldeck-waterfall-controls{display:none;margin:9px 0 0!important;padding:7px 8px!important;border:1px solid rgba(159,234,255,.18)!important;background:rgba(5,13,21,.44)!important;border-radius:4px!important;box-sizing:border-box!important;}"
                + ".signaldeck-skin #signaldeck-waterfall-controls .sd-wf-head{display:flex!important;align-items:center!important;justify-content:space-between!important;margin-bottom:5px!important;color:#edf8ff!important;font:700 11px/14px sans-serif!important;letter-spacing:.2px!important;}"
                + ".signaldeck-skin #signaldeck-waterfall-controls .sd-wf-actions{display:flex!important;gap:5px!important;}"
                + ".signaldeck-skin #signaldeck-waterfall-controls button{height:22px!important;min-height:22px!important;padding:0 8px!important;border-radius:4px!important;border:1px solid rgba(159,234,255,.38)!important;background:rgba(16,40,58,.92)!important;color:#fff!important;font:700 10px/20px sans-serif!important;}"
                + ".signaldeck-skin #signaldeck-waterfall-controls .sd-wf-row{display:grid!important;grid-template-columns:38px 1fr 40px!important;align-items:center!important;gap:7px!important;margin-top:4px!important;color:#b8ccd8!important;font:700 10px/18px sans-serif!important;}"
                + ".signaldeck-skin #signaldeck-waterfall-controls input[type=range]{width:100%!important;height:24px!important;margin:0!important;padding:0!important;background:transparent!important;border:0!important;accent-color:#9feaff!important;}"
                + ".signaldeck-skin #signaldeck-waterfall-controls .sd-wf-value{text-align:right!important;color:#edf8ff!important;font:700 9px/18px monospace!important;}"
                + ".signaldeck-skin [data-signaldeck-decoder-panel=true]{left:6px!important;right:6px!important;width:auto!important;max-width:calc(100vw - 12px)!important;max-height:34vh!important;overflow:auto!important;box-sizing:border-box!important;border-radius:4px!important;background:rgba(0,67,78,.95)!important;border:1px solid rgba(159,234,255,.36)!important;padding:8px!important;}"
                + ".signaldeck-skin [data-signaldeck-decoder-panel=true] table{width:100%!important;max-width:100%!important;table-layout:fixed!important;border-collapse:collapse!important;font:12px/15px sans-serif!important;}"
                + ".signaldeck-skin [data-signaldeck-decoder-panel=true] th,.signaldeck-skin [data-signaldeck-decoder-panel=true] td{padding:2px 4px!important;white-space:normal!important;overflow-wrap:anywhere!important;word-break:break-word!important;max-width:50vw!important;}"
                + ".signaldeck-skin [data-signaldeck-decoder-panel=true] *{box-sizing:border-box!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver [data-signaldeck-hidden=true],.signaldeck-skin #openwebrx-panel-receiver [id*=settings],.signaldeck-skin #openwebrx-panel-receiver [id*=display],.signaldeck-skin #openwebrx-panel-receiver [class*=settings],.signaldeck-skin #openwebrx-panel-receiver [class*=display]{display:none!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-hint{transform:translateX(18px)!important;transition:transform .12s ease-out!important;}"
                + ".signaldeck-skin #openwebrx-panel-receiver.sd-swipe-up-hint{transform:translateY(-24px)!important;transition:transform .12s ease-out!important;}"
                + "@media (orientation:landscape){.signaldeck-skin #signaldeck-receiver-handle{top:78px!important;}.signaldeck-skin #openwebrx-panel-receiver{top:78px!important;width:min(58vw,500px)!important;height:64vh!important;max-height:64vh!important;}}"
                + "';"
                + "var style=document.createElement('style');style.id='signaldeck-skin-style';style.textContent=css;document.head.appendChild(style);"
                + "function receiverToggle(){return document.querySelector('[data-toggle-panel=\"openwebrx-panel-receiver\"]');}"
                + "function receiverVisible(panel){return !!(panel&&document.documentElement.classList.contains('sd-receiver-open'));}"
                + "function showReceiver(){var panel=document.getElementById('openwebrx-panel-receiver');if(panel){panel.removeAttribute('hidden');panel.style.display='block';panel.style.visibility='visible';document.documentElement.classList.add('sd-receiver-open');hideReceiverSections();ensureWaterfallControls();return;}var toggle=receiverToggle();if(toggle){toggle.click();}}"
                + "function hideReceiver(){document.documentElement.classList.remove('sd-receiver-open');}"
                + "function ensureDefaultStep(){if(window.__signalDeckStepTouched){return;}var s=document.getElementById('openwebrx-tuning-step-listbox');if(!s||!s.options||!s.options.length){return;}var found=-1;for(var i=0;i<s.options.length;i++){var o=s.options[i];var raw=((o.value||'')+' '+(o.textContent||'')).toLowerCase().replace(/\\s+/g,'');var val=parseFloat(o.value);if(val===1000||raw.indexOf('1khz')>=0||raw.indexOf('1000hz')>=0){found=i;break;}}if(found>=0&&s.selectedIndex!==found){s.selectedIndex=found;s.dispatchEvent(new Event('change',{bubbles:true}));console.log('SignalDeck default step 1kHz');}}"
                + "function hideForeignPanels(){var panels=document.querySelectorAll('[id^=\"openwebrx-panel-\"]');for(var i=0;i<panels.length;i++){var id=(panels[i].id||'').toLowerCase();if(id!=='openwebrx-panel-receiver'&&/(log|status|map|files|settings|help)/.test(id)){panels[i].style.display='none';panels[i].style.pointerEvents='none';}}}"
                + "function ownText(el){var out='';for(var i=0;i<el.childNodes.length;i++){if(el.childNodes[i].nodeType===3){out+=el.childNodes[i].nodeValue+' ';}}return out.replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "function normText(el){return ((ownText(el)||el.textContent||'')+'').replace(/[.:>\\-]/g,' ').replace(/\\s+/g,' ').trim().toLowerCase();}"
                + "function tidyDigRow(){var select=document.querySelector('#openwebrx-panel-receiver .openwebrx-secondary-demod-listbox,#openwebrx-panel-receiver select[class*=secondary]');if(!select){return null;}var row=select.closest('.openwebrx-panel-line')||select.parentElement;if(row){row.setAttribute('data-signaldeck-dig-row','true');}return row;}"
                + "function rangePercent(input){var min=parseFloat(input.min),max=parseFloat(input.max),value=parseFloat(input.value);if(isNaN(min)){min=0;}if(isNaN(max)||max===min){max=100;}if(isNaN(value)){value=min;}return Math.max(0,Math.min(100,Math.round((value-min)*100/(max-min))));}"
                + "function nativeWaterfallInput(which){var byId=document.getElementById(which==='min'?'openwebrx-waterfall-color-min':'openwebrx-waterfall-color-max');if(byId){return byId;}var inputs=document.querySelectorAll('input[type=range],input:not([type])');var best=null;for(var i=0;i<inputs.length;i++){var el=inputs[i];if(el.closest&&el.closest('#signaldeck-waterfall-controls')){continue;}var key=((el.id||'')+' '+(el.title||'')+' '+(el.className||'')+' '+(el.name||'')).toLowerCase();var isWf=key.indexOf('waterfall')>=0||key.indexOf('wf')>=0;var isMin=key.indexOf('min')>=0||key.indexOf('minimum')>=0||key.indexOf('floor')>=0||key.indexOf('low')>=0;var isMax=key.indexOf('max')>=0||key.indexOf('maximum')>=0||key.indexOf('peak')>=0||key.indexOf('high')>=0;if(isWf&&((which==='min'&&isMin)||(which==='max'&&isMax))){best=el;break;}}return best;}"
                + "function setWaterfallRange(which,pct){var input=nativeWaterfallInput(which);if(!input){console.log('SignalDeck waterfall '+which+' target not found');return;}var min=parseFloat(input.min),max=parseFloat(input.max);if(isNaN(min)){min=-200;}if(isNaN(max)||max===min){max=100;}var value=Math.round(min+(max-min)*Math.max(0,Math.min(100,pct))/100);input.value=value;input.dispatchEvent(new Event('input',{bubbles:true}));input.dispatchEvent(new Event('change',{bubbles:true}));if(window.Waterfall&&typeof Waterfall.updateColors==='function'){Waterfall.updateColors(which==='min'?0:1);}syncWaterfallControls();console.log('SignalDeck waterfall '+which+' pct='+pct+' value='+value);}"
                + "function syncWaterfallControls(){var box=document.getElementById('signaldeck-waterfall-controls');if(!box){return;}['min','max'].forEach(function(which){var native=nativeWaterfallInput(which);var custom=box.querySelector('[data-wf='+which+']');var value=box.querySelector('[data-wf-value='+which+']');if(!native||!custom){return;}custom.value=rangePercent(native);if(value){value.textContent=native.value+' dB';}});}"
                + "function ensureWaterfallControls(){var panel=document.getElementById('openwebrx-panel-receiver');var row=tidyDigRow();var min=nativeWaterfallInput('min'),max=nativeWaterfallInput('max');var freq=((document.querySelector('.webrx-actual-freq')||{}).textContent||'').trim();var ready=panel&&receiverVisible(panel)&&row&&min&&max&&freq&&freq.toLowerCase()!=='0 hz';var box=document.getElementById('signaldeck-waterfall-controls');if(!box){box=document.createElement('div');box.id='signaldeck-waterfall-controls';box.innerHTML='<div class=\"sd-wf-head\"><span>Waterfall levels</span><span class=\"sd-wf-actions\"><button type=\"button\" data-wf-action=\"auto\">Auto</button><button type=\"button\" data-wf-action=\"reset\">Reset</button></span></div><label class=\"sd-wf-row\"><span>Floor</span><input type=\"range\" min=\"0\" max=\"100\" value=\"50\" data-wf=\"min\"><span class=\"sd-wf-value\" data-wf-value=\"min\">--</span></label><label class=\"sd-wf-row\"><span>Peak</span><input type=\"range\" min=\"0\" max=\"100\" value=\"50\" data-wf=\"max\"><span class=\"sd-wf-value\" data-wf-value=\"max\">--</span></label>';box.querySelector('[data-wf=min]').addEventListener('input',function(){setWaterfallRange('min',parseInt(this.value,10));});box.querySelector('[data-wf=max]').addEventListener('input',function(){setWaterfallRange('max',parseInt(this.value,10));});box.querySelector('[data-wf-action=auto]').addEventListener('click',function(){if(window.Waterfall&&typeof Waterfall.setAutoRange==='function'){Waterfall.setAutoRange();setTimeout(syncWaterfallControls,120);}});box.querySelector('[data-wf-action=reset]').addEventListener('click',function(){if(window.Waterfall&&typeof Waterfall.setDefaultRange==='function'){Waterfall.setDefaultRange();setTimeout(syncWaterfallControls,120);}});}if(row&&box.parentElement!==row.parentElement){row.parentElement.insertBefore(box,row.nextSibling);}if(!ready){box.style.display='none';return;}box.style.display='block';syncWaterfallControls();}"
                + "function hideBlockFromHeader(header){header.style.display='none';header.setAttribute('data-signaldeck-hidden','true');var node=header.nextElementSibling;while(node){var t=normText(node);if(t.indexOf('modes')>=0||t.indexOf('controls')>=0||t.indexOf('settings')>=0||t.indexOf('display')>=0){break;}node.style.display='none';node.setAttribute('data-signaldeck-hidden','true');node=node.nextElementSibling;}}"
                + "function hideReceiverSections(){var panel=document.getElementById('openwebrx-panel-receiver');if(!panel){return;}var nodes=panel.querySelectorAll('*');for(var i=0;i<nodes.length;i++){var t=normText(nodes[i]);var own=(ownText(nodes[i])||'').replace(/[.:>\\-]/g,' ').replace(/\\s+/g,' ').trim().toLowerCase();var isHeader=nodes[i].className&&((' '+nodes[i].className+' ').toLowerCase().indexOf('openwebrx-section-divider')>=0);if(isHeader||own==='settings'||own==='display'||own==='controls'){if(t.indexOf('settings')>=0||t.indexOf('display')>=0||t.indexOf('controls')>=0){hideBlockFromHeader(nodes[i]);}}}var rows=panel.querySelectorAll('.openwebrx-panel-line');for(var j=0;j<rows.length;j++){var rt=normText(rows[j]);if(rt.indexOf('sq')>=0||rt.indexOf('nr')>=0||rt.indexOf('volume')>=0||rt.indexOf('audio')>=0||rt.indexOf('1khz')>=0){rows[j].style.display='none';rows[j].setAttribute('data-signaldeck-hidden','true');}}}"
                + "function hideNativeStatusMeters(){var terms=/\\b(audio buffer|audio output|audio stream|network usage|server cpu|clients)\\b/i;var nodes=document.body?document.body.querySelectorAll('*'):[];for(var i=0;i<nodes.length;i++){var el=nodes[i];if(el.closest&&el.closest('#signaldeck-deck,#signaldeck-receiver-modal,#openwebrx-panel-receiver,.openwebrx-message-panel,.openwebrx-meta-panel,#openwebrx-panel-digimodes')){continue;}var text=((el.textContent||'')+'').replace(/\\s+/g,' ').trim();if(!terms.test(text)){continue;}var r=el.getBoundingClientRect();if(!r||r.width<=0||r.height<=0||r.top<window.innerHeight*.52||r.height>96||r.width>window.innerWidth*.82){continue;}var box=el;for(var p=el.parentElement;p&&p!==document.body;p=p.parentElement){var pr=p.getBoundingClientRect();if(!pr||pr.width<=0||pr.height<=0){break;}if(pr.top<window.innerHeight*.5||pr.height>110||pr.width>window.innerWidth*.9){break;}box=p;}box.style.display='none';box.style.pointerEvents='none';box.style.visibility='hidden';box.setAttribute('data-signaldeck-native-meter','true');}}"
                + "function markDecoderPanels(){var tables=document.body?document.body.querySelectorAll('table'):[];for(var i=0;i<tables.length;i++){var text=((tables[i].innerText||tables[i].textContent||'')+'').replace(/\\s+/g,' ').trim().toLowerCase();if(!/(freq\\s+text|id\\s+device|pressure_kpa|temperature_c|crc|tpms|packet|callsign|message)/.test(text)){continue;}var box=tables[i].closest('.openwebrx-panel,.openwebrx-message-panel,.openwebrx-meta-panel')||tables[i].parentElement;for(var p=box;p&&p!==document.body;p=p.parentElement){var r=p.getBoundingClientRect();if(r&&r.width>window.innerWidth*.55&&r.height>38&&r.height<window.innerHeight*.5){box=p;break;}}if(box){box.setAttribute('data-signaldeck-decoder-panel','true');}}}"
                + "function hideNativeImageExpander(){var nodes=document.body?document.body.querySelectorAll('*'):[];for(var i=0;i<nodes.length;i++){var el=nodes[i];if(el.id==='signaldeck-receiver-handle'){continue;}var r=el.getBoundingClientRect();if(!r||r.width<=0||r.height<=0){continue;}var text=(ownText(el)||'').trim().toLowerCase();var key=((el.id||'')+' '+(el.className||'')).toLowerCase();var center=Math.abs((r.left+r.right)/2-window.innerWidth/2);if(center<104&&r.width>=22&&r.width<=150&&r.height>=10&&r.height<=70&&r.top>54&&r.top<122&&(text.length===0||/arrow|expand|collapse|toggle|image|photo|handle/.test(key))){el.style.display='none';el.style.pointerEvents='none';el.setAttribute('data-signaldeck-hidden','true');}if((text==='antena'||text==='antenna'||text.indexOf('autor:')===0||text.indexOf('author:')===0)&&r.top>54&&r.top<window.innerHeight*.5){var box=el;for(var p=el.parentElement;p&&p!==document.body;p=p.parentElement){var pr=p.getBoundingClientRect();if(pr.width>window.innerWidth*.68&&pr.height>36&&pr.height<window.innerHeight*.55){box=p;break;}}box.style.display='none';box.style.pointerEvents='none';box.setAttribute('data-signaldeck-hidden','true');}}}"
                + "function installReceiverSwipe(){if(window.__signalDeckReceiverSwipe){return;}window.__signalDeckReceiverSwipe=true;var sx=0,sy=0,fromRight=false,onPanel=false;document.addEventListener('touchstart',function(e){var t=(e.touches&&e.touches.length)?e.touches[0]:null;if(!t){return;}sx=t.clientX;sy=t.clientY;fromRight=sx>window.innerWidth-34;onPanel=!!(e.target&&e.target.closest&&e.target.closest('#openwebrx-panel-receiver'));},{passive:true});document.addEventListener('touchend',function(e){var t=(e.changedTouches&&e.changedTouches.length)?e.changedTouches[0]:null;if(!t){return;}var dx=t.clientX-sx,dy=t.clientY-sy;if(Math.abs(dy)>90||Math.abs(dx)<70){return;}if(!receiverVisible(document.getElementById('openwebrx-panel-receiver'))&&fromRight&&dx<-72){showReceiver();return;}if(receiverVisible(document.getElementById('openwebrx-panel-receiver'))&&onPanel&&dx>72){hideReceiver();return;}},{passive:true});}"
                + "function ensureReceiverTab(){if(document.getElementById('signaldeck-receiver-tab')){return;}var tab=document.createElement('div');tab.id='signaldeck-receiver-tab';tab.textContent='Receiver';tab.addEventListener('click',function(){showReceiver();});document.body.appendChild(tab);}"
                + "hideForeignPanels();hideReceiverSections();tidyDigRow();ensureWaterfallControls();ensureDefaultStep();markDecoderPanels();hideNativeImageExpander();hideNativeStatusMeters();installReceiverSwipe();ensureReceiverTab();setInterval(ensureWaterfallControls,900);setInterval(ensureDefaultStep,900);setInterval(markDecoderPanels,1200);"
                + "new MutationObserver(function(){hideForeignPanels();hideReceiverSections();tidyDigRow();markDecoderPanels();hideNativeImageExpander();hideNativeStatusMeters();installReceiverSwipe();ensureReceiverTab();}).observe(document.body,{childList:true,subtree:true});"
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
        statusLogTicks++;
        String cleanedFrequency = cleanFrequency(freq);
        boolean hasReceiverData = cleanedFrequency.length() > 0
                && !"0 Hz".equals(cleanedFrequency)
                && !("0 d".equals(meter) || "0 dB".equals(meter));

        if (statusLogTicks % 5 == 0 || !hasReceiverData) {
            logDebug("status receiver=" + currentReceiver.name
                    + " freq=" + cleanedFrequency
                    + " step=" + step
                    + " meter=" + meter
                    + " clock=" + clock
                    + " hasData=" + hasReceiverData
                    + " emptyTicks=" + emptyStatusTicks);
            logWebViewSnapshot("status");
        }

        if (hasReceiverData) {
            emptyStatusTicks = 0;
        } else {
            emptyStatusTicks++;
        }

        if (hasReceiverData) {
            frequencyText.setText(cleanedFrequency);
        } else if (emptyStatusTicks >= 6) {
            frequencyText.setText("No data");
        } else if (freq.length() > 0) {
            frequencyText.setText(cleanedFrequency);
        }
        if (tuningKnob != null) {
            tuningKnob.setStepLabel(step);
        }

        StringBuilder status = new StringBuilder();
        if (!hasReceiverData && emptyStatusTicks >= 6) {
            status.append("No receiver data");
        } else if (step.length() > 0) {
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

    private LinearLayout.LayoutParams columnControlParams(boolean button) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                button ? dp(27) : dp(32)
        );
        params.setMargins(dp(2), dp(2), dp(2), dp(2));
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
        params.setMargins(0, 0, 0, Math.max(dp(8), safeBottomInset));
        return params;
    }

    private FrameLayout.LayoutParams collapsedPanelParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dp(102),
                dp(34)
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.setMargins(0, 0, 0, Math.max(dp(6), safeBottomInset));
        return params;
    }

    private FrameLayout.LayoutParams drawerParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(0, safeTopInset, 0, safeBottomInset);
        return params;
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

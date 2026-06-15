package org.kilohertz.owrxplus;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiverCatalog {
    private static final String CATALOG_URL = "https://rx-tx.info/map-sdr-points";
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "<div\\s+data-views-row-index=\"\\d+\".*?</div>\\s*</div>\\s*</div>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern LAT_PATTERN = Pattern.compile("data-lat=\"([^\"]+)\"");
    private static final Pattern LNG_PATTERN = Pattern.compile("data-lng=\"([^\"]+)\"");
    private static final Pattern TITLE_PATTERN = Pattern.compile("<h3>(.*?)</h3>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern URL_PATTERN = Pattern.compile("<a\\s+href=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOCATOR_PATTERN = Pattern.compile("<br\\s*/?>\\s*([A-R]{2}\\d{2}[a-x]{0,2})\\s*:", Pattern.CASE_INSENSITIVE);
    private static final Pattern COUNTRY_QTH_PATTERN = Pattern.compile("<b>([^<]+)</b>:\\s*([^<]+)<br\\s*/?>", Pattern.CASE_INSENSITIVE);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onLoaded(List<ReceiverInfo> receivers, String source);

        void onError(List<ReceiverInfo> fallback, String message);
    }

    public void loadOpenWebRxReceivers(final Callback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String html = fetch(CATALOG_URL);
                    final List<ReceiverInfo> receivers = parseOpenWebRx(html);
                    if (receivers.isEmpty()) {
                        throw new IllegalStateException("No OpenWebRX receivers found");
                    }
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onLoaded(receivers, "rx-tx.info");
                        }
                    });
                } catch (final Exception exception) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onError(fallbackReceivers(), exception.getMessage());
                        }
                    });
                }
            }
        });
    }

    private String fetch(String urlString) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("User-Agent", "SignalDeck Android receiver catalog");

        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            builder.append(buffer, 0, read);
        }
        reader.close();
        return builder.toString();
    }

    private List<ReceiverInfo> parseOpenWebRx(String html) {
        Map<String, ReceiverInfo> byUrl = new LinkedHashMap<String, ReceiverInfo>();
        Matcher rowMatcher = ROW_PATTERN.matcher(html);
        while (rowMatcher.find()) {
            String row = rowMatcher.group();
            if (!row.toLowerCase(Locale.US).contains("openwebrx")) {
                continue;
            }

            String title = clean(match(TITLE_PATTERN, row));
            String url = clean(match(URL_PATTERN, row));
            if (title.length() == 0 || url.length() == 0) {
                continue;
            }

            String locator = clean(match(LOCATOR_PATTERN, row)).toUpperCase(Locale.US);
            String country = "";
            String qth = "";
            Matcher cq = COUNTRY_QTH_PATTERN.matcher(row);
            if (cq.find()) {
                country = clean(cq.group(1));
                qth = clean(cq.group(2));
            }

            double lat = parseDouble(match(LAT_PATTERN, row));
            double lng = parseDouble(match(LNG_PATTERN, row));
            byUrl.put(url, new ReceiverInfo(title, normalizeUrl(url), locator, country, qth, lat, lng));
        }

        List<ReceiverInfo> receivers = new ArrayList<ReceiverInfo>(byUrl.values());
        Collections.sort(receivers, new Comparator<ReceiverInfo>() {
            @Override
            public int compare(ReceiverInfo first, ReceiverInfo second) {
                return first.name.compareToIgnoreCase(second.name);
            }
        });
        return receivers;
    }

    private String match(Pattern pattern, String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String clean(String value) {
        return value
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String normalizeUrl(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    public List<ReceiverInfo> fallbackReceivers() {
        List<ReceiverInfo> receivers = new ArrayList<ReceiverInfo>();
        receivers.add(new ReceiverInfo(
                "kilohertz_sdr",
                "https://sdr.kilohertz021.org/",
                "JN95wg",
                "RS",
                "Novi Sad",
                45.267,
                19.833
        ));
        receivers.add(new ReceiverInfo(
                "2m/70cm | Berlin - JO62QJ | DC7JZB",
                "http://90.187.72.177:8073/",
                "JO62qj",
                "DE",
                "Lichtenrade",
                52.407796292384,
                13.403881331682
        ));
        receivers.add(new ReceiverInfo(
                "F1FHL",
                "http://aubjpla.myddns.me:8073/",
                "JN17fl",
                "FR",
                "Aubigny-sur-Nere",
                47.467949235863,
                2.4548189598526
        ));
        receivers.add(new ReceiverInfo(
                "DK0TE",
                "http://dk0te.dhbw-ravensburg.de:8073/",
                "JN47rp",
                "DE",
                "Friedrichshafen",
                47.664995,
                9.448133
        ));
        return receivers;
    }
}

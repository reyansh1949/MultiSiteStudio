package com.multisite.studio;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.content.SharedPreferences;
import android.text.InputType;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private final ArrayList<WebView> webViews = new ArrayList<>();
    private final ArrayList<EditText> urlFields = new ArrayList<>();
    private final ArrayList<LinearLayout> siteCells = new ArrayList<>();

    private LinearLayout root;
    private LinearLayout homeLayout;
    private LinearLayout settingsLayout;
    private boolean settingsOpen = false;
    private GridLayout grid;
    private ScrollView gridScroll;
    private int normalCellWidth;
    private int normalCellHeight;
    private Button fullBackButton;

    private FrameLayout fullscreenContainer;
    private GridLayout.LayoutParams savedCellParams;
    private ViewGroup.LayoutParams savedWebParams;
    private int savedGridIndex;
    private int savedWebChildIndex;
    private EditText bulkUrl;
    private Spinner startSpinner;
    private Spinner endSpinner;
    private int startSite = 1;
    private int endSite = 4;
    private LinearLayout soundControlsContainer;
    private LinearLayout proxyControlsContainer;
    private boolean muteAll = false;
    private final ArrayList<Boolean> siteMuteStates = new ArrayList<>();
    private EditText jumpNumber;
    private SharedPreferences prefs;
    private int siteCount = 4;
    private boolean profilesSupported = false;
    private int fullScreenIndex = -1;
    private ViewGroup fullscreenParent;
    private int fullscreenChildIndex;
    private final ArrayList<View> savedRootViews = new ArrayList<>();
    private final ArrayList<Integer> savedRootVisibility = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences("multisite_data", MODE_PRIVATE);
        int oldCount = Math.max(1, Math.min(25,
                prefs.getInt("site_count", 4)));
        startSite = Math.max(1, Math.min(25,
                prefs.getInt("site_start", 1)));
        endSite = Math.max(startSite, Math.min(25,
                prefs.getInt("site_end", oldCount)));
        siteCount = endSite - startSite + 1;

profilesSupported = WebViewFeature.isFeatureSupported(
        WebViewFeature.MULTI_PROFILE);

Toast.makeText(this,
        "Multi-Profile Supported: " + profilesSupported,
        Toast.LENGTH_LONG).show();
        buildInterface();
        createSites();
    }


    private static final int JJK_BG = Color.rgb(9, 10, 19);
    private static final int JJK_CARD = Color.rgb(23, 24, 39);
    private static final int JJK_SURFACE = Color.rgb(32, 34, 56);
    private static final int JJK_PURPLE = Color.rgb(155, 92, 255);
    private static final int JJK_RED = Color.rgb(229, 57, 85);
    private static final int JJK_TEXT = Color.rgb(245, 242, 255);
    private static final int JJK_MUTED = Color.rgb(170, 167, 188);

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private android.graphics.drawable.GradientDrawable rounded(
            int fill, int stroke, int radius) {
        android.graphics.drawable.GradientDrawable shape =
                new android.graphics.drawable.GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        shape.setStroke(dp(1), stroke);
        return shape;
    }

    private void styleButton(Button button, int color) {
        button.setTextColor(JJK_TEXT);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinHeight(dp(36));
        button.setPadding(dp(10), dp(5), dp(10), dp(5));
        button.setBackground(rounded(color, JJK_PURPLE, 10));
    }

    private void styleEditText(EditText edit) {
        edit.setTextColor(JJK_TEXT);
        edit.setHintTextColor(JJK_MUTED);
        edit.setPadding(dp(9), dp(5), dp(9), dp(5));
        edit.setBackground(rounded(JJK_SURFACE, JJK_PURPLE, 8));
    }


    private ArrayAdapter<String> whiteSpinnerAdapter(String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, items) {
            @Override
            public View getView(int position, View convertView,
                    android.view.ViewGroup parent) {
                TextView text = (TextView) super.getView(
                        position, convertView, parent);
                text.setTextColor(JJK_TEXT);
                text.setTextSize(14);
                return text;
            }

            @Override
            public View getDropDownView(int position, View convertView,
                    android.view.ViewGroup parent) {
                TextView text = (TextView) super.getDropDownView(
                        position, convertView, parent);
                text.setTextColor(JJK_TEXT);
                text.setBackgroundColor(JJK_SURFACE);
                text.setPadding(dp(12), dp(10), dp(12), dp(10));
                return text;
            }
        };
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void buildInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int statusBarHeight = 0;
        int statusBarId = getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (statusBarId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(statusBarId);
        }
        root.setPadding(5, 5 + statusBarHeight, 5, 5);
        root.setBackgroundColor(JJK_BG);
        getWindow().setStatusBarColor(JJK_BG);
        getWindow().setNavigationBarColor(JJK_BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        homeLayout = new LinearLayout(this);
        homeLayout.setOrientation(LinearLayout.VERTICAL);

        settingsLayout = new LinearLayout(this);
        settingsLayout.setOrientation(LinearLayout.VERTICAL);
        settingsLayout.setPadding(dp(6), dp(5), dp(6), dp(5));
        settingsLayout.setBackgroundColor(JJK_BG);
        settingsLayout.setVisibility(View.GONE);

        TextView settingsTitle = new TextView(this);
        settingsTitle.setText("SETTINGS");
        settingsTitle.setTextSize(20);
        settingsTitle.setTextColor(JJK_PURPLE);
        settingsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        settingsTitle.setGravity(Gravity.CENTER);
        settingsLayout.addView(settingsTitle);

        Button backHome = new Button(this);
        backHome.setText("← Back to Home");
        styleButton(backHome, JJK_SURFACE);
        backHome.setOnClickListener(v -> showHomePage());
        settingsLayout.addView(backHome);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(5), dp(6), dp(5));
        row.setBackground(rounded(JJK_CARD, JJK_PURPLE, 10));

        TextView label = new TextView(this);
        label.setText("Websites: ");
        label.setTextColor(JJK_TEXT);
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(label);

        TextView fromLabel = new TextView(this);
        fromLabel.setText("From:");
        fromLabel.setTextColor(JJK_TEXT);
        row.addView(fromLabel);

        String[] rangeValues = new String[25];
        for (int i = 0; i < 25; i++) rangeValues[i] = String.valueOf(i + 1);

        ArrayAdapter<String> rangeAdapter = whiteSpinnerAdapter(rangeValues);

        startSpinner = new Spinner(this);
        startSpinner.setAdapter(rangeAdapter);
        startSpinner.setSelection(startSite - 1);
        row.addView(startSpinner);

        TextView toLabel = new TextView(this);
        toLabel.setText(" To:");
        toLabel.setTextColor(JJK_TEXT);
        row.addView(toLabel);

        endSpinner = new Spinner(this);
        endSpinner.setAdapter(rangeAdapter);
        endSpinner.setSelection(endSite - 1);
        row.addView(endSpinner);

        AdapterView.OnItemSelectedListener listener =
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                int from = startSpinner.getSelectedItemPosition() + 1;
                int to = endSpinner.getSelectedItemPosition() + 1;
                if (from > to) {
                    if (parent == startSpinner) {
                        endSpinner.setSelection(from - 1);
                        to = from;
                    } else {
                        startSpinner.setSelection(to - 1);
                        from = to;
                    }
                }
                applyWebsiteRange(from, to);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        startSpinner.setOnItemSelectedListener(listener);
        endSpinner.setOnItemSelectedListener(listener);

        settingsLayout.addView(row);

        LinearLayout jumpRow = new LinearLayout(this);
        jumpRow.setOrientation(LinearLayout.HORIZONTAL);
        jumpRow.setGravity(Gravity.CENTER_VERTICAL);
        jumpRow.setPadding(dp(8), dp(5), dp(8), dp(5));
        jumpRow.setBackground(rounded(JJK_CARD, JJK_PURPLE, 10));

        TextView jumpLabel = new TextView(this);
        jumpLabel.setText("Jump to:");
        jumpLabel.setTextColor(JJK_TEXT);
        jumpRow.addView(jumpLabel);

        jumpNumber = new EditText(this);
        jumpNumber.setSingleLine(true);
        jumpNumber.setTextSize(14);
        jumpNumber.setInputType(InputType.TYPE_CLASS_NUMBER);
        jumpNumber.setHint("Website #");
        styleEditText(jumpNumber);
        LinearLayout.LayoutParams jumpInputParams =
                new LinearLayout.LayoutParams(0, -2, 1f);
        jumpInputParams.setMargins(dp(10), 0, dp(8), 0);
        jumpRow.addView(jumpNumber, jumpInputParams);

        Button jumpButton = new Button(this);
        jumpButton.setText("GO");
        jumpButton.setTextSize(10);
        styleButton(jumpButton, JJK_PURPLE);
        jumpButton.setOnClickListener(v -> jumpToSite());
        jumpRow.addView(jumpButton, new LinearLayout.LayoutParams(-2, -2));

        settingsLayout.addView(jumpRow);
        buildSoundControls();
        buildProxyControls();

        TextView title = new TextView(this);
        title.setText("MULTISITE");
        title.setTextSize(21);
        title.setTextColor(JJK_PURPLE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setShadowLayer(dp(12), 0, 0, JJK_PURPLE);
        title.setGravity(Gravity.CENTER);
        homeLayout.addView(title);

        Button settingsButton = new Button(this);
        settingsButton.setText("⚙ Settings");
        styleButton(settingsButton, JJK_SURFACE);
        settingsButton.setOnClickListener(v -> showSettingsPage());
        homeLayout.addView(settingsButton);

        bulkUrl = new EditText(this);
        bulkUrl.setSingleLine(true);
        bulkUrl.setHint("One URL for all websites");
        styleEditText(bulkUrl);
        homeLayout.addView(bulkUrl);

        LinearLayout actions = new LinearLayout(this);
        actions.setPadding(0, dp(4), 0, dp(4));

        Button openAll = new Button(this);
        openAll.setText("Open All");
        styleButton(openAll, JJK_PURPLE);
        openAll.setOnClickListener(v -> openBulk());

        Button refresh = new Button(this);
        refresh.setText("Refresh All");
        styleButton(refresh, JJK_RED);
        refresh.setOnClickListener(v -> {
            for (WebView web : webViews) web.reload();
        });

        actions.addView(openAll, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(refresh, new LinearLayout.LayoutParams(0, -2, 1));
        homeLayout.addView(actions);

        ScrollView scroll = new ScrollView(this);
        gridScroll = scroll;
        grid = new GridLayout(this);
        grid.setColumnCount(1);
        scroll.addView(grid);

        homeLayout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(homeLayout, new LinearLayout.LayoutParams(-1, -1));
        root.addView(settingsLayout, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);
    }


    private void showSettingsPage() {
        if (fullScreenIndex >= 0) return;
        settingsOpen = true;
        homeLayout.setVisibility(View.GONE);
        settingsLayout.setVisibility(View.VISIBLE);
    }

    private void showHomePage() {
        settingsOpen = false;
        settingsLayout.setVisibility(View.GONE);
        homeLayout.setVisibility(View.VISIBLE);
    }

    private void applyWebsiteRange(int from, int to) {
        if (fullScreenIndex >= 0) return;
        if (from == startSite && to == endSite) return;

        saveUrls();

        startSite = from;
        endSite = to;
        siteCount = endSite - startSite + 1;

        prefs.edit()
                .putInt("site_start", startSite)
                .putInt("site_end", endSite)
                .putInt("site_count", siteCount)
                .apply();

        createSites();
        buildSoundControls();
        buildProxyControls();

        Toast.makeText(this,
                "Showing Website " + startSite + " to " + endSite,
                Toast.LENGTH_SHORT).show();
    }


    private void jumpToSite() {
        String value = jumpNumber.getText().toString().trim();

        if (value.isEmpty()) {
            Toast.makeText(this, "Enter website number", Toast.LENGTH_SHORT).show();
            return;
        }

        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid website number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (number < startSite || number > endSite) {
            Toast.makeText(this, "Enter a number from " + startSite +
                    " to " + endSite, Toast.LENGTH_SHORT).show();
            return;
        }

        int index = number - startSite;
        if (index < siteCells.size()) {
            gridScroll.post(() -> gridScroll.smoothScrollTo(
                    0, siteCells.get(index).getTop()));
        }
    }

    private void createSites() {
        if (!profilesSupported) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Separate Sessions Not Supported")
                .setMessage("Your Android System WebView does not support isolated website profiles. Websites will not be opened because shared login sessions are not allowed.")
                .setPositiveButton("OK", null)
                .show();
            return;
        }

        for (WebView web : webViews) {
            web.stopLoading();
            web.destroy();
        }

        webViews.clear();
        siteCells.clear();
        urlFields.clear();
        grid.removeAllViews();

        int width = getResources().getDisplayMetrics().widthPixels - 14;
        int videoHeight = (int)(width * 9f / 16f);
        int extraHeight = (int)(getResources().getDisplayMetrics().density * 120);
        normalCellWidth = width;
        normalCellHeight = videoHeight + extraHeight;

        for (int i = 0; i < siteCount; i++) {
            final int index = i;
            final int globalIndex = startSite + i - 1;
            final int websiteNumber = globalIndex + 1;

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setPadding(dp(5), dp(5), dp(5), dp(5));
            cell.setBackground(rounded(JJK_CARD, JJK_PURPLE, 12));
            siteCells.add(cell);

            TextView label = new TextView(this);
            label.setText("Website " + websiteNumber);
            label.setGravity(Gravity.CENTER);
            label.setTextColor(JJK_PURPLE);
            label.setTypeface(null, android.graphics.Typeface.BOLD);
            label.setTextSize(14);
            label.setPadding(0, dp(4), 0, dp(4));
            cell.addView(label);

            EditText url = new EditText(this);
            url.setSingleLine(true);
            url.setTextSize(9);
            url.setHint("URL");
            styleEditText(url);
            url.setText(prefs.getString("url_" + globalIndex, ""));
            urlFields.add(url);
            cell.addView(url, new LinearLayout.LayoutParams(-1, 35));

            WebView web = new WebView(this);
            web.setBackgroundColor(Color.BLACK);

            if (profilesSupported) {
                try {
                    WebViewCompat.setProfile(web, "site_" + websiteNumber);
                } catch (Exception e) {
                    web.destroy();

                    for (WebView existing : webViews) {
                        existing.stopLoading();
                        existing.destroy();
                    }

                    webViews.clear();
                    siteCells.clear();
                    urlFields.clear();
                    grid.removeAllViews();

                    Toast.makeText(this,
                            "Profile setup failed. All sites stopped to protect session isolation.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }

            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setMediaPlaybackRequiresUserGesture(false);
            web.getSettings().setDomStorageEnabled(true);
            web.getSettings().setSupportZoom(true);
            web.getSettings().setBuiltInZoomControls(true);
            web.getSettings().setDisplayZoomControls(false);

            web.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    applyMuteToWebView(view, index);
                }
            });
            webViews.add(web);

            cell.addView(web, new LinearLayout.LayoutParams(-1, videoHeight));

            LinearLayout buttons = new LinearLayout(this);

            Button go = new Button(this);
            go.setText("Go");
            go.setTextSize(9);
            styleButton(go, JJK_PURPLE);
            go.setOnClickListener(v -> {
                String address = normalize(urlFields.get(index).getText().toString());
                if (address != null) {
                    prefs.edit().putString(
                            "url_" + (startSite + index - 1), address).apply();
                    loadWebsite(webViews.get(index), address);
                }
            });

            Button full = new Button(this);
            full.setText("Full");
            full.setTextSize(9);
            styleButton(full, JJK_RED);
            full.setOnClickListener(v -> showFull(index));

            buttons.addView(go, new LinearLayout.LayoutParams(0, -2, 1));
            buttons.addView(full, new LinearLayout.LayoutParams(0, -2, 1));
            cell.addView(buttons);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = width;
            params.height = normalCellHeight;
            params.setMargins(2, 2, 2, 2);
            grid.addView(cell, params);
        }
    }

    private String normalize(String input) {
        String address = input.trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "Enter URL", Toast.LENGTH_SHORT).show();
            return null;
        }
        if (!address.startsWith("http://") &&
                !address.startsWith("https://")) {
            address = "https://" + address;
        }
        return address;
    }

    private String getYouTubeVideoId(String address) {
        try {
            Uri uri = Uri.parse(address);
            String host = uri.getHost();

            if (host == null) return null;
            host = host.toLowerCase();

            if (host.equals("youtu.be")) {
                if (!uri.getPathSegments().isEmpty())
                    return uri.getPathSegments().get(0);
            }

            if (host.equals("youtube.com") ||
                    host.endsWith(".youtube.com")) {

                String videoId = uri.getQueryParameter("v");
                if (videoId != null && !videoId.isEmpty())
                    return videoId;

                java.util.List<String> parts = uri.getPathSegments();

                if (parts.size() >= 2) {
                    String type = parts.get(0);

                    if (type.equals("shorts") ||
                            type.equals("embed") ||
                            type.equals("live")) {
                        return parts.get(1);
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    private void loadWebsite(WebView web, String address) {
        String videoId = getYouTubeVideoId(address);

        if (videoId != null) {
            String html =
                    "<!DOCTYPE html>" +
                    "<html><head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1'>" +
                    "<style>" +
                    "html,body{margin:0;padding:0;width:100%;height:100%;overflow:hidden;background:#000;}" +
                    "iframe{display:block;width:100%;height:100%;border:0;}" +
                    "</style></head><body>" +
                    "<iframe src='https://www.youtube.com/embed/" + videoId +
                    "?autoplay=1&mute=1&playsinline=1&rel=0&controls=1&enablejsapi=1&origin=https://com.multisite.studio' " +
                    "referrerpolicy='strict-origin' " +
                    "allow='autoplay; encrypted-media; picture-in-picture; fullscreen' " +
                    "allowfullscreen></iframe>" +
                    "</body></html>";

            web.loadDataWithBaseURL(
                    "https://com.multisite.studio/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
            );
        } else {
            web.loadUrl(address);
        }
    }


    private void buildSoundControls() {
        if (soundControlsContainer != null) {
            settingsLayout.removeView(soundControlsContainer);
        }

        soundControlsContainer = new LinearLayout(this);
        soundControlsContainer.setOrientation(LinearLayout.VERTICAL);
        soundControlsContainer.setPadding(dp(8), dp(8), dp(8), dp(5));
        soundControlsContainer.setBackground(
                rounded(JJK_CARD, JJK_PURPLE, 10));

        TextView heading = new TextView(this);
        heading.setText("SOUND CONTROL");
        heading.setTextColor(JJK_PURPLE);
        heading.setTextSize(15);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setPadding(dp(5), dp(3), dp(5), dp(8));
        soundControlsContainer.addView(heading);

        muteAll = prefs.getBoolean("mute_all", false);

        Switch allSwitch = new Switch(this);
        allSwitch.setText("Mute All Websites");
        allSwitch.setTextColor(JJK_TEXT);
        allSwitch.setTextSize(13);
        allSwitch.setChecked(muteAll);

        allSwitch.setOnCheckedChangeListener((button, checked) -> {
            muteAll = checked;
            prefs.edit().putBoolean("mute_all", checked).apply();
            applyMuteToAllWebViews();
        });

        soundControlsContainer.addView(allSwitch);

        TextView individualHeading = new TextView(this);
        individualHeading.setText("Individual Website Sound");
        individualHeading.setTextColor(JJK_MUTED);
        individualHeading.setTextSize(12);
        individualHeading.setPadding(dp(5), dp(8), dp(5), dp(5));
        soundControlsContainer.addView(individualHeading);

        siteMuteStates.clear();

        ArrayList<String> siteNames = new ArrayList<>();

        for (int i = 0; i < siteCount; i++) {
            int globalIndex = startSite + i - 1;
            siteNames.add("Website " + (globalIndex + 1));
            siteMuteStates.add(
                    prefs.getBoolean("mute_site_" + globalIndex, false));
        }

        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setGravity(Gravity.CENTER_VERTICAL);
        soundRow.setPadding(dp(5), dp(5), dp(5), dp(5));

        Spinner siteSpinner = new Spinner(this);

        ArrayAdapter<String> soundAdapter =
                whiteSpinnerAdapter(siteNames.toArray(new String[0]));

        siteSpinner.setAdapter(soundAdapter);

        LinearLayout.LayoutParams spinnerParams =
                new LinearLayout.LayoutParams(0, dp(48), 1f);

        soundRow.addView(siteSpinner, spinnerParams);

        Button muteButton = new Button(this);
        muteButton.setTextColor(JJK_TEXT);
        muteButton.setTextSize(12);
        muteButton.setAllCaps(false);
        muteButton.setPadding(dp(12), 0, dp(12), 0);
        muteButton.setBackground(
                rounded(JJK_CARD, JJK_PURPLE, 8));

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        -2, dp(42));

        buttonParams.setMargins(dp(8), 0, 0, 0);
        soundRow.addView(muteButton, buttonParams);

        final int[] selectedSite = {0};

        muteButton.setText(
                siteMuteStates.get(0) ? "Unmute" : "Mute");

        siteSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {

                        selectedSite[0] = position;

                        muteButton.setText(
                                siteMuteStates.get(position)
                                        ? "Unmute" : "Mute");
                    }

                    @Override
                    public void onNothingSelected(
                            AdapterView<?> parent) {
                    }
                });

        muteButton.setOnClickListener(v -> {
            int index = selectedSite[0];

            boolean newMuteState =
                    !siteMuteStates.get(index);

            siteMuteStates.set(index, newMuteState);

            int globalIndex = startSite + index - 1;
            prefs.edit()
                    .putBoolean("mute_site_" + globalIndex, newMuteState)
                    .apply();

            muteButton.setText(
                    newMuteState ? "Unmute" : "Mute");

            if (index < webViews.size()) {
                applyMuteToWebView(
                        webViews.get(index), index);
            }
        });

        soundControlsContainer.addView(soundRow);

        settingsLayout.addView(soundControlsContainer);
    }


    private String proxySlotKey(String country, int slot, String field) {
        return "proxy_" + country + "_" + slot + "_" + field;
    }

    private void buildProxyControls() {
        if (proxyControlsContainer != null) {
            settingsLayout.removeView(proxyControlsContainer);
        }

        proxyControlsContainer = new LinearLayout(this);
        proxyControlsContainer.setOrientation(LinearLayout.VERTICAL);
        proxyControlsContainer.setPadding(dp(10), dp(10), dp(10), dp(10));
        proxyControlsContainer.setBackground(
                rounded(JJK_CARD, JJK_PURPLE, 10));

        TextView heading = new TextView(this);
        heading.setText("COUNTRY PROXY MANAGER");
        heading.setTextColor(JJK_PURPLE);
        heading.setTextSize(15);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        proxyControlsContainer.addView(heading);

        TextView info = new TextView(this);
        info.setText("10 proxy slots per country | Manual IP:PORT");
        info.setTextColor(JJK_MUTED);
        info.setTextSize(11);
        info.setPadding(0, dp(5), 0, dp(10));
        proxyControlsContainer.addView(info);

        ArrayList<String> sites = new ArrayList<>();
        for (int i = 0; i < siteCount; i++) {
            sites.add("Website " + (startSite + i));
        }

        Spinner siteSpinner = new Spinner(this);
        siteSpinner.setAdapter(
                whiteSpinnerAdapter(sites.toArray(new String[0])));
        proxyControlsContainer.addView(siteSpinner);

        String[] codes = Locale.getISOCountries();
        ArrayList<String> countryCodes = new ArrayList<>();
        ArrayList<String> countryNames = new ArrayList<>();

        for (String code : codes) {
            Locale locale = new Locale("", code);
            countryCodes.add(code);
            countryNames.add(locale.getDisplayCountry(Locale.ENGLISH));
        }

        Spinner countrySpinner = new Spinner(this);
        countrySpinner.setAdapter(
                whiteSpinnerAdapter(countryNames.toArray(new String[0])));
        proxyControlsContainer.addView(countrySpinner);

        ArrayList<String> slots = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            slots.add("Proxy Slot " + i);
        }

        Spinner slotSpinner = new Spinner(this);
        slotSpinner.setAdapter(
                whiteSpinnerAdapter(slots.toArray(new String[0])));
        proxyControlsContainer.addView(slotSpinner);

        TextView ipLabel = new TextView(this);
        ipLabel.setText("Proxy IP:PORT");
        ipLabel.setTextColor(JJK_TEXT);
        proxyControlsContainer.addView(ipLabel);

        EditText endpoint = new EditText(this);
        endpoint.setSingleLine(true);
        endpoint.setHint("Example: 192.0.2.10:8080");
        endpoint.setTextSize(13);
        styleEditText(endpoint);
        proxyControlsContainer.addView(endpoint);

        TextView userLabel = new TextView(this);
        userLabel.setText("Username (optional)");
        userLabel.setTextColor(JJK_TEXT);
        proxyControlsContainer.addView(userLabel);

        EditText username = new EditText(this);
        username.setSingleLine(true);
        username.setHint("Proxy username");
        username.setTextSize(13);
        styleEditText(username);
        proxyControlsContainer.addView(username);

        TextView passLabel = new TextView(this);
        passLabel.setText("Password (optional)");
        passLabel.setTextColor(JJK_TEXT);
        proxyControlsContainer.addView(passLabel);

        EditText password = new EditText(this);
        password.setSingleLine(true);
        password.setHint("Proxy password");
        password.setInputType(
                InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setTextSize(13);
        styleEditText(password);
        proxyControlsContainer.addView(password);

        Runnable loadSlot = () -> {
            int ci = countrySpinner.getSelectedItemPosition();
            int si = slotSpinner.getSelectedItemPosition();

            if (ci < 0 || ci >= countryCodes.size()) return;

            String code = countryCodes.get(ci);
            endpoint.setText(prefs.getString(
                    proxySlotKey(code, si, "endpoint"), ""));
            username.setText(prefs.getString(
                    proxySlotKey(code, si, "username"), ""));
            password.setText(prefs.getString(
                    proxySlotKey(code, si, "password"), ""));
        };

        int indiaIndex = countryCodes.indexOf("IN");
        if (indiaIndex >= 0) countrySpinner.setSelection(indiaIndex);

        countrySpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                loadSlot.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        slotSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                loadSlot.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        siteSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                    View view, int position, long id) {
                int globalSiteIndex = startSite + position - 1;
                String assignedCountry = prefs.getString(
                        "proxy_site_country_" + globalSiteIndex, "IN");
                int ci = countryCodes.indexOf(assignedCountry);
                countrySpinner.setSelection(ci >= 0 ? ci : 0);

                int assignedSlot = prefs.getInt(
                        "proxy_site_slot_" + globalSiteIndex, 0);
                slotSpinner.setSelection(
                        Math.max(0, Math.min(9, assignedSlot)));
                loadSlot.run();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Button saveButton = new Button(this);
        saveButton.setText("Save Country Proxy Slot");
        styleButton(saveButton, JJK_PURPLE);

        saveButton.setOnClickListener(v -> {
            int ci = countrySpinner.getSelectedItemPosition();
            int si = slotSpinner.getSelectedItemPosition();

            if (ci < 0 || ci >= countryCodes.size()) return;

            String value = endpoint.getText().toString().trim();

            if (!value.isEmpty() && !value.matches(
                    "^[a-zA-Z0-9.-]+:[0-9]{1,5}$")) {
                Toast.makeText(this,
                        "Enter valid IP:PORT or leave empty",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (!value.isEmpty()) {
                try {
                    int port = Integer.parseInt(
                            value.substring(value.lastIndexOf(':') + 1));
                    if (port < 1 || port > 65535) {
                        throw new NumberFormatException();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Invalid proxy port",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String code = countryCodes.get(ci);

            prefs.edit()
                    .putString(proxySlotKey(code, si, "endpoint"), value)
                    .putString(proxySlotKey(code, si, "username"),
                            username.getText().toString().trim())
                    .putString(proxySlotKey(code, si, "password"),
                            password.getText().toString())
                    .apply();

            Toast.makeText(this, "Proxy slot saved",
                    Toast.LENGTH_SHORT).show();
        });

        proxyControlsContainer.addView(saveButton);

        Button assignButton = new Button(this);
        assignButton.setText("Assign Proxy to Selected Website");
        styleButton(assignButton, JJK_SURFACE);

        assignButton.setOnClickListener(v -> {
            int site = siteSpinner.getSelectedItemPosition();
            int ci = countrySpinner.getSelectedItemPosition();
            int si = slotSpinner.getSelectedItemPosition();

            if (site < 0 || ci < 0 || ci >= countryCodes.size()) return;

            String code = countryCodes.get(ci);
            String savedEndpoint = prefs.getString(
                    proxySlotKey(code, si, "endpoint"), "");

            if (savedEndpoint.isEmpty()) {
                Toast.makeText(this,
                        "Save a proxy in this slot first",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            int globalSiteIndex = startSite + site - 1;
            prefs.edit()
                    .putString("proxy_site_country_" + globalSiteIndex, code)
                    .putInt("proxy_site_slot_" + globalSiteIndex, si)
                    .apply();

            Toast.makeText(this,
                    "Proxy assigned to Website " + (globalSiteIndex + 1),
                    Toast.LENGTH_SHORT).show();
        });

        proxyControlsContainer.addView(assignButton);

        TextView note = new TextView(this);
        note.setText(
                "Saved settings only. Proxy routing is not active yet. " +
                "Use trusted proxy providers; empty slots are not working proxies.");
        note.setTextColor(JJK_MUTED);
        note.setTextSize(11);
        note.setPadding(0, dp(8), 0, 0);
        proxyControlsContainer.addView(note);

        settingsLayout.addView(proxyControlsContainer);
    }

    private void applyMuteToAllWebViews() {
        for (int i = 0; i < webViews.size(); i++) {
            applyMuteToWebView(webViews.get(i), i);
        }
    }

    private void applyMuteToWebView(WebView web, int index) {
        if (web == null) return;

        boolean individualMute =
                index < siteMuteStates.size() &&
                siteMuteStates.get(index);

        boolean shouldMute = muteAll || individualMute;

        String script =
                "(function() {" +
                "var mute = " + shouldMute + ";" +
                "window.__multiSiteMute = mute;" +
                "function applyMedia() {" +
                " var m = window.__multiSiteMute;" +
                " document.querySelectorAll('audio,video').forEach(function(el) {" +
                "   el.muted = m;" +
                " });" +
                "}" +
                "applyMedia();" +
                "if (window.__multiSiteObserver) {" +
                " window.__multiSiteObserver.disconnect();" +
                "}" +
                "if (document.documentElement) {" +
                " window.__multiSiteObserver = new MutationObserver(applyMedia);" +
                " window.__multiSiteObserver.observe(document.documentElement," +
                " {childList:true,subtree:true});" +
                "}" +
                "document.addEventListener('play', applyMedia, true);" +
                "if (window.__multiSiteYtTimer) {" +
                " clearInterval(window.__multiSiteYtTimer);" +
                "}" +
                "var frames = document.querySelectorAll(" +
                " 'iframe[src*=\"youtube.com/embed\"]');" +
                "if (frames.length) {" +
                " var tries = 0;" +
                " window.__multiSiteYtTimer = setInterval(function() {" +
                "  var command = window.__multiSiteMute ? 'mute' : 'unMute';" +
                "  frames.forEach(function(frame) {" +
                "   try {" +
                "    frame.contentWindow.postMessage(JSON.stringify({" +
                "     event:'command',func:command,args:[]" +
                "    }), '*');" +
                "   } catch(e) {}" +
                "  });" +
                "  tries++;" +
                "  if (tries >= 20) clearInterval(window.__multiSiteYtTimer);" +
                " }, 500);" +
                "}" +
                "})();";

        web.evaluateJavascript(script, null);
    }

    private void openBulk() {
        String address = normalize(bulkUrl.getText().toString());
        if (address == null) return;

        for (int i = 0; i < webViews.size(); i++) {
            urlFields.get(i).setText(address);
            prefs.edit().putString(
                    "url_" + (startSite + i - 1), address).apply();
            loadWebsite(webViews.get(i), address);
        }
    }

    private void saveUrls() {
        if (prefs == null) return;
        for (int i = 0; i < urlFields.size(); i++) {
            prefs.edit().putString("url_" + (startSite + i - 1),
                    urlFields.get(i).getText().toString()).apply();
        }
    }


    private void showFull(int index) {
        if (index < 0 || index >= webViews.size()) return;
        if (fullScreenIndex >= 0) return;

        LinearLayout cell = siteCells.get(index);
        WebView web = webViews.get(index);

        savedGridIndex = grid.indexOfChild(cell);
        if (savedGridIndex < 0) return;

        savedCellParams = (GridLayout.LayoutParams) cell.getLayoutParams();
        savedWebParams = web.getLayoutParams();
        savedWebChildIndex = cell.indexOfChild(web);

        savedRootVisibility.clear();
        for (int i = 0; i < root.getChildCount(); i++) {
            savedRootVisibility.add(root.getChildAt(i).getVisibility());
        }

        cell.removeView(web);
        grid.removeView(cell);
        fullScreenIndex = index;

        for (int i = 0; i < root.getChildCount(); i++) {
            root.getChildAt(i).setVisibility(View.GONE);
        }

        fullscreenContainer = new FrameLayout(this);
        fullscreenContainer.setBackgroundColor(JJK_BG);
        fullscreenContainer.addView(web,
                new FrameLayout.LayoutParams(-1, -1));

        root.addView(fullscreenContainer,
                new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void returnToGrid() {
        if (fullScreenIndex < 0) return;

        int index = fullScreenIndex;
        LinearLayout cell = siteCells.get(index);
        WebView web = webViews.get(index);

        if (web.getParent() instanceof ViewGroup) {
            ((ViewGroup) web.getParent()).removeView(web);
        }

        if (fullscreenContainer != null) {
            root.removeView(fullscreenContainer);
            fullscreenContainer = null;
        }

        if (fullBackButton != null) {
            root.removeView(fullBackButton);
            fullBackButton = null;
        }

        cell.addView(web, savedWebChildIndex, savedWebParams);
        grid.addView(cell,
                Math.min(savedGridIndex, grid.getChildCount()),
                savedCellParams);

        for (int i = 0; i < savedRootVisibility.size()
                && i < root.getChildCount(); i++) {
            root.getChildAt(i).setVisibility(savedRootVisibility.get(i));
        }

        savedRootVisibility.clear();
        fullScreenIndex = -1;
        savedCellParams = null;
        savedWebParams = null;
    }

    @Override
    public void onBackPressed() {
        if (fullScreenIndex >= 0) {
            returnToGrid();
        } else if (settingsOpen) {
            showHomePage();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveUrls();
        for (WebView web : webViews) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (WebView web : webViews) web.onResume();
    }

    @Override
    protected void onDestroy() {
        saveUrls();
        for (WebView web : webViews) {
            web.stopLoading();
            web.destroy();
        }
        webViews.clear();
        siteCells.clear();
        super.onDestroy();
    }
}

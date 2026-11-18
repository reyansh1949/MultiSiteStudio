package com.multisite.studio;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.content.SharedPreferences;

import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private final ArrayList<WebView> webViews = new ArrayList<>();
    private final ArrayList<EditText> urlFields = new ArrayList<>();
    private final ArrayList<LinearLayout> siteCells = new ArrayList<>();

    private LinearLayout root;
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
    private Spinner countSpinner;
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
        siteCount = Math.max(1, Math.min(100, prefs.getInt("site_count", 4)));

        profilesSupported = WebViewFeature.isFeatureSupported(
                WebViewFeature.MULTI_PROFILE);

        buildInterface();
        createSites();
    }

    private void buildInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(5, 5, 5, 5);

        TextView title = new TextView(this);
        title.setText("MULTISITE STUDIO");
        title.setTextSize(21);
        title.setTextColor(Color.BLUE);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView status = new TextView(this);
        status.setText(profilesSupported
                ? "Separate login profiles supported"
                : "Warning: Separate login profiles not supported by this WebView");
        status.setTextColor(profilesSupported ? Color.rgb(0,130,0) : Color.RED);
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("Websites: ");
        row.addView(label);

        countSpinner = new Spinner(this);
        String[] values = new String[100];
        for (int i = 0; i < 100; i++) values[i] = String.valueOf(i + 1);

        countSpinner.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, values));
        countSpinner.setSelection(siteCount - 1);

        countSpinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p,
                    View v, int position, long id) {
                int count = position + 1;
                if (fullScreenIndex >= 0) {
                    countSpinner.setSelection(siteCount - 1);
                    return;
                }
                if (count != siteCount) {
                    siteCount = count;
                    prefs.edit().putInt("site_count", siteCount).apply();
                    createSites();
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        row.addView(countSpinner);
        root.addView(row);

        bulkUrl = new EditText(this);
        bulkUrl.setSingleLine(true);
        bulkUrl.setHint("One URL for all websites");
        root.addView(bulkUrl);

        LinearLayout actions = new LinearLayout(this);

        Button openAll = new Button(this);
        openAll.setText("Open All");
        openAll.setOnClickListener(v -> openBulk());

        Button refresh = new Button(this);
        refresh.setText("Refresh All");
        refresh.setOnClickListener(v -> {
            for (WebView web : webViews) web.reload();
        });

        actions.addView(openAll, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(refresh, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(actions);

        ScrollView scroll = new ScrollView(this);
        gridScroll = scroll;
        grid = new GridLayout(this);
        grid.setColumnCount(1);
        scroll.addView(grid);

        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
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

        saveUrls();

        for (WebView web : webViews) {
            web.stopLoading();
            web.destroy();
        }

        webViews.clear();
        siteCells.clear();
        urlFields.clear();
        grid.removeAllViews();

        int width = getResources().getDisplayMetrics().widthPixels - 14;
        int height = (int)(getResources().getDisplayMetrics().density * 480);
        normalCellWidth = width;
        normalCellHeight = height;

        for (int i = 0; i < siteCount; i++) {
            final int index = i;

            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setPadding(2, 2, 2, 2);
            cell.setBackgroundColor(Color.WHITE);
            siteCells.add(cell);

            TextView label = new TextView(this);
            label.setText("Site " + (i + 1));
            label.setGravity(Gravity.CENTER);
            cell.addView(label);

            EditText url = new EditText(this);
            url.setSingleLine(true);
            url.setTextSize(9);
            url.setHint("URL");
            url.setText(prefs.getString("url_" + i, ""));
            urlFields.add(url);
            cell.addView(url, new LinearLayout.LayoutParams(-1, 35));

            WebView web = new WebView(this);

            if (profilesSupported) {
                try {
                    WebViewCompat.setProfile(web, "site_" + (i + 1));
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
            web.getSettings().setDomStorageEnabled(true);
            web.getSettings().setSupportZoom(true);
            web.getSettings().setBuiltInZoomControls(true);
            web.getSettings().setDisplayZoomControls(false);

            web.setWebViewClient(new WebViewClient());
            webViews.add(web);

            cell.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

            LinearLayout buttons = new LinearLayout(this);

            Button go = new Button(this);
            go.setText("Go");
            go.setTextSize(9);
            go.setOnClickListener(v -> {
                String address = normalize(urlFields.get(index).getText().toString());
                if (address != null) {
                    prefs.edit().putString("url_" + index, address).apply();
                    webViews.get(index).loadUrl(address);
                }
            });

            Button full = new Button(this);
            full.setText("Full");
            full.setTextSize(9);
            full.setOnClickListener(v -> showFull(index));

            buttons.addView(go, new LinearLayout.LayoutParams(0, -2, 1));
            buttons.addView(full, new LinearLayout.LayoutParams(0, -2, 1));
            cell.addView(buttons);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = width;
            params.height = height;
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

    private void openBulk() {
        String address = normalize(bulkUrl.getText().toString());
        if (address == null) return;

        for (int i = 0; i < webViews.size(); i++) {
            urlFields.get(i).setText(address);
            prefs.edit().putString("url_" + i, address).apply();
            webViews.get(i).loadUrl(address);
        }
    }

    private void saveUrls() {
        if (prefs == null) return;
        for (int i = 0; i < urlFields.size(); i++) {
            prefs.edit().putString("url_" + i,
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

        fullBackButton = new Button(this);
        fullBackButton.setText("Back to Equal View");
        fullBackButton.setOnClickListener(v -> returnToGrid());

        fullscreenContainer = new FrameLayout(this);
        fullscreenContainer.setBackgroundColor(Color.BLACK);
        fullscreenContainer.addView(web,
                new FrameLayout.LayoutParams(-1, -1));

        root.addView(fullBackButton,
                new LinearLayout.LayoutParams(-1, -2));
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

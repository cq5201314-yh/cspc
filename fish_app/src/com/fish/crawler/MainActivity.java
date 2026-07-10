package com.fish.crawler;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(0xFF1E88E5);
            window.setNavigationBarColor(0xFF1565C0);
            window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        webView.addJavascriptInterface(this, "FishApp");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @JavascriptInterface
    public void showToast(final String message) {
        handler.post(new ToastRunnable(this, message));
    }

    @JavascriptInterface
    public void downloadFile(final String url, final String filename) {
        if (!isValidHttpUrl(url)) {
            handler.post(new ToastRunnable(this, "无效的下载链接"));
            return;
        }
        handler.post(new DownloadRunnable(this, url, filename));
    }

    @JavascriptInterface
    public void openBrowser(final String url) {
        if (!isValidHttpUrl(url)) {
            handler.post(new ToastRunnable(this, "无效的链接"));
            return;
        }
        handler.post(new OpenUrlRunnable(this, url));
    }

    @JavascriptInterface
    public void copyToClipboard(final String text) {
        handler.post(() -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("url", text));
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private static boolean isValidHttpUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    // Helper class for showing toast
    private static class ToastRunnable implements Runnable {
        private final Activity activity;
        private final String message;
        
        ToastRunnable(Activity activity, String message) {
            this.activity = activity;
            this.message = message;
        }
        
        @Override
        public void run() {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }

    // Helper class for downloading
    private static class DownloadRunnable implements Runnable {
        private final Activity activity;
        private final String url;
        private final String filename;
        
        DownloadRunnable(Activity activity, String url, String filename) {
            this.activity = activity;
            this.url = url;
            this.filename = filename;
        }
        
        @Override
        public void run() {
            try {
                DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                Uri uri = Uri.parse(url);
                
                DownloadManager.Request request = new DownloadManager.Request(uri);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "FishCrawler/" + filename);
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setAllowedOverMetered(true);
                request.setAllowedOverRoaming(true);
                
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null && !cookie.isEmpty()) {
                    request.addRequestHeader("Cookie", cookie);
                }
                
                dm.enqueue(request);
                Toast.makeText(activity, "开始下载: " + filename, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Helper class for opening URL
    private static class OpenUrlRunnable implements Runnable {
        private final Activity activity;
        private final String url;
        
        OpenUrlRunnable(Activity activity, String url) {
            this.activity = activity;
            this.url = url;
        }
        
        @Override
        public void run() {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(activity, "打开失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
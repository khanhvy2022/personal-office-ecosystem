/* -*- tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.libreoffice.androidlib;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * S1 Hardened COWebViewClient:
 * Captures in-process cool:// requests and strictly blocks all external network schemes (http, https, ws, wss)
 * and non-asset file paths.
 */
public class COWebViewClient extends WebViewClient {
    private final MobileSocket mobileSocket;

    public COWebViewClient() {
        super();
        mobileSocket = new MobileSocket();
    }

    public static WebResourceResponse response(int code, String reasonPhrase, InputStream data, Map<String, String> headers) {
        return new WebResourceResponse(null, null, code, reasonPhrase, headers, data);
    }

    public static WebResourceResponse response(int code, String reasonPhrase, InputStream data, long length) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "null");
        headers.put("Content-Length", Long.toString(length));
        return response(code, reasonPhrase, data, headers);
    }

    public static WebResourceResponse response(int code, String reasonPhrase, InputStream data) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "null");
        return response(code, reasonPhrase, data, headers);
    }

    public static WebResourceResponse response(int code, String reasonPhrase) {
        ByteArrayInputStream data = new ByteArrayInputStream(new byte[0]);
        return response(code, reasonPhrase, data, 0);
    }

    /**
     * S1 Security Policy Check: Only allow cool://, file:///android_asset/, data:, and blob: schemes.
     */
    public static boolean isAllowedOfflineUri(@Nullable Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (scheme == null) return false;
        String lower = scheme.toLowerCase(Locale.ROOT);
        if ("cool".equals(lower) || "data".equals(lower) || "blob".equals(lower) || "javascript".equals(lower)) {
            return true;
        }
        if ("file".equals(lower)) {
            String path = uri.getPath();
            return path != null && path.startsWith("/android_asset/");
        }
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        // S1: Prevent any external navigation inside the editor WebView
        Uri url = request != null ? request.getUrl() : null;
        return !isAllowedOfflineUri(url);
    }

    @Nullable
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        Uri url = request.getUrl();
        if (!isAllowedOfflineUri(url)) {
            return response(403, "Blocked by EW Offline Security Policy");
        }
        if (!Objects.equals(url.getScheme(), "cool")) {
            return super.shouldInterceptRequest(view, request);
        }

        String path = url.getPath();
        if (Objects.equals(path, "/cool/media")) {
            return handleMediaRequest(request);
        } else if (path != null && path.startsWith("/cool/mobilesocket/")) {
            return handleMobileSocketRequest(request);
        } else {
            return response(404, "Not Found");
        }
    }

    private WebResourceResponse handleMediaRequest(WebResourceRequest request) {
        String tag = request.getUrl().getQueryParameter("Tag");
        if (tag == null) {
            return response(404, "Not Found");
        }
        String mediaPath = getEmbeddedMediaPath(tag);
        if (mediaPath == null || mediaPath.isEmpty()) {
            return response(404, "Not Found");
        }
        File media = new File(mediaPath);
        if (!media.exists()) {
            return response(404, "Not Found");
        }
        try {
            FileInputStream data = new FileInputStream(media);
            return response(200, "OK", data, media.length());
        } catch (FileNotFoundException e) {
            return response(404, "Not Found");
        }
    }

    private WebResourceResponse handleMobileSocketRequest(WebResourceRequest request) {
        List<String> path = request.getUrl().getPathSegments();
        if (path.size() < 7) {
            return response(400, "Bad Request");
        }
        if (path.get(6).equals("open")) {
            return mobileSocket.open();
        }
        return mobileSocket.write();
    }

    public MobileSocket getMobileSocket() {
        return mobileSocket;
    }

    private native String getEmbeddedMediaPath(String tag);
}

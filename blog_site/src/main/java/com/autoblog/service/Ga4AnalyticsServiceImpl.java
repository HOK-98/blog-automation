package com.autoblog.service;

import com.autoblog.model.ChannelMetric;
import com.autoblog.model.DeviceMetric;
import com.autoblog.model.Ga4Metrics;
import com.autoblog.model.PageViewMetric;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Ga4AnalyticsServiceImpl implements Ga4AnalyticsService {
    // GA4 API는 외부 호출이라 매 페이지 로딩마다 호출하면 대시보드가 느려집니다.
    // 운영 화면에서는 5분 캐시를 사용하고, 필요할 때 수동 새로고침하는 구조가 현실적입니다.
    private static final long CACHE_MILLIS = 5 * 60 * 1000;
    private String propertyId;
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private Ga4Metrics cachedOverview;
    private long overviewCachedAt;
    private List<PageViewMetric> cachedPageViews;
    private long pageViewsCachedAt;

    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    @Override
    public synchronized Ga4Metrics getOverview() {
        try {
            long now = System.currentTimeMillis();
            if (cachedOverview != null && now - overviewCachedAt < CACHE_MILLIS) {
                return cachedOverview;
            }
            // refresh_token으로 access_token을 발급받은 뒤 GA4 Data API runReport를 호출합니다.
            String accessToken = refreshAccessToken();
            ObjectMapper mapper = new ObjectMapper();
            Ga4Metrics metrics = parseOverview(mapper, runReport(accessToken, overviewRequest()));
            metrics.setChannels(parseChannels(mapper, runReport(accessToken, channelRequest())));
            metrics.setDevices(parseDevices(mapper, runReport(accessToken, deviceRequest())));
            cachedOverview = metrics;
            overviewCachedAt = now;
            return metrics;
        } catch (Exception e) {
            throw new IllegalStateException("GA4 통계 조회에 실패했습니다.", e);
        }
    }

    @Override
    public synchronized List<PageViewMetric> getPageViews() {
        try {
            long now = System.currentTimeMillis();
            if (cachedPageViews != null && now - pageViewsCachedAt < CACHE_MILLIS) {
                return cachedPageViews;
            }
            // 글별 조회수는 GA4의 pagePath + screenPageViews 값을 가져와
            // 워드프레스 글 URL path와 매칭하는 방식으로 사용합니다.
            String accessToken = refreshAccessToken();
            List<PageViewMetric> pageViews = parsePageViews(new ObjectMapper(), runReport(accessToken, pageViewsRequest()));
            cachedPageViews = pageViews;
            pageViewsCachedAt = now;
            return pageViews;
        } catch (Exception e) {
            throw new IllegalStateException("GA4 페이지별 조회수 조회에 실패했습니다.", e);
        }
    }

    private String refreshAccessToken() throws Exception {
        URL url = new URL("https://oauth2.googleapis.com/token");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        String body = "client_id=" + encode(trim(clientId))
                + "&client_secret=" + encode(trim(clientSecret))
                + "&refresh_token=" + encode(trim(refreshToken))
                + "&grant_type=refresh_token";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        String response = read(connection);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException("GA4 access_token 발급 실패: HTTP " + status + " / " + response);
        }
        JsonNode json = new ObjectMapper().readTree(response);
        String accessToken = json.path("access_token").asText("");
        if (accessToken == null || accessToken.trim().isEmpty()) {
            throw new IllegalStateException("GA4 access_token 발급 응답에 access_token이 없습니다: " + response);
        }
        return accessToken.trim();
    }

    private String runReport(String accessToken, String body) throws Exception {
        URL url = new URL("https://analyticsdata.googleapis.com/v1beta/properties/" + propertyId + ":runReport");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Content-Type", "application/json");
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        String response = read(connection);
        if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) {
            throw new IllegalStateException("GA4 runReport 실패: HTTP " + connection.getResponseCode() + " / " + response);
        }
        return response;
    }

    private Ga4Metrics parseOverview(ObjectMapper mapper, String raw) throws Exception {
        JsonNode root = mapper.readTree(raw);
        JsonNode rows = root.path("rows");
        Ga4Metrics metrics = new Ga4Metrics();
        if (!rows.isArray() || rows.size() == 0) {
            metrics.setViews(0);
            metrics.setUsers(0);
            metrics.setAverageSessionDuration(0);
            metrics.setBounceRate(0);
            return metrics;
        }
        JsonNode values = rows.get(0).path("metricValues");
        metrics.setViews(values.get(0).path("value").asLong());
        metrics.setUsers(values.get(1).path("value").asLong());
        metrics.setAverageSessionDuration(values.get(2).path("value").asDouble());
        metrics.setBounceRate(values.get(3).path("value").asDouble());
        return metrics;
    }

    private List<ChannelMetric> parseChannels(ObjectMapper mapper, String raw) throws Exception {
        JsonNode rows = mapper.readTree(raw).path("rows");
        List<ChannelMetric> result = new ArrayList<ChannelMetric>();
        if (!rows.isArray()) {
            return result;
        }
        for (JsonNode row : rows) {
            result.add(new ChannelMetric(
                    row.path("dimensionValues").get(0).path("value").asText(),
                    row.path("metricValues").get(0).path("value").asLong()));
        }
        return result;
    }

    private List<DeviceMetric> parseDevices(ObjectMapper mapper, String raw) throws Exception {
        JsonNode rows = mapper.readTree(raw).path("rows");
        List<DeviceMetric> result = new ArrayList<DeviceMetric>();
        if (!rows.isArray()) {
            return result;
        }
        for (JsonNode row : rows) {
            result.add(new DeviceMetric(
                    row.path("dimensionValues").get(0).path("value").asText(),
                    row.path("metricValues").get(0).path("value").asLong()));
        }
        return result;
    }

    private List<PageViewMetric> parsePageViews(ObjectMapper mapper, String raw) throws Exception {
        JsonNode rows = mapper.readTree(raw).path("rows");
        List<PageViewMetric> result = new ArrayList<PageViewMetric>();
        if (!rows.isArray()) {
            return result;
        }
        for (JsonNode row : rows) {
            result.add(new PageViewMetric(
                    row.path("dimensionValues").get(0).path("value").asText(),
                    row.path("metricValues").get(0).path("value").asLong()));
        }
        return result;
    }

    private String overviewRequest() {
        return "{\"dateRanges\":[{\"startDate\":\"30daysAgo\",\"endDate\":\"today\"}],"
                + "\"metrics\":[{\"name\":\"screenPageViews\"},{\"name\":\"activeUsers\"},{\"name\":\"averageSessionDuration\"},{\"name\":\"bounceRate\"}]}";
    }

    private String channelRequest() {
        return "{\"dateRanges\":[{\"startDate\":\"30daysAgo\",\"endDate\":\"today\"}],"
                + "\"dimensions\":[{\"name\":\"sessionDefaultChannelGroup\"}],"
                + "\"metrics\":[{\"name\":\"activeUsers\"}],"
                + "\"limit\":\"5\"}";
    }

    private String deviceRequest() {
        return "{\"dateRanges\":[{\"startDate\":\"30daysAgo\",\"endDate\":\"today\"}],"
                + "\"dimensions\":[{\"name\":\"deviceCategory\"}],"
                + "\"metrics\":[{\"name\":\"activeUsers\"}]}";
    }

    private String pageViewsRequest() {
        return "{\"dateRanges\":[{\"startDate\":\"30daysAgo\",\"endDate\":\"today\"}],"
                + "\"dimensions\":[{\"name\":\"pagePath\"}],"
                + "\"metrics\":[{\"name\":\"screenPageViews\"}],"
                + "\"limit\":\"1000\"}";
    }

    private String read(HttpURLConnection connection) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String encode(String value) throws Exception {
        return URLEncoder.encode(value, "UTF-8");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

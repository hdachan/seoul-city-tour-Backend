package com.example.seoulcitytour.controller;

import com.google.analytics.data.v1beta.*;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.api.gax.core.FixedCredentialsProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final String PROPERTY_ID = "528786578";

    // 하루 단위 캐시
    private final Map<String, Object> cache    = new ConcurrentHashMap<>();
    private final Map<String, String> cacheDate = new ConcurrentHashMap<>();

    private boolean isCacheValid(String key) {
        return cacheDate.containsKey(key) &&
                cacheDate.get(key).equals(LocalDate.now().toString());
    }

    @SuppressWarnings("unchecked")
    private <T> T getCache(String key) { return (T) cache.get(key); }

    private void setCache(String key, Object value) {
        cache.put(key, value);
        cacheDate.put(key, LocalDate.now().toString());
    }

    // 매일 자정 캐시 초기화
    @Scheduled(cron = "0 0 0 * * *")
    public void clearCache() {
        cache.clear();
        cacheDate.clear();
    }

    private BetaAnalyticsDataClient createClient() throws Exception {
        InputStream keyStream = new ClassPathResource("ga4-key.json").getInputStream();
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(keyStream)
                .createScoped("https://www.googleapis.com/auth/analytics.readonly");

        BetaAnalyticsDataSettings settings = BetaAnalyticsDataSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        return BetaAnalyticsDataClient.create(settings);
    }

    // 월별 일별 방문자수
    @GetMapping("/visitors")
    public ResponseEntity<?> getVisitors(
            @RequestParam(defaultValue = "30") int days) {
        String key = "visitors_" + days;
        if (isCacheValid(key)) return ResponseEntity.ok(getCache(key));
        try (var client = createClient()) {
            var request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + PROPERTY_ID)
                    .addDimensions(Dimension.newBuilder().setName("date"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(days + "daysAgo")
                            .setEndDate("today"))
                    .addOrderBys(OrderBy.newBuilder()
                            .setDimension(OrderBy.DimensionOrderBy.newBuilder().setDimensionName("date")))
                    .build();

            var response = client.runReport(request);
            var result   = new ArrayList<Map<String, Object>>();

            for (var row : response.getRowsList()) {
                var map = new LinkedHashMap<String, Object>();
                map.put("date",    row.getDimensionValues(0).getValue());
                map.put("users",   Long.parseLong(row.getMetricValues(0).getValue()));
                map.put("sessions",Long.parseLong(row.getMetricValues(1).getValue()));
                result.add(map);
            }

            setCache(key, result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // 유입 경로
    @GetMapping("/sources")
    public ResponseEntity<?> getSources(
            @RequestParam(defaultValue = "30") int days) {
        String key = "sources_" + days;
        if (isCacheValid(key)) return ResponseEntity.ok(getCache(key));
        try (var client = createClient()) {
            var request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + PROPERTY_ID)
                    .addDimensions(Dimension.newBuilder().setName("sessionDefaultChannelGroup"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(days + "daysAgo")
                            .setEndDate("today"))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder().setMetricName("sessions"))
                            .setDesc(true))
                    .build();

            var response = client.runReport(request);
            var result   = new ArrayList<Map<String, Object>>();

            for (var row : response.getRowsList()) {
                var map = new LinkedHashMap<String, Object>();
                map.put("channel", row.getDimensionValues(0).getValue());
                map.put("sessions",Long.parseLong(row.getMetricValues(0).getValue()));
                map.put("users",   Long.parseLong(row.getMetricValues(1).getValue()));
                result.add(map);
            }

            setCache(key, result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // 국가별 방문자
    @GetMapping("/countries")
    public ResponseEntity<?> getCountries(
            @RequestParam(defaultValue = "30") int days) {
        String key = "countries_" + days;
        if (isCacheValid(key)) return ResponseEntity.ok(getCache(key));
        try (var client = createClient()) {
            var request = RunReportRequest.newBuilder()
                    .setProperty("properties/" + PROPERTY_ID)
                    .addDimensions(Dimension.newBuilder().setName("country"))
                    .addMetrics(Metric.newBuilder().setName("activeUsers"))
                    .addMetrics(Metric.newBuilder().setName("sessions"))
                    .addDateRanges(DateRange.newBuilder()
                            .setStartDate(days + "daysAgo")
                            .setEndDate("today"))
                    .addOrderBys(OrderBy.newBuilder()
                            .setMetric(OrderBy.MetricOrderBy.newBuilder().setMetricName("activeUsers"))
                            .setDesc(true))
                    .setLimit(10)
                    .build();

            var response = client.runReport(request);
            var result   = new ArrayList<Map<String, Object>>();

            for (var row : response.getRowsList()) {
                var map = new LinkedHashMap<String, Object>();
                map.put("country", row.getDimensionValues(0).getValue());
                map.put("users",   Long.parseLong(row.getMetricValues(0).getValue()));
                map.put("sessions",Long.parseLong(row.getMetricValues(1).getValue()));
                result.add(map);
            }

            setCache(key, result);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
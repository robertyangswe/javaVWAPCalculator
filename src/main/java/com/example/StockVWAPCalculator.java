package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class StockVWAPCalculator {
    private static final String STOCK_SYMBOL = "DDOG";
    private static final int CONNECT_TIMEOUT_SECONDS = 30;
    private static final int READ_TIMEOUT_SECONDS = 30;
    private static final int WRITE_TIMEOUT_SECONDS = 30;
    private static final int DAYS_IN_YEAR = 365;
    private static final int SECONDS_IN_DAY = 24 * 60 * 60;
    private static final int YEARS_OF_HISTORY = 5;
    private static final String DATE_FORMAT = "%d-%02d";
    private static final String OUTPUT_FORMAT = "%-10s $%-14.2f %,15d%n";
    private static final String HEADER_FORMAT = "%-10s %-15s %-15s%n";
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            Map<String, MonthlyVWAP> monthlyVWAPs = calculateMonthlyVWAP(STOCK_SYMBOL);
            printResults(monthlyVWAPs);
        } catch (Exception e) {
            System.err.println("Error calculating VWAP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, MonthlyVWAP> calculateMonthlyVWAP(String symbol) throws IOException {
        // Calculate timestamps for date range (1 year)
        long endTime = Instant.now().getEpochSecond();
        long startTime = endTime - (DAYS_IN_YEAR * SECONDS_IN_DAY * YEARS_OF_HISTORY);

        String url = String.format(
            "https://query2.finance.yahoo.com/v8/finance/chart/%s?period1=%d&period2=%d&interval=1d",
            symbol, startTime, endTime
        );

        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .header("Connection", "keep-alive")
            .header("Upgrade-Insecure-Requests", "1")
            .header("Sec-Fetch-Dest", "document")
            .header("Sec-Fetch-Mode", "navigate")
            .header("Sec-Fetch-Site", "none")
            .header("Cache-Control", "max-age=0")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response " + response);
            }

            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            
            // Navigate through Yahoo Finance's JSON structure
            JsonNode result = root.path("chart").path("result").get(0);
            JsonNode timestamps = result.path("timestamp");
            JsonNode indicators = result.path("indicators").path("quote").get(0);
            JsonNode closePrices = indicators.path("close");
            JsonNode volumes = indicators.path("volume");

            if (timestamps.isMissingNode() || closePrices.isMissingNode() || volumes.isMissingNode()) {
                throw new IOException("Missing required data in Yahoo Finance response");
            }

            Map<String, MonthlyVWAP> monthlyVWAPs = new TreeMap<>();

            for (int i = 0; i < timestamps.size(); i++) {
                // Skip any null values in the data
                if (closePrices.get(i).isNull() || volumes.get(i).isNull()) {
                    continue;
                }

                long timestamp = timestamps.get(i).asLong();
                double closePrice = closePrices.get(i).asDouble();
                long volume = volumes.get(i).asLong();

                // Convert timestamp to YYYY-MM format
                LocalDate date = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
                String month = String.format(DATE_FORMAT, date.getYear(), date.getMonthValue());

                monthlyVWAPs.computeIfAbsent(month, k -> new MonthlyVWAP())
                    .addData(closePrice, volume);
            }

            return monthlyVWAPs;
        }
    }

    private static void printResults(Map<String, MonthlyVWAP> monthlyVWAPs) {
        System.out.println("Monthly Volume Weighted Average Price (VWAP) for " + STOCK_SYMBOL);
        System.out.println("=================================================");
        System.out.printf(HEADER_FORMAT, "Month", "VWAP", "Total Volume");
        System.out.println("-------------------------------------------------");

        monthlyVWAPs.forEach((month, vwap) -> {
            System.out.printf(OUTPUT_FORMAT, 
                month, vwap.getVWAP(), vwap.getTotalVolume());
        });
    }

    private static class MonthlyVWAP {
        private double sumPriceVolume = 0.0;
        private long totalVolume = 0;

        public void addData(double price, long volume) {
            sumPriceVolume += price * volume;
            totalVolume += volume;
        }

        public double getVWAP() {
            return totalVolume > 0 ? sumPriceVolume / totalVolume : 0.0;
        }

        public long getTotalVolume() {
            return totalVolume;
        }
    }
} 
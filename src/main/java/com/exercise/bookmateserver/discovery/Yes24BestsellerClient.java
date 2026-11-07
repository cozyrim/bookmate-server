package com.exercise.bookmateserver.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class Yes24BestsellerClient {
    private static final URI ENDPOINT = URI.create("https://apis.yes24.com/v1/category/bestsellerRealtime?categoryId=001&detail=Y");
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final ObjectMapper mapper;
    private final String key;

    public Yes24BestsellerClient(ObjectMapper mapper, @Value("${YES24_API_KEY:}") String key) {
        this.mapper = mapper;
        this.key = key.trim();
    }

    public List<BestsellerResponse.Item> fetch() {
        if (key.isBlank()) throw unavailable();
        var request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(8))
                .header("X-Api-Key", key).GET().build();
        var pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            var response = pending.get(8, TimeUnit.SECONDS);
            if (response.statusCode() != 200) throw unavailable();
            return parse(mapper.readTree(response.body()));
        } catch (IOException | ExecutionException | TimeoutException e) {
            pending.cancel(true);
            throw unavailable();
        } catch (InterruptedException e) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw unavailable();
        }
    }

    static List<BestsellerResponse.Item> parse(JsonNode root) {
        var items = root.path("data").path("items");
        if (!root.path("success").asBoolean() || !items.isArray()) throw unavailable();
        List<BestsellerResponse.Item> result = new ArrayList<>();
        for (JsonNode item : items) {
            if ("Y".equalsIgnoreCase(item.path("adultYn").asText("")) || item.path("title").asText("").isBlank()) continue;
            String id = item.path("itemId").asText("");
            int rank = item.path("sortOrder").asInt();
            if (!id.matches("[0-9]+") || rank <= 0) continue;
            String cover = item.path("cover").asText("");
            // The image and outbound product destination are constrained to the provider.
            if (!cover.startsWith("https://image.yes24.com/")) cover = "";
            int pages = item.path("pages").asInt();
            result.add(new BestsellerResponse.Item(rank, id, item.path("title").asText(""),
                    item.path("author").asText(""), item.path("publisher").asText(""),
                    item.path("isbn13").asText(""), cover, "https://www.yes24.com/product/goods/" + id,
                    pages > 0 ? pages : null, introduction(item.path("contentDetail"))));
        }
        return result.stream().sorted(java.util.Comparator.comparingInt(BestsellerResponse.Item::rank)).limit(20).toList();
    }

    static String introduction(JsonNode detail) {
        String value = plainText(detail.path("bookIntroduction").asText(""));
        return value.isBlank() ? plainText(detail.path("bookSummary").asText("")) : value;
    }

    private static String plainText(String html) {
        // Render provider copy as text only: no remote images, scripts, or web view.
        String bounded = html.length() > 100_000 ? html.substring(0, 100_000) : html;
        String text = bounded
                .replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>", "")
                .replaceAll("(?i)<br\\b[^>]*>|</?(?:p|div|li|h[1-6])\\b[^>]*>", "\n")
                .replaceAll("(?s)<[a-zA-Z/!][^>]*>", "");
        return org.springframework.web.util.HtmlUtils.htmlUnescape(text)
                .replace('\u00a0', ' ')
                .replace("\r\n", "\n")
                .replaceAll("[ \t]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n").trim();
    }

    static ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "베스트셀러를 불러올 수 없어요. 잠시 후 다시 시도해주세요.");
    }
}

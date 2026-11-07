package com.exercise.bookmateserver.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BestsellerServiceTest {
    @Test void sharesFreshResponseAndRefreshesAfterExpiry() {
        var client = mock(Yes24BestsellerClient.class);
        when(client.fetch()).thenReturn(List.of());
        var clock = mock(Clock.class);
        Instant start = Instant.parse("2026-11-08T00:00:00Z");
        when(clock.instant()).thenReturn(start);
        var service = new BestsellerService(client, clock);
        var first = service.realtime();
        assertSame(first, service.realtime());
        verify(client, times(1)).fetch();
        when(clock.instant()).thenReturn(start.plusSeconds(61));
        assertNotSame(first, service.realtime());
        verify(client, times(2)).fetch();
    }
    @Test void failureHasCooldownAndNeverReturnsExpiredRankings() {
        var client = mock(Yes24BestsellerClient.class);
        when(client.fetch()).thenThrow(Yes24BestsellerClient.unavailable());
        var clock = Clock.fixed(Instant.parse("2026-11-08T00:00:00Z"), ZoneOffset.UTC);
        var service = new BestsellerService(client, clock);
        assertThrows(RuntimeException.class, service::realtime);
        assertThrows(RuntimeException.class, service::realtime);
        verify(client, times(1)).fetch();
    }
    @Test void parsesRanksAndRestrictsExternalAssets() throws Exception {
        var json = new ObjectMapper().readTree("""
            {"success":true,"data":{"items":[
              {"itemId":123,"sortOrder":2,"title":"Book","author":"Writer","pages":"128","cover":"https://evil.test/a"},
              {"itemId":456,"sortOrder":1,"title":"Other","adultYn":"N","isbn13":"9780000000000","contentDetail":{"bookIntroduction":"<p>소개</p>"}},
              {"itemId":789,"sortOrder":3,"title":"Adult","adultYn":"Y"},
              {"itemId":900,"sortOrder":4,"title":null,"author":null}
            ]}}
            """);
        var items = Yes24BestsellerClient.parse(json);
        assertEquals(2, items.size());
        assertEquals(1, items.get(0).rank());
        assertEquals("소개", items.get(0).introduction());
        assertEquals("", items.get(1).introduction());
        assertEquals("", items.get(1).cover());
        assertEquals(128, items.get(1).totalPages());
        assertEquals("https://www.yes24.com/product/goods/123", items.get(1).productUrl());
    }
    @Test void slowRefreshDoesNotBlockConcurrentRequests() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var client = mock(Yes24BestsellerClient.class);
        when(client.fetch()).thenAnswer(call -> { entered.countDown(); release.await(5, java.util.concurrent.TimeUnit.SECONDS); return List.of(); });
        var service = new BestsellerService(client);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var refresh = executor.submit(service::realtime);
            try {
                assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
                var follower = executor.submit(() -> assertThrows(RuntimeException.class, service::realtime));
                follower.get(1, java.util.concurrent.TimeUnit.SECONDS);
            } finally { release.countDown(); }
            assertNotNull(refresh.get(2, java.util.concurrent.TimeUnit.SECONDS));
            assertNotNull(service.realtime());
            verify(client, times(1)).fetch();
        }
    }
    @Test void introductionRemovesMarkupAndUsesSummaryWhenMissing() throws Exception {
        var mapper = new ObjectMapper();
        var detail = mapper.createObjectNode();
        detail.put("bookIntroduction", "<b>책 &amp; 독서</b><br>첫 문장<p>다음 문단</p><script>bad()</script>");
        assertEquals("책 & 독서\n첫 문장\n다음 문단", Yes24BestsellerClient.introduction(detail));
        detail.put("bookIntroduction", "&lt;The Little Prince&gt; &amp; 독서");
        assertEquals("<The Little Prince> & 독서", Yes24BestsellerClient.introduction(detail));
        detail.put("bookIntroduction", "첫 줄<br class='line'>다음 줄<br/>마지막 줄");
        assertEquals("첫 줄\n다음 줄\n마지막 줄", Yes24BestsellerClient.introduction(detail));
        detail.putNull("bookIntroduction");
        detail.put("bookSummary", "대체 소개 &quot;문장&quot;");
        assertEquals("대체 소개 \"문장\"", Yes24BestsellerClient.introduction(detail));
        assertEquals("", Yes24BestsellerClient.introduction(mapper.createObjectNode()));
    }
    @Test void rejectsProviderErrorAndMissingKey() throws Exception {
        assertThrows(RuntimeException.class, () -> Yes24BestsellerClient.parse(new ObjectMapper().readTree("{\"success\":false}")));
        assertThrows(RuntimeException.class, () -> new Yes24BestsellerClient(new ObjectMapper(), "").fetch());
    }
}

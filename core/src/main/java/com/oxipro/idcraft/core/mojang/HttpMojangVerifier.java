package com.oxipro.idcraft.core.mojang;

import com.google.gson.Gson;
import com.oxipro.idcraft.api.mojang.IMojangVerifier;
import com.oxipro.idcraft.api.mojang.MojangProfile;
import com.oxipro.idcraft.api.mojang.PremiumLookupResult;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class HttpMojangVerifier implements IMojangVerifier {

    private static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    private static class MojangApiResponse {
        String id;
        String name;
    }

    private static class CacheEntry {
        final PremiumLookupResult result;
        final Instant expiresAt;

        CacheEntry(PremiumLookupResult result, Instant expiresAt) {
            this.result = result;
            this.expiresAt = expiresAt;
        }
    }

    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private final boolean cacheEnabled;
    private final Duration positiveTtl;
    private final Duration negativeTtl;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public HttpMojangVerifier() {
        this(true);
    }

    public HttpMojangVerifier(boolean cacheEnabled) {
        this(cacheEnabled, Duration.ofMinutes(30), Duration.ofMinutes(5));
    }

    public HttpMojangVerifier(boolean cacheEnabled, Duration positiveTtl, Duration negativeTtl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.cacheEnabled = cacheEnabled;
        this.positiveTtl = positiveTtl;
        this.negativeTtl = negativeTtl;
    }

    @Override
    public PremiumLookupResult lookup(String username) {
        if (username == null || !VALID_USERNAME.matcher(username).matches()) {
            // Invalid name is definitive not-premium (cannot be a Mojang account)
            return PremiumLookupResult.notPremium();
        }

        String key = username.toLowerCase();

        if (cacheEnabled) {
            CacheEntry cached = cache.get(key);
            if (cached != null && cached.expiresAt.isAfter(Instant.now())) {
                return cached.result;
            }
        }

        PremiumLookupResult result = fetchFromMojang(username);

        // Never cache UNKNOWN — only definitive answers
        if (cacheEnabled && !result.isUnknown()) {
            Duration ttl;
            if (result.isPremium()) {
                ttl = positiveTtl;
            } else {
                ttl = negativeTtl;
            }
            cache.put(key, new CacheEntry(result, Instant.now().plus(ttl)));
        }

        return result;
    }

    private PremiumLookupResult fetchFromMojang(String username) {
        try {
            String encoded = URLEncoder.encode(username, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + encoded))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int code = response.statusCode();
            if (code == 200) {
                MojangApiResponse parsed = gson.fromJson(response.body(), MojangApiResponse.class);
                if (parsed == null || parsed.id == null) {
                    return PremiumLookupResult.unknown();
                }
                MojangProfile profile = new MojangProfile(dashedUuid(parsed.id), parsed.name);
                return PremiumLookupResult.premium(profile);
            }
            if (code == 404 || code == 204) {
                return PremiumLookupResult.notPremium();
            }
            // 429, 5xx, unexpected → UNKNOWN (do not treat as free name)
            return PremiumLookupResult.unknown();
        } catch (Exception e) {
            return PremiumLookupResult.unknown();
        }
    }

    private static UUID dashedUuid(String raw32) {
        String dashed = raw32.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5");
        return UUID.fromString(dashed);
    }
}

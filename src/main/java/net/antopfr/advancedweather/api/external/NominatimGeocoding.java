package net.antopfr.advancedweather.api.external;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.antopfr.advancedweather.util.AWLogger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class NominatimGeocoding {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String URL_TEMPLATE =
            "https://nominatim.openstreetmap.org/reverse?lat=%s&lon=%s&format=json&zoom=18&addressdetails=1";

    private static String cachedLocation = "Unknown";

    public static String getCachedLocation() {
        return cachedLocation;
    }

    public static void invalidateCache() {
        cachedLocation = "Loading...";
    }

    public static CompletableFuture<String> fetchLocationNameAsync(double lat, double lon) {
        String url = String.format(URL_TEMPLATE,
                String.valueOf(lat).replace(',', '.'),
                String.valueOf(lon).replace(',', '.'));

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .header("User-Agent", "AdvancedWeatherMod/0.1.3 (advancedweather@gmail.com)")
                        .GET()
                        .build();

                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    AWLogger.AW.warn("[AdvancedWeather] Nominatim returned HTTP {}", response.statusCode());
                    return null;
                }

                String result = parseLocation(response.body());
                if (result != null) {
                    cachedLocation = result;
                }
                return result;

            } catch (Exception e) {
                AWLogger.AW.warn("[AdvancedWeather] Nominatim unreachable: {}", e.getMessage());
                return null;
            }
        });
    }

    private static String parseLocation(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (!root.has("address")) return "Unknown Location";

            JsonObject address = root.getAsJsonObject("address");

            String place = "Unknown";
            if (address.has("village")) {
                place = address.get("village").getAsString();
            } else if (address.has("town")) {
                place = address.get("town").getAsString();
            } else if (address.has("city")) {
                place = address.get("city").getAsString();
            } else if (address.has("municipality")) {
                place = address.get("municipality").getAsString();
            }

            String country = "Unknown Country";
            if (address.has("country")) {
                country = address.get("country").getAsString();
            }

            return place + ", " + country;

        } catch (Exception e) {
            AWLogger.AW.warn("[AdvancedWeather] Failed to parse Nominatim response");
            return "Parsing Error";
        }
    }
}

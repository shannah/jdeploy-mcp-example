package org.acme;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkus.qute.Qute;
import jakarta.inject.Inject;

public class WeatherTools {

    @Inject
    @RestClient
    WeatherClient weatherClient;

    @Tool(description = "Get weather alerts for a US state.")
    String getAlerts(@ToolArg(description = "Two-letter US state code (e.g. CA, NY)") String state) {
        Alerts alerts = weatherClient.getAlerts(state);
        return formatAlerts(alerts);
    }

    @Tool(description = "Get weather forecast for a location.")
    String getForecast(
            @ToolArg(description = "Latitude of the location") double latitude,
            @ToolArg(description = "Longitude of the location") double longitude) {
        // First get the grid point info for this location
        var points = weatherClient.getPoints(latitude, longitude);

        // Extract the grid data from the points response
        var properties = points.getJsonObject("properties");
        String office = properties.getString("gridId");
        int gridX = properties.getInt("gridX");
        int gridY = properties.getInt("gridY");

        // Get the forecast using the grid data
        Forecast forecast = weatherClient.getForecast(office, gridX, gridY);
        return formatForecast(forecast);
    }

    private String formatAlerts(Alerts alerts) {
        if (alerts.features() == null || alerts.features().isEmpty()) {
            return "No active alerts for this state.";
        }
        return alerts.features().stream()
                .map(feature -> Qute.fmt("""
                        Event: {p.event}
                        Area: {p.areaDesc}
                        Headline: {p.headline}
                        Description: {p.description}
                        """, Map.of("p", feature.properties())).toString())
                .collect(Collectors.joining("\n---\n"));
    }

    private String formatForecast(Forecast forecast) {
        return forecast.properties().periods().stream()
                .map(period -> Qute.fmt("""
                        {p.name}:
                        Temperature: {p.temperature}{p.temperatureUnit}
                        Wind: {p.windSpeed} {p.windDirection}
                        Forecast: {p.detailedForecast}
                        """, Map.of("p", period)).toString())
                .collect(Collectors.joining("\n---\n"));
    }
}

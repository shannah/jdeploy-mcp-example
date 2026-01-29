package org.acme;

import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkiverse.mcp.server.Tool;
import io.quarkiverse.mcp.server.ToolArg;
import io.quarkus.qute.Qute;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

public class WeatherTools {

    @Inject
    @RestClient
    WeatherClient weatherClient;

    @Tool(description = "Get weather alerts for a US state. Only works for US states.")
    String getAlerts(@ToolArg(description = "Two-letter US state code (e.g. CA, NY)") String state) {
        try {
            Alerts alerts = weatherClient.getAlerts(state);
            return formatAlerts(alerts);
        } catch (WebApplicationException e) {
            return "Error fetching alerts: " + e.getMessage() +
                   ". Make sure the state code is a valid US state (e.g. CA, NY, TX).";
        } catch (Exception e) {
            return "Error fetching alerts: " + e.getMessage();
        }
    }

    @Tool(description = "Get weather forecast for a location. Only works for US locations (uses the National Weather Service API).")
    String getForecast(
            @ToolArg(description = "Latitude of the location (must be in the US)") double latitude,
            @ToolArg(description = "Longitude of the location (must be in the US)") double longitude) {
        try {
            // First get the grid point info for this location
            var points = weatherClient.getPoints(latitude, longitude);

            // Check if we got a valid response
            if (points == null || !points.containsKey("properties")) {
                return "Error: Unable to get forecast data. The location may be outside the US coverage area.";
            }

            // Extract the grid data from the points response
            var properties = points.getJsonObject("properties");
            if (properties == null) {
                return "Error: Unable to get forecast data. The location may be outside the US coverage area.";
            }

            String office = properties.getString("gridId");
            int gridX = properties.getInt("gridX");
            int gridY = properties.getInt("gridY");

            // Get the forecast using the grid data
            Forecast forecast = weatherClient.getForecast(office, gridX, gridY);
            return formatForecast(forecast);
        } catch (WebApplicationException e) {
            int status = e.getResponse().getStatus();
            if (status == 404) {
                return "Error: Location not found. The National Weather Service API only covers US locations. " +
                       "Coordinates (" + latitude + ", " + longitude + ") may be outside the US.";
            }
            return "Error fetching forecast: " + e.getMessage();
        } catch (Exception e) {
            return "Error fetching forecast: " + e.getMessage() +
                   ". Note: This service only works for US locations.";
        }
    }

    private String formatAlerts(Alerts alerts) {
        if (alerts == null || alerts.features() == null || alerts.features().isEmpty()) {
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
        if (forecast == null || forecast.properties() == null || forecast.properties().periods() == null) {
            return "No forecast data available for this location.";
        }
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

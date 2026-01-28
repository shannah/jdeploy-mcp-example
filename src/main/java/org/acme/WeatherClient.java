package org.acme;

import jakarta.json.JsonObject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.RestPath;

@RegisterRestClient(baseUri = "https://api.weather.gov")
@Produces(MediaType.APPLICATION_JSON)
public interface WeatherClient {

    @GET
    @Path("/alerts/active/area/{state}")
    Alerts getAlerts(@RestPath String state);

    @GET
    @Path("/points/{latitude},{longitude}")
    JsonObject getPoints(@RestPath double latitude, @RestPath double longitude);

    @GET
    @Path("/gridpoints/{office}/{gridX},{gridY}/forecast")
    Forecast getForecast(@RestPath String office, @RestPath int gridX, @RestPath int gridY);
}

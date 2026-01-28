package org.acme;

import java.util.List;

public record Alerts(List<Feature> features) {

    public record Feature(Properties properties) {}

    public record Properties(String event, String areaDesc, String headline, String description) {}
}

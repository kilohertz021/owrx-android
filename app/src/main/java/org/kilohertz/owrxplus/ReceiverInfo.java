package org.kilohertz.owrxplus;

public class ReceiverInfo {
    public final String name;
    public final String url;
    public final String locator;
    public final String country;
    public final String qth;
    public final double latitude;
    public final double longitude;

    public ReceiverInfo(String name, String url, String locator, String country, String qth,
                        double latitude, double longitude) {
        this.name = name;
        this.url = url;
        this.locator = locator;
        this.country = country;
        this.qth = qth;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String subtitle() {
        StringBuilder builder = new StringBuilder();
        if (country.length() > 0) {
            builder.append(country);
        }
        if (qth.length() > 0) {
            if (builder.length() > 0) {
                builder.append(": ");
            }
            builder.append(qth);
        }
        if (locator.length() > 0) {
            if (builder.length() > 0) {
                builder.append("  ");
            }
            builder.append(locator);
        }
        return builder.toString();
    }
}

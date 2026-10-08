package com.flock.urja.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalGeo {
    private double latitude;
    private double longitude;
}

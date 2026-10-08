package com.flock.urja.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalGeoEnvelope {
    private PortalGeo data;
}

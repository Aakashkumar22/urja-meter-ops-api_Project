package com.flock.urja.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalEnergyEnvelope {
    private List<PortalEnergyReading> data;
}

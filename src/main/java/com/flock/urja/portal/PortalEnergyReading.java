package com.flock.urja.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalEnergyReading {
    private String timestamp;   // dd/MM/yyyy HH:mm, no timezone
    private String kwh;         // string-encoded number
    private String kvah;
    private String voltR;
}

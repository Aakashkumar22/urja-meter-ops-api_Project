package com.flock.urja.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalMeter {
    private String meterId;
    private String serialNo;
    private String make;
    private String phaseType;
    private String installStatus;
    private String dtCode;
}

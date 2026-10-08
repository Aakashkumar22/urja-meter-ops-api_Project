package com.flock.urja.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterDto {
    private String id;
    private String serialNo;
    private String make;
    private String phaseType;
    private String installStatus;
    private String dtCode;
}

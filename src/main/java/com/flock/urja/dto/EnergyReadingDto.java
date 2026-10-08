package com.flock.urja.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnergyReadingDto {
    private String timestamp;   // ISO-8601 UTC
    private double kwh;
    private double kvah;
    private int voltR;
}

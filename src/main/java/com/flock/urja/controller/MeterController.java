package com.flock.urja.controller;

import com.flock.urja.dto.EnergyReadingDto;
import com.flock.urja.dto.GeoDto;
import com.flock.urja.dto.MeterDto;
import com.flock.urja.service.MeterService;
import com.flock.urja.service.PagedResponse;
import com.flock.urja.service.PaginatedMeterService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/meters")
@RequiredArgsConstructor
public class MeterController {

    private final MeterService service;


    private final PaginatedMeterService paginatedService;




    @Operation(summary = "List meters (paginated, 1-indexed pages)")
    @GetMapping
    public List<MeterDto> list(@RequestParam String q,@RequestParam  int page) {
        return service.listMeters(q,page);
    }

    @Operation(summary = "Get meter GPS coordinates")
    @GetMapping("/{meterId}/geo")
    public GeoDto geo(@PathVariable String meterId) {
        return service.getMeterGeo(meterId);
    }

    @Operation(summary = "Get meter energy readings (fixed rolling window)")
    @GetMapping("/{meterId}/energy")
    public List<EnergyReadingDto> energy(@PathVariable String meterId) {
        return service.getMeterEnergy(meterId);
    }
}

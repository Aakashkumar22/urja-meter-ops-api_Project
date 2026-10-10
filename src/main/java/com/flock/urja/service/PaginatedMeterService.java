package com.flock.urja.service;


import com.flock.urja.client.PortalDataClient;
import com.flock.urja.dto.EnergyReadingDto;
import com.flock.urja.dto.GeoDto;
import com.flock.urja.dto.MeterDto;
import com.flock.urja.service.PagedResponse;
import com.flock.urja.portal.PortalEnergyEnvelope;
import com.flock.urja.portal.PortalEnergyReading;
import com.flock.urja.portal.PortalGeoEnvelope;
import com.flock.urja.portal.PortalMeter;
import com.flock.urja.portal.PortalPage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaginatedMeterService {

    private static final ZoneId PORTAL_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter PORTAL_TS_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PortalDataClient portal;

    public PagedResponse<MeterDto> listMeters2( String q,int page) {
        String encodedQ = URLEncoder.encode(q == null ? "" : q, StandardCharsets.UTF_8);
        String path = "/portal/meters/search?q=" + encodedQ + "&page=" + page;

        PortalPage<PortalMeter> raw = portal.get(path,
                new ParameterizedTypeReference<PortalPage<PortalMeter>>() {});

        List<MeterDto> items = (raw == null || raw.getData() == null)
                ? Collections.emptyList()
                : raw.getData().stream().map(this::toDto).collect(Collectors.toList());

        long total = raw != null ? raw.getTotal() : 0;
        int pageSize = raw != null ? raw.getPageSize() : 0;
        int currentPage = raw != null ? raw.getPage() : page;

        return new PagedResponse<>(
                items,
                new PagedResponse.PaginationInfo(total, currentPage, pageSize)
        );
    }

    public GeoDto getMeterGeo(String meterId) {
        PortalGeoEnvelope env = portal.get(
                "/portal/meters/" + meterId + "/geo",
                PortalGeoEnvelope.class);
        if (env == null || env.getData() == null) {
            throw new IllegalArgumentException("No geo data for meter " + meterId);
        }
        return new GeoDto(env.getData().getLatitude(), env.getData().getLongitude());
    }

    public List<EnergyReadingDto> getMeterEnergy(String meterId) {
        PortalEnergyEnvelope env = portal.get(
                "/portal/meters/" + meterId + "/energy",
                PortalEnergyEnvelope.class);
        if (env == null || env.getData() == null) return Collections.emptyList();
        return env.getData().stream().map(this::toEnergyDto).collect(Collectors.toList());
    }

    private MeterDto toDto(PortalMeter p) {
        return new MeterDto(p.getMeterId(), p.getSerialNo(), p.getMake(),
                p.getPhaseType(), p.getInstallStatus(), p.getDtCode());
    }

    private EnergyReadingDto toEnergyDto(PortalEnergyReading r) {
        LocalDateTime ldt = LocalDateTime.parse(r.getTimestamp(), PORTAL_TS_FMT);
        Instant utc = ldt.atZone(PORTAL_ZONE).toInstant();
        return new EnergyReadingDto(
                utc.toString(),
                Double.parseDouble(r.getKwh()),
                Double.parseDouble(r.getKvah()),
                Integer.parseInt(r.getVoltR())
        );
    }
}

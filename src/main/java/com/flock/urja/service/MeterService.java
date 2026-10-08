package com.flock.urja.service;

import com.flock.urja.client.PortalDataClient;
import com.flock.urja.dto.EnergyReadingDto;
import com.flock.urja.dto.GeoDto;
import com.flock.urja.dto.MeterDto;
import com.flock.urja.portal.PortalEnergyEnvelope;
import com.flock.urja.portal.PortalEnergyReading;
import com.flock.urja.portal.PortalGeoEnvelope;
import com.flock.urja.portal.PortalMeter;
import com.flock.urja.portal.PortalPage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeterService {

    private static final ZoneId PORTAL_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter PORTAL_TS_FMT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PortalDataClient portal;

    public List<MeterDto> listMeters(int page) {
        String path = "/portal/meters/search?q=&page=" + page;
        PortalPage<PortalMeter> raw = portal.get(path,
            new ParameterizedTypeReference<PortalPage<PortalMeter>>() {});
        if (raw == null || raw.getData() == null) return Collections.emptyList();
        return raw.getData().stream().map(this::toDto).collect(Collectors.toList());
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

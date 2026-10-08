package com.flock.urja.portal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PortalPage<T> {
    private List<T> data;
    private int page;
    private int pageSize;
    private long total;
}

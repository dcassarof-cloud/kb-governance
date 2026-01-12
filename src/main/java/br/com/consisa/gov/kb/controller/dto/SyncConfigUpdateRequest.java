package br.com.consisa.gov.kb.controller.dto;

import br.com.consisa.gov.kb.domain.SyncMode;

public class SyncConfigUpdateRequest {
    public boolean enabled;
    public SyncMode mode;
    public int intervalMinutes;
    public int daysBack;
}

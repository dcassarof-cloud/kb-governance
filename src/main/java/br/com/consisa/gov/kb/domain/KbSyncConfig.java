package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "kb_sync_config")
public class  KbSyncConfig {

    @Id
    private Long id = 1L; // singleton

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, length = 30)
    private SyncMode mode = SyncMode.DELTA;

    @Column(name = "interval_minutes", nullable = false)
    private int intervalMinutes = 60;

    @Column(name = "days_back", nullable = false)
    private int daysBack = 2;

    @Column(name = "last_started_at")
    private OffsetDateTime lastStartedAt;

    @Column(name = "last_finished_at")
    private OffsetDateTime lastFinishedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist @PreUpdate
    public void touch() {
        updatedAt = OffsetDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public SyncMode getMode() { return mode; }
    public void setMode(SyncMode mode) { this.mode = mode; }

    public int getIntervalMinutes() { return intervalMinutes; }
    public void setIntervalMinutes(int intervalMinutes) { this.intervalMinutes = intervalMinutes; }

    public int getDaysBack() { return daysBack; }
    public void setDaysBack(int daysBack) { this.daysBack = daysBack; }

    public OffsetDateTime getLastStartedAt() { return lastStartedAt; }
    public void setLastStartedAt(OffsetDateTime lastStartedAt) { this.lastStartedAt = lastStartedAt; }

    public OffsetDateTime getLastFinishedAt() { return lastFinishedAt; }
    public void setLastFinishedAt(OffsetDateTime lastFinishedAt) { this.lastFinishedAt = lastFinishedAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

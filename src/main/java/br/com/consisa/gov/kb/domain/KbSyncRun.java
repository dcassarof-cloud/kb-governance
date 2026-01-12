package br.com.consisa.gov.kb.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "kb_sync_run")
public class KbSyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SyncMode mode;

    @Column(name = "days_back")
    private Integer daysBack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncRunStatus status;

    @Column(name = "synced_count", nullable = false)
    private int syncedCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "skipped_count", nullable = false)
    private int skippedCount;

    @Column(name = "not_found_count", nullable = false)
    private int notFoundCount;

    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(length = 400)
    private String note;

    // getters/setters
    public Long getId() { return id; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }
    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public SyncMode getMode() { return mode; }
    public void setMode(SyncMode mode) { this.mode = mode; }
    public Integer getDaysBack() { return daysBack; }
    public void setDaysBack(Integer daysBack) { this.daysBack = daysBack; }
    public SyncRunStatus getStatus() { return status; }
    public void setStatus(SyncRunStatus status) { this.status = status; }
    public int getSyncedCount() { return syncedCount; }
    public void setSyncedCount(int syncedCount) { this.syncedCount = syncedCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getSkippedCount() { return skippedCount; }
    public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
    public int getNotFoundCount() { return notFoundCount; }
    public void setNotFoundCount(int notFoundCount) { this.notFoundCount = notFoundCount; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}

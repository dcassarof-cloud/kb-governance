package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskActionDto;
import br.com.consisa.gov.kb.client.movidesk.MovideskClient;
import br.com.consisa.gov.kb.client.movidesk.MovideskTicketResponse;
import br.com.consisa.gov.kb.domain.FaqCluster;
import br.com.consisa.gov.kb.domain.FaqClusterTicket;
import br.com.consisa.gov.kb.domain.JobRun;
import br.com.consisa.gov.kb.domain.SupportTicket;
import br.com.consisa.gov.kb.domain.SupportTicketMessage;
import br.com.consisa.gov.kb.repository.FaqClusterRepository;
import br.com.consisa.gov.kb.repository.FaqClusterTicketRepository;
import br.com.consisa.gov.kb.repository.JobRunRepository;
import br.com.consisa.gov.kb.repository.SupportTicketMessageRepository;
import br.com.consisa.gov.kb.repository.SupportTicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class SupportImportService {

    private static final Logger log = LoggerFactory.getLogger(SupportImportService.class);

    private final MovideskClient movideskClient;
    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final FaqClusterRepository clusterRepository;
    private final FaqClusterTicketRepository clusterTicketRepository;
    private final RecurrenceService recurrenceService;
    private final SupportNormalizationService normalizationService;
    private final JobRunRepository jobRunRepository;
    private final ObjectMapper objectMapper;

    public SupportImportService(
            MovideskClient movideskClient,
            SupportTicketRepository ticketRepository,
            SupportTicketMessageRepository messageRepository,
            FaqClusterRepository clusterRepository,
            FaqClusterTicketRepository clusterTicketRepository,
            RecurrenceService recurrenceService,
            SupportNormalizationService normalizationService,
            JobRunRepository jobRunRepository,
            ObjectMapper objectMapper
    ) {
        this.movideskClient = movideskClient;
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.clusterRepository = clusterRepository;
        this.clusterTicketRepository = clusterTicketRepository;
        this.recurrenceService = recurrenceService;
        this.normalizationService = normalizationService;
        this.jobRunRepository = jobRunRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportResult runImport(OffsetDateTime start, OffsetDateTime end) {
        JobRun jobRun = new JobRun();
        jobRun.setJobName("support-import");
        jobRun.setStatus("RUNNING");
        jobRun.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        jobRunRepository.save(jobRun);

        int ticketsCreated = 0;
        int ticketsUpdated = 0;
        int messagesCreated = 0;

        try {
            List<MovideskTicketResponse> tickets = movideskClient.searchTickets(start, end);
            for (MovideskTicketResponse ticket : tickets) {
                SupportTicket saved = upsertTicket(ticket);
                if (saved.getCreatedAt().equals(saved.getUpdatedAt())) {
                    ticketsCreated++;
                } else {
                    ticketsUpdated++;
                }
                messagesCreated += saveMessages(saved, ticket.getActions());
                updateClusters(saved, ticket);
            }

            recurrenceService.evaluateRules();

            jobRun.setStatus("SUCCESS");
        } catch (Exception ex) {
            jobRun.setStatus("FAILED");
            ObjectNode details = objectMapper.createObjectNode();
            details.put("error", ex.getMessage());
            jobRun.setDetailsJson(details);
            throw ex;
        } finally {
            jobRun.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
            jobRunRepository.save(jobRun);
        }

        return new ImportResult(ticketsCreated, ticketsUpdated, messagesCreated);
    }

    private SupportTicket upsertTicket(MovideskTicketResponse ticket) {
        SupportTicket entity = ticketRepository.findByExternalTicketId(ticket.getId())
                .orElseGet(SupportTicket::new);
        entity.setExternalTicketId(ticket.getId());
        entity.setProtocol(ticket.getProtocol());
        entity.setSubject(ticket.getSubject());
        entity.setStatus(ticket.getStatus());
        entity.setOwnerTeam(ticket.getOwnerTeam());
        entity.setRequester(ticket.getClients() != null && !ticket.getClients().isEmpty()
                ? ticket.getClients().get(0).getBusinessName()
                : null);
        if (ticket.getCreatedDate() != null) {
            entity.setOriginCreatedAt(ticket.getCreatedDate().atOffset(ZoneOffset.UTC));
        }
        if (ticket.getLastUpdate() != null) {
            entity.setOriginUpdatedAt(ticket.getLastUpdate().atOffset(ZoneOffset.UTC));
            entity.setLastMessageAt(ticket.getLastUpdate().atOffset(ZoneOffset.UTC));
        }
        return ticketRepository.save(entity);
    }

    private int saveMessages(SupportTicket ticket, List<MovideskActionDto> actions) {
        if (actions == null || actions.isEmpty()) {
            return 0;
        }
        int created = 0;
        int index = 0;
        for (MovideskActionDto action : actions) {
            String key = ticket.getExternalTicketId() + ":" + index + ":" + Integer.toHexString(Objects.hash(action.getDescription(), action.getHtmlDescription()));
            index++;
            if (messageRepository.findByExternalMessageKey(key).isPresent()) {
                continue;
            }
            SupportTicketMessage message = new SupportTicketMessage();
            message.setTicketId(ticket.getId());
            message.setDirection(resolveDirection(action));
            message.setAuthor(action.getCreatedBy() != null ? action.getCreatedBy().getBusinessName() : null);
            message.setContent(action.getDescription());
            message.setContentHtml(action.getHtmlDescription());
            message.setExternalMessageKey(key);
            messageRepository.save(message);
            created++;
        }
        return created;
    }

    private String resolveDirection(MovideskActionDto action) {
        Integer type = action.getType();
        if (type != null && type == 2) {
            return "OUT";
        }
        return "IN";
    }

    private void updateClusters(SupportTicket ticket, MovideskTicketResponse source) {
        StringBuilder sb = new StringBuilder();
        if (source.getSubject() != null) {
            sb.append(source.getSubject()).append(" ");
        }
        if (source.getActions() != null) {
            for (MovideskActionDto action : source.getActions()) {
                if (action.getDescription() != null) {
                    sb.append(action.getDescription()).append(" ");
                }
            }
        }
        String normalized = normalizationService.normalize(sb.toString());
        if (normalized.isBlank()) {
            return;
        }
        String fingerprint = normalizationService.fingerprint(normalized);

        FaqCluster cluster = clusterRepository.findByFingerprint(fingerprint)
                .orElseGet(FaqCluster::new);
        if (cluster.getId() == null) {
            cluster.setFingerprint(fingerprint);
            cluster.setNormalizedText(normalized);
            cluster.setSampleText(truncate(sb.toString(), 400));
            cluster.setTicketCount(0);
        }
        clusterRepository.save(cluster);

        if (!clusterTicketRepository.existsByClusterIdAndTicketId(cluster.getId(), ticket.getId())) {
            FaqClusterTicket link = new FaqClusterTicket();
            link.setClusterId(cluster.getId());
            link.setTicketId(ticket.getId());
            clusterTicketRepository.save(link);
            int count = (int) clusterTicketRepository.countByClusterId(cluster.getId());
            cluster.setTicketCount(count);
            clusterRepository.save(cluster);
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    public record ImportResult(int ticketsCreated, int ticketsUpdated, int messagesCreated) {
    }
}

package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.client.movidesk.MovideskClient;
import br.com.consisa.gov.kb.client.movidesk.MovideskTicketRequest;
import br.com.consisa.gov.kb.client.movidesk.MovideskTicketResponse;
import br.com.consisa.gov.kb.domain.DetectedNeed;
import br.com.consisa.gov.kb.domain.FaqCluster;
import br.com.consisa.gov.kb.domain.RecurrenceRule;
import br.com.consisa.gov.kb.repository.DetectedNeedRepository;
import br.com.consisa.gov.kb.repository.FaqClusterRepository;
import br.com.consisa.gov.kb.repository.RecurrenceRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class NeedService {

    private static final Logger log = LoggerFactory.getLogger(NeedService.class);

    private static final String TASK_STATUS_CREATED = "CREATED";

    private final DetectedNeedRepository needRepository;
    private final FaqClusterRepository clusterRepository;
    private final RecurrenceRuleRepository ruleRepository;
    private final MovideskTicketService movideskTicketService;
    private final MovideskClient movideskClient;

    @Value("${movidesk.ticket.service:Base de Conhecimento}")
    private String ticketService;

    @Value("${movidesk.ticket.default-team:ERP - EMPRESARIAL}")
    private String defaultTeam;

    @Value("${movidesk.ticket.client-id}")
    private String defaultClientId;

    public NeedService(
            DetectedNeedRepository needRepository,
            FaqClusterRepository clusterRepository,
            RecurrenceRuleRepository ruleRepository,
            MovideskTicketService movideskTicketService,
            MovideskClient movideskClient
    ) {
        this.needRepository = needRepository;
        this.clusterRepository = clusterRepository;
        this.ruleRepository = ruleRepository;
        this.movideskTicketService = movideskTicketService;
        this.movideskClient = movideskClient;
    }

    @Transactional(readOnly = true)
    public DetectedNeed getNeed(Long id) {
        return needRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Need não encontrada: " + id));
    }

    @Transactional
    public DetectedNeed createTask(Long needId) {
        DetectedNeed need = getNeed(needId);
        need.setTaskStatus(TASK_STATUS_CREATED);
        need.setTaskCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return needRepository.save(need);
    }

    @Transactional
    public DetectedNeed createMasterTicket(Long needId, String actor) {
        DetectedNeed need = getNeed(needId);
        if (need.getExternalTicketId() != null && !need.getExternalTicketId().isBlank()) {
            movideskTicketService.addTicketComment(need.getExternalTicketId(), buildComment(need, actor));
            return need;
        }

        FaqCluster cluster = clusterRepository.findById(need.getClusterId())
                .orElseThrow(() -> new IllegalStateException("Cluster não encontrado: " + need.getClusterId()));
        RecurrenceRule rule = ruleRepository.findById(need.getRuleId())
                .orElseThrow(() -> new IllegalStateException("Regra não encontrada: " + need.getRuleId()));

        MovideskTicketRequest request = new MovideskTicketRequest.Builder()
                .subject("[KB] Need detectado - Cluster " + cluster.getId())
                .urgency("Low")
                .justification(buildDescription(cluster, rule))
                .serviceFirstLevel(ticketService)
                .addClient(defaultClientId)
                .ownerTeam(defaultTeam)
                .createdBy(actor != null ? actor : defaultClientId)
                .addAction(buildDescription(cluster, rule), actor != null ? actor : defaultClientId)
                .addTag("kb-governance")
                .addTag("recurrence-need")
                .build();

        MovideskTicketResponse response = movideskTicketService.createTicket(request);
        need.setExternalTicketId(response.getId());
        return needRepository.save(need);
    }

    @Transactional(readOnly = true)
    public List<MovideskTicketResponse> fetchRecurringTickets(OffsetDateTime start, OffsetDateTime end) {
        return movideskClient.searchTickets(start, end);
    }

    private String buildDescription(FaqCluster cluster, RecurrenceRule rule) {
        StringBuilder sb = new StringBuilder();
        sb.append("📌 NEED DETECTADO VIA RECORRÊNCIA\n\n");
        sb.append("Cluster ID: ").append(cluster.getId()).append("\n");
        sb.append("Regra: ").append(rule.getName()).append("\n");
        sb.append("Janela (dias): ").append(rule.getWindowDays()).append("\n");
        sb.append("Limite: ").append(rule.getThresholdCount()).append("\n\n");
        sb.append("Exemplo de ticket:\n");
        sb.append(truncate(cluster.getSampleText(), 500)).append("\n");
        return sb.toString();
    }

    private String buildComment(DetectedNeed need, String actor) {
        return "Need já possui ticket mestre. Atualização registrada por " + (actor != null ? actor : "sistema")
                + " em " + OffsetDateTime.now(ZoneOffset.UTC) + ".";
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }
}

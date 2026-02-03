package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.DetectedNeed;
import br.com.consisa.gov.kb.domain.RecurrenceRule;
import br.com.consisa.gov.kb.repository.DetectedNeedRepository;
import br.com.consisa.gov.kb.repository.FaqClusterRepository;
import br.com.consisa.gov.kb.repository.FaqClusterTicketRepository;
import br.com.consisa.gov.kb.repository.RecurrenceRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class RecurrenceService {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceService.class);

    private static final String NEED_STATUS_OPEN = "OPEN";
    private static final String TASK_STATUS_PENDING = "PENDING";

    private final RecurrenceRuleRepository ruleRepository;
    private final FaqClusterRepository clusterRepository;
    private final FaqClusterTicketRepository clusterTicketRepository;
    private final DetectedNeedRepository needRepository;

    public RecurrenceService(
            RecurrenceRuleRepository ruleRepository,
            FaqClusterRepository clusterRepository,
            FaqClusterTicketRepository clusterTicketRepository,
            DetectedNeedRepository needRepository
    ) {
        this.ruleRepository = ruleRepository;
        this.clusterRepository = clusterRepository;
        this.clusterTicketRepository = clusterTicketRepository;
        this.needRepository = needRepository;
    }

    @Transactional
    public void evaluateRules() {
        List<RecurrenceRule> rules = ruleRepository.findByActiveTrue();
        if (rules.isEmpty()) {
            return;
        }
        clusterRepository.findAll().forEach(cluster -> {
            for (RecurrenceRule rule : rules) {
                evaluateCluster(rule, cluster.getId());
            }
        });
    }

    private void evaluateCluster(RecurrenceRule rule, Long clusterId) {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(rule.getWindowDays());
        long count = clusterTicketRepository.countTicketsInWindow(clusterId, cutoff);
        if (count < rule.getThresholdCount()) {
            return;
        }
        DetectedNeed need = needRepository.findByClusterIdAndRuleId(clusterId, rule.getId())
                .orElseGet(DetectedNeed::new);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (need.getId() != null && need.getLastDetectedAt() != null) {
            OffsetDateTime nextAllowed = need.getLastDetectedAt().plusHours(rule.getCooldownHours());
            if (now.isBefore(nextAllowed)) {
                return;
            }
        }

        if (need.getId() == null) {
            need.setClusterId(clusterId);
            need.setRuleId(rule.getId());
            need.setStatus(NEED_STATUS_OPEN);
            need.setTaskStatus(TASK_STATUS_PENDING);
        }
        need.setLastDetectedAt(now);
        needRepository.save(need);
        log.info("🔁 Need detectado: cluster={} rule={} count={}", clusterId, rule.getId(), count);
    }
}

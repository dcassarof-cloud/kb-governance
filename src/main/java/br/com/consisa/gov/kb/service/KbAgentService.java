package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbAgent;
import br.com.consisa.gov.kb.repository.KbAgentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 👥 Service de Gerenciamento de Agentes
 *
 * RESPONSABILIDADES:
 * ------------------
 * - CRUD de agentes
 * - Busca por especialidade e carga de trabalho
 * - Seleção inteligente de agentes para atribuição
 * - Atualização de estatísticas
 *
 * QUANDO USAR:
 * ------------
 * - Auto-assign de artigos
 * - Balanceamento de carga
 * - Relatórios de produtividade
 *
 * ✅ VERSÃO CORRIGIDA - Compatível com KbAgentRepository
 */
@Service
public class KbAgentService {

    private static final Logger log = LoggerFactory.getLogger(KbAgentService.class);

    private final KbAgentRepository repository;

    public KbAgentService(KbAgentRepository repository) {
        this.repository = repository;
    }

    /**
     * 🔍 Busca agente por ID
     */
    @Transactional(readOnly = true)
    public Optional<KbAgent> findById(String id) {
        return repository.findById(id);
    }

    /**
     * 📋 Lista todos os agentes ativos
     */
    @Transactional(readOnly = true)
    public List<KbAgent> findAllActive() {
        return repository.findByIsActiveTrue();
    }

    /**
     * 🎯 Seleciona melhor agente para um sistema específico
     *
     * Critérios (em ordem):
     * 1. Agente ativo
     * 2. Tem especialidade no sistema
     * 3. Menor carga de trabalho (assigned_count)
     * 4. Maior produtividade histórica (completed_count)
     *
     * @param systemCode código do sistema (ex: CONSISANET, NOTAON)
     * @return agente mais adequado ou empty se nenhum encontrado
     */
    @Transactional(readOnly = true)
    public Optional<KbAgent> findBestAgentForSystem(String systemCode) {
        log.debug("🎯 Buscando melhor agente para sistema: {}", systemCode);

        List<KbAgent> candidates = repository.findBySpecialtyOrderByWorkload(systemCode);

        if (candidates.isEmpty()) {
            log.warn("⚠️ Nenhum agente com especialidade em {}", systemCode);
            // Fallback: busca agente menos ocupado geral
            return repository.findLeastBusy();
        }

        KbAgent selected = candidates.get(0);

        log.info("✅ Agente selecionado: {} (especialidade={}, carga={})",
                selected.getBusinessName(), systemCode, selected.getAssignedCount());

        return Optional.of(selected);
    }

    /**
     * 🎯 Seleciona agente menos ocupado (sem considerar especialidade)
     */
    @Transactional(readOnly = true)
    public Optional<KbAgent> findLeastBusyAgent() {
        return repository.findLeastBusy();
    }

    /**
     * 📊 Busca top N agentes mais produtivos
     *
     * ✅ CORRIGIDO: Usa PageRequest para limitar resultados
     */
    @Transactional(readOnly = true)
    public List<KbAgent> findTopProductive(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        return repository.findTopByProductivity(pageRequest);
    }

    /**
     * 📊 Conta total de agentes ativos
     */
    @Transactional(readOnly = true)
    public long countActiveAgents() {
        return repository.countByIsActiveTrue();
    }

    /**
     * ➕ Incrementa contador de atribuições do agente
     */
    @Transactional
    public void incrementAssigned(String agentId) {
        repository.findById(agentId).ifPresent(agent -> {
            agent.incrementAssigned();
            repository.save(agent);
            log.debug("📊 Agente {} agora tem {} atribuições",
                    agent.getBusinessName(), agent.getAssignedCount());
        });
    }

    /**
     * ✅ Registra conclusão de atribuição
     */
    @Transactional
    public void registerCompletion(String agentId) {
        repository.findById(agentId).ifPresent(agent -> {
            agent.completeAssignment();
            repository.save(agent);
            log.debug("✅ Agente {} concluiu atribuição (total: {})",
                    agent.getBusinessName(), agent.getCompletedCount());
        });
    }

    /**
     * ❌ Decrementa contador (para cancelamento)
     */
    @Transactional
    public void decrementAssigned(String agentId) {
        repository.findById(agentId).ifPresent(agent -> {
            agent.decrementAssigned();
            repository.save(agent);
            log.debug("📊 Agente {} teve atribuição removida (total: {})",
                    agent.getBusinessName(), agent.getAssignedCount());
        });
    }

    /**
     * 💾 Salva ou atualiza agente
     */
    @Transactional
    public KbAgent save(KbAgent agent) {
        return repository.save(agent);
    }

    /**
     * 📊 Conta agentes ativos
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return repository.countByIsActiveTrue();
    }
}
package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbArticle;
import br.com.consisa.gov.kb.domain.KbArticleVersion;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbArticleVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 📚 SERVICE DE VERSIONAMENTO DE ARTIGOS
 *
 * RESPONSABILIDADES:
 * ------------------
 * ✅ Criar snapshot do artigo antes de alterações
 * ✅ Rastrear histórico completo de mudanças
 * ✅ Permitir comparação entre versões
 * ✅ Habilitar rollback se necessário
 * ✅ Auditoria completa de alterações
 *
 * QUANDO USAR:
 * ------------
 * - Antes de QUALQUER atualização de conteúdo
 * - Aprovação de artigo
 * - Correção de problemas de governança
 * - Sincronização com Movidesk
 *
 * COMO FUNCIONA:
 * --------------
 * 1. Antes de alterar artigo, chama createVersion()
 * 2. Sistema salva snapshot completo do estado atual
 * 3. Incrementa version_number automaticamente
 * 4. Calcula hash do conteúdo para detectar mudanças reais
 */
@Service
public class KbArticleVersionService {

    private static final Logger log = LoggerFactory.getLogger(KbArticleVersionService.class);

    private final KbArticleVersionRepository versionRepo;
    private final KbArticleRepository articleRepo;

    public KbArticleVersionService(
            KbArticleVersionRepository versionRepo,
            KbArticleRepository articleRepo
    ) {
        this.versionRepo = versionRepo;
        this.articleRepo = articleRepo;
    }

    // ======================
    // CRIAÇÃO DE VERSÕES
    // ======================

    /**
     * 📸 Cria snapshot do artigo ANTES de alteração
     *
     * @param articleId ID do artigo
     * @param changedBy quem está fazendo a mudança
     * @param reason motivo da mudança
     * @param changeType tipo: CREATED, UPDATED, APPROVED, REVERTED
     * @return versão criada
     */
    @Transactional
    public KbArticleVersion createVersion(
            Long articleId,
            String changedBy,
            String reason,
            String changeType
    ) {
        var article = articleRepo.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Artigo não encontrado: " + articleId));

        log.info("📸 Criando versão do artigo {} (por: {}, motivo: {})",
                articleId, changedBy, reason);

        // Busca última versão para incrementar número
        Integer nextVersion = versionRepo.getMaxVersionNumber(articleId)
                .map(v -> v + 1)
                .orElse(1);

        // Calcula hash do conteúdo
        String contentHash = calculateContentHash(article);

        // Verifica se conteúdo realmente mudou (evita versões duplicadas)
        var lastVersion = versionRepo.findLatestVersion(articleId);
        if (lastVersion.isPresent() && lastVersion.get().getContentHash().equals(contentHash)) {
            log.warn("⚠️ Conteúdo não mudou, pulando criação de versão");
            return lastVersion.get();
        }

        // Cria nova versão
        var version = new KbArticleVersion();
        version.setArticleId(articleId);
        version.setVersionNumber(nextVersion);

        // Snapshot do conteúdo
        version.setTitle(article.getTitle());
        version.setSummary(article.getSummary());
        version.setContentText(article.getContentText());
        version.setContentHtml(article.getContentHtml());
        version.setContentHash(contentHash);

        // Metadata
        version.setChangedBy(changedBy);
        version.setChangeReason(reason);
        version.setChangeType(changeType);
        version.setGovernanceStatus(article.getGovernanceStatus());

        var saved = versionRepo.save(version);

        log.info("✅ Versão {} criada para artigo {}", nextVersion, articleId);

        return saved;
    }

    /**
     * 📸 Cria versão inicial (quando artigo é criado pela primeira vez)
     */
    @Transactional
    public KbArticleVersion createInitialVersion(Long articleId, String createdBy) {
        return createVersion(
                articleId,
                createdBy,
                "Versão inicial do artigo",
                "CREATED"
        );
    }

    /**
     * 📸 Cria versão após atualização
     */
    @Transactional
    public KbArticleVersion createUpdateVersion(Long articleId, String updatedBy, String reason) {
        return createVersion(
                articleId,
                updatedBy,
                reason,
                "UPDATED"
        );
    }

    /**
     * 📸 Cria versão após aprovação
     */
    @Transactional
    public KbArticleVersion createApprovalVersion(Long articleId, String approvedBy) {
        return createVersion(
                articleId,
                approvedBy,
                "Artigo aprovado para uso em IA",
                "APPROVED"
        );
    }

    // ======================
    // CONSULTAS
    // ======================

    /**
     * 📜 Retorna histórico completo de versões de um artigo
     */
    @Transactional(readOnly = true)
    public List<KbArticleVersion> getHistory(Long articleId) {
        return versionRepo.findByArticleIdOrderByVersionNumberDesc(articleId);
    }

    /**
     * 📜 Retorna versão específica
     */
    @Transactional(readOnly = true)
    public KbArticleVersion getVersion(Long articleId, Integer versionNumber) {
        return versionRepo.findByArticleIdAndVersionNumber(articleId, versionNumber)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Versão não encontrada: artigo=" + articleId + ", versão=" + versionNumber
                ));
    }

    /**
     * 📜 Retorna última versão
     */
    @Transactional(readOnly = true)
    public KbArticleVersion getLatestVersion(Long articleId) {
        return versionRepo.findLatestVersion(articleId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhuma versão encontrada para artigo: " + articleId
                ));
    }

    /**
     * 📊 Compara duas versões
     */
    @Transactional(readOnly = true)
    public VersionComparison compareVersions(Long articleId, Integer versionA, Integer versionB) {
        var vA = getVersion(articleId, versionA);
        var vB = getVersion(articleId, versionB);

        boolean titleChanged = !vA.getTitle().equals(vB.getTitle());
        boolean contentChanged = !vA.getContentHash().equals(vB.getContentHash());

        return new VersionComparison(
                vA,
                vB,
                titleChanged,
                contentChanged,
                calculateDiffSize(vA, vB)
        );
    }

    // ======================
    // ROLLBACK
    // ======================

    /**
     * 🔄 Restaura artigo para uma versão anterior
     *
     * CUIDADO: Isso cria uma NOVA versão com o conteúdo antigo.
     * Não deleta o histórico.
     */
    @Transactional
    public KbArticle rollbackToVersion(Long articleId, Integer targetVersion, String rolledBackBy) {
        var version = getVersion(articleId, targetVersion);
        var article = articleRepo.findById(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Artigo não encontrado"));

        log.warn("🔄 ROLLBACK: artigo {} para versão {} (por: {})",
                articleId, targetVersion, rolledBackBy);

        // Cria snapshot ANTES do rollback
        createVersion(
                articleId,
                rolledBackBy,
                "Rollback para versão " + targetVersion,
                "REVERTED"
        );

        // Restaura conteúdo
        article.setTitle(version.getTitle());
        article.setSummary(version.getSummary());
        article.setContentText(version.getContentText());
        article.setContentHtml(version.getContentHtml());

        var saved = articleRepo.save(article);

        log.info("✅ Rollback concluído para artigo {}", articleId);

        return saved;
    }

    // ======================
    // HELPERS
    // ======================

    /**
     * Calcula hash SHA-256 do conteúdo para detectar mudanças
     */
    private String calculateContentHash(KbArticle article) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            String combined = String.join("|",
                    article.getTitle() != null ? article.getTitle() : "",
                    article.getSummary() != null ? article.getSummary() : "",
                    article.getContentText() != null ? article.getContentText() : ""
            );

            byte[] hash = digest.digest(combined.getBytes());
            return bytesToHex(hash);

        } catch (NoSuchAlgorithmException e) {
            log.error("Erro ao calcular hash", e);
            return "HASH_ERROR";
        }
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private int calculateDiffSize(KbArticleVersion vA, KbArticleVersion vB) {
        int titleDiff = Math.abs(
                (vA.getTitle() != null ? vA.getTitle().length() : 0) -
                (vB.getTitle() != null ? vB.getTitle().length() : 0)
        );

        int contentDiff = Math.abs(
                (vA.getContentText() != null ? vA.getContentText().length() : 0) -
                (vB.getContentText() != null ? vB.getContentText().length() : 0)
        );

        return titleDiff + contentDiff;
    }

    // ======================
    // RECORD (DTO)
    // ======================

    public record VersionComparison(
            KbArticleVersion versionA,
            KbArticleVersion versionB,
            boolean titleChanged,
            boolean contentChanged,
            int diffSize
    ) {}
}

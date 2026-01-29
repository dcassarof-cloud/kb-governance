package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.controller.api.dto.ManualTaskAssignRequest;
import br.com.consisa.gov.kb.controller.api.dto.ManualTaskMergeRequest;
import br.com.consisa.gov.kb.controller.api.dto.ManualTaskMoveRequest;
import br.com.consisa.gov.kb.controller.api.dto.ManualTaskReviewRequest;
import br.com.consisa.gov.kb.controller.api.dto.ManualTaskStatusRequest;
import br.com.consisa.gov.kb.domain.*;
import br.com.consisa.gov.kb.repository.KbArticleRepository;
import br.com.consisa.gov.kb.repository.KbManualActionLogRepository;
import br.com.consisa.gov.kb.repository.KbManualTaskRepository;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class KbManualTaskService {

    private final KbManualTaskRepository taskRepository;
    private final KbManualActionLogRepository actionLogRepository;
    private final KbNotificationService notificationService;
    private final KbArticleRepository articleRepository;
    private final KbSystemRepository systemRepository;
    private final ObjectMapper objectMapper;

    public KbManualTaskService(
            KbManualTaskRepository taskRepository,
            KbManualActionLogRepository actionLogRepository,
            KbNotificationService notificationService,
            KbArticleRepository articleRepository,
            KbSystemRepository systemRepository,
            ObjectMapper objectMapper
    ) {
        this.taskRepository = taskRepository;
        this.actionLogRepository = actionLogRepository;
        this.notificationService = notificationService;
        this.articleRepository = articleRepository;
        this.systemRepository = systemRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Page<KbManualTaskRepository.ManualTaskRow> listTasks(
            Pageable pageable,
            Long systemId,
            String status,
            String risk,
            String priority,
            String assigneeId,
            String issueType,
            String text
    ) {
        return taskRepository.pageTasks(pageable, systemId, status, risk, priority, assigneeId, issueType, text);
    }

    @Transactional(readOnly = true)
    public KbManualTaskRepository.ManualTaskRow getTaskRow(Long taskId) {
        return taskRepository.findTaskRowById(taskId);
    }

    @Transactional
    public KbManualTask assign(Long taskId, ManualTaskAssignRequest request) {
        KbManualTask task = getTaskOrThrow(taskId);

        KbManualAssigneeType assigneeType = parseEnum(KbManualAssigneeType.class, request.assigneeType(), "assigneeType");
        if (request.assigneeId() == null || request.assigneeId().isBlank()) {
            throw new IllegalArgumentException("assigneeId é obrigatório");
        }

        task.setAssigneeType(assigneeType);
        task.setAssigneeId(request.assigneeId());
        task.setDueAt(request.dueAt());

        KbManualTask saved = taskRepository.save(task);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("assigneeType", assigneeType.name());
        payload.put("assigneeId", request.assigneeId());
        payload.put("dueAt", request.dueAt());
        payload.put("note", request.note());

        logAction(saved.getId(), KbManualActionType.ASSIGNED, request.actorType(), request.actorId(), request.actorName(), payload);

        notificationService.notifyRecipient(
                KbNotificationRecipientType.valueOf(assigneeType.name()),
                request.assigneeId(),
                KbNotificationSeverity.INFO,
                "Novo manual atribuído",
                "Você recebeu um manual para governança (ID: " + saved.getArticleId() + ")",
                "/governance/tasks/" + saved.getId()
        );

        return saved;
    }

    @Transactional
    public KbManualTask updateStatus(Long taskId, ManualTaskStatusRequest request) {
        KbManualTask task = getTaskOrThrow(taskId);

        KbManualTaskStatus currentStatus = task.getStatus();
        KbManualTaskStatus newStatus = parseEnum(KbManualTaskStatus.class, request.status(), "status");
        validateTransition(currentStatus, newStatus);

        if (newStatus == KbManualTaskStatus.IGNORED) {
            if (request.ignoredReason() == null || request.ignoredReason().isBlank()) {
                throw new IllegalArgumentException("ignoredReason é obrigatório quando status = IGNORED");
            }
            task.setIgnoredReason(request.ignoredReason());
        } else {
            task.setIgnoredReason(null);
        }

        task.setStatus(newStatus);
        KbManualTask saved = taskRepository.save(task);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", currentStatus.name());
        payload.put("to", newStatus.name());
        payload.put("ignoredReason", request.ignoredReason());
        payload.put("note", request.note());

        logAction(saved.getId(), KbManualActionType.STATUS_CHANGED, request.actorType(), request.actorId(), request.actorName(), payload);

        return saved;
    }

    @Transactional
    public void mergeManual(Long taskId, ManualTaskMergeRequest request) {
        KbManualTask task = getTaskOrThrow(taskId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("articleId", task.getArticleId());
        payload.put("mergedIntoArticleId", request.mergedIntoArticleId());
        payload.put("note", request.note());

        logAction(taskId, KbManualActionType.MERGED, request.actorType(), request.actorId(), request.actorName(), payload);
    }

    @Transactional
    public void moveManual(Long taskId, ManualTaskMoveRequest request) {
        KbManualTask task = getTaskOrThrow(taskId);
        KbArticle article = articleRepository.findById(task.getArticleId())
                .orElseThrow(() -> new IllegalStateException("Artigo não encontrado: " + task.getArticleId()));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fromSystemId", article.getSystem() != null ? article.getSystem().getId() : null);
        payload.put("fromMenuId", article.getSourceMenuId());
        payload.put("fromMenuName", article.getSourceMenuName());

        if (request.systemId() != null) {
            KbSystem system = systemRepository.findById(request.systemId())
                    .orElseThrow(() -> new IllegalArgumentException("Sistema não encontrado: " + request.systemId()));
            article.setSystem(system);
        }

        if (request.menuId() != null) {
            article.setSourceMenuId(request.menuId());
        }

        if (request.menuName() != null) {
            article.setSourceMenuName(request.menuName());
        }

        articleRepository.save(article);

        payload.put("toSystemId", request.systemId());
        payload.put("toMenuId", request.menuId());
        payload.put("toMenuName", request.menuName());
        payload.put("note", request.note());

        logAction(taskId, KbManualActionType.MOVED, request.actorType(), request.actorId(), request.actorName(), payload);
    }

    @Transactional
    public void reviewManual(Long taskId, ManualTaskReviewRequest request) {
        getTaskOrThrow(taskId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("checklist", request.checklist());
        payload.put("approved", request.approved());
        payload.put("note", request.note());

        logAction(taskId, KbManualActionType.REVIEWED, request.actorType(), request.actorId(), request.actorName(), payload);
    }

    private KbManualTask getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Task não encontrada: " + taskId));
    }

    private void validateTransition(KbManualTaskStatus current, KbManualTaskStatus target) {
        if (current == target) {
            return;
        }

        boolean allowed = switch (current) {
            case OPEN -> target == KbManualTaskStatus.IN_PROGRESS
                    || target == KbManualTaskStatus.BLOCKED
                    || target == KbManualTaskStatus.DONE
                    || target == KbManualTaskStatus.IGNORED;
            case IN_PROGRESS -> target == KbManualTaskStatus.BLOCKED
                    || target == KbManualTaskStatus.DONE
                    || target == KbManualTaskStatus.IGNORED;
            case BLOCKED -> target == KbManualTaskStatus.IN_PROGRESS
                    || target == KbManualTaskStatus.DONE
                    || target == KbManualTaskStatus.IGNORED;
            case DONE, IGNORED -> false;
        };

        if (!allowed) {
            throw new IllegalStateException(
                    "Transição inválida: " + current + " -> " + target
            );
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " é obrigatório");
        }
        return Enum.valueOf(enumClass, value.trim().toUpperCase(Locale.ROOT));
    }

    private void logAction(
            Long taskId,
            KbManualActionType actionType,
            String actorType,
            String actorId,
            String actorName,
            Map<String, Object> payload
    ) {
        KbManualActionLog log = new KbManualActionLog();
        log.setTaskId(taskId);
        log.setActionType(actionType);
        log.setActorType(parseActorType(actorType));
        log.setActorId(actorId);
        log.setActorName(actorName);
        log.setPayloadJson(toJson(payload));
        actionLogRepository.save(log);
    }

    private KbManualActorType parseActorType(String actorType) {
        if (actorType == null || actorType.isBlank()) {
            return KbManualActorType.USER;
        }
        return KbManualActorType.valueOf(actorType.trim().toUpperCase(Locale.ROOT));
    }

    private JsonNode toJson(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        return objectMapper.valueToTree(payload);
    }
}

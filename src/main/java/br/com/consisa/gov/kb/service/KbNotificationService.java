package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.domain.KbNotification;
import br.com.consisa.gov.kb.domain.KbNotificationRecipientType;
import br.com.consisa.gov.kb.domain.KbNotificationSeverity;
import br.com.consisa.gov.kb.repository.KbNotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KbNotificationService {

    private final KbNotificationRepository repository;

    public KbNotificationService(KbNotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public KbNotification notifyRecipient(
            KbNotificationRecipientType recipientType,
            String recipientId,
            KbNotificationSeverity severity,
            String title,
            String message,
            String actionUrl
    ) {
        KbNotification notification = new KbNotification();
        notification.setRecipientType(recipientType);
        notification.setRecipientId(recipientId);
        notification.setSeverity(severity != null ? severity : KbNotificationSeverity.INFO);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setActionUrl(actionUrl);
        notification.setIsRead(Boolean.FALSE);
        return repository.save(notification);
    }
}

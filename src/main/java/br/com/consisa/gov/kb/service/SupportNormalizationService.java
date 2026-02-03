package br.com.consisa.gov.kb.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class SupportNormalizationService {

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("\\b\\d{6,}\\b", "[id]");
        normalized = normalized.replaceAll("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b", "[cpf]");
        normalized = normalized.replaceAll("\\b\\d{11}\\b", "[cpf]");
        normalized = normalized.replaceAll("\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b", "[cnpj]");
        normalized = normalized.replaceAll("\\b\\d{14}\\b", "[cnpj]");
        normalized = normalized.replaceAll("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", "[email]");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    public String fingerprint(String normalized) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar fingerprint", ex);
        }
    }
}

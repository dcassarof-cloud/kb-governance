package br.com.consisa.gov.kb.governance;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Utilitários de análise de conteúdo (texto).
 */
@Service
public class KbContentAnalysisService {

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    public String normalize(String text) {
        if (text == null) return "";
        String t = text.trim().toLowerCase();
        t = MULTI_SPACE.matcher(t).replaceAll(" ");
        return t;
    }

    public int length(String text) {
        if (text == null) return 0;
        return text.trim().length();
    }

    public boolean hasPlaceholder(String normalized) {
        if (normalized == null || normalized.isBlank()) return false;

        // ✅ só frases bem específicas (evita falso positivo)
        String[] patterns = {
                "conteúdo em construção",
                "manual em construção",
                "em construção",
                "em breve",
                "a definir",
                "preencher aqui",
                "inserir aqui",
                "colocar aqui",
                "todo:",
                "todo ",
                "[todo]"
        };

        for (String p : patterns) {
            if (normalized.contains(p)) return true;
        }

        return false;
    }

}

package br.com.consisa.gov.kb.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 🔐 Service especializado em hash e normalização de conteúdo
 *
 * RESPONSABILIDADES:
 * ------------------
 * 1. Normalizar texto (remove espaços múltiplos, lowercase)
 * 2. Gerar SHA-256 do conteúdo normalizado
 * 3. Detectar duplicados via content_hash
 *
 * POR QUE NORMALIZAR?
 * -------------------
 * Sem normalização, estes conteúdos seriam diferentes:
 * - "Como   cadastrar   cliente"
 * - "como cadastrar cliente"
 * - "COMO CADASTRAR CLIENTE"
 *
 * Com normalização, todos geram o MESMO hash.
 *
 * ALGORITMO:
 * ----------
 * 1. trim()
 * 2. toLowerCase()
 * 3. regex: múltiplos espaços → espaço único
 * 4. SHA-256 → hex string
 *
 * EXEMPLO:
 * --------
 * Input:  "Como   CADASTRAR   Cliente  "
 * Norm:   "como cadastrar cliente"
 * Hash:   "a1b2c3d4..."
 */
@Service
public class KbArticleHashService {

    private static final MessageDigest SHA256_DIGEST;

    static {
        try {
            SHA256_DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 não disponível no sistema", e);
        }
    }

    /**
     * Normaliza texto para comparação/hash.
     *
     * Regras:
     * - Remove espaços no início/fim
     * - Converte para minúsculas
     * - Múltiplos espaços → espaço único
     *
     * @param text texto bruto
     * @return texto normalizado ou empty string se null
     */
    public String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        return text
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    /**
     * Gera SHA-256 hex de um texto.
     *
     * @param text texto já normalizado (use normalize() antes)
     * @return hash hex (64 caracteres) ou null se texto vazio
     *
     * @throws RuntimeException se erro ao gerar hash
     */
    public String sha256(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);

            // MessageDigest não é thread-safe, mas é rápido
            // Para alta concorrência, considere usar ThreadLocal
            synchronized (SHA256_DIGEST) {
                byte[] hashBytes = SHA256_DIGEST.digest(bytes);
                return bytesToHex(hashBytes);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar SHA-256", e);
        }
    }

    /**
     * Gera hash normalizado (pipeline completo).
     *
     * Equivale a: sha256(normalize(text))
     *
     * @param text texto bruto
     * @return hash hex ou null se texto vazio
     */
    public String generateContentHash(String text) {
        String normalized = normalize(text);

        if (normalized.isBlank()) {
            return null;
        }

        return sha256(normalized);
    }

    /**
     * Converte byte[] para string hexadecimal.
     *
     * Exemplo: [0xAB, 0xCD] → "abcd"
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }

        return hex.toString();
    }

    /**
     * Calcula tamanho do texto limpo (sem espaços múltiplos).
     *
     * Útil para detectar conteúdo muito curto.
     *
     * @param text texto bruto
     * @return quantidade de caracteres após normalização
     */
    public int cleanLength(String text) {
        return normalize(text).length();
    }
}

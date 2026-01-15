-- Habilita funções criptográficas (digest, gen_random_uuid, etc)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) backfill (simples): usa content_text se tiver, senão content_html
-- Normaliza: lower + remove múltiplos espaços
-- digest() precisa de bytea -> converte com convert_to(..., 'UTF8')

UPDATE kb_article
SET content_hash = encode(
        digest(
                convert_to(
                        lower(
                                regexp_replace(
                                        coalesce(nullif(content_text, ''), content_html, ''),
                                        '\s+',
                                        ' ',
                                        'g'
                                )
                        ),
                        'UTF8'
                ),
                'sha256'
        ),
        'hex'
                   )
WHERE content_hash IS NULL OR content_hash = '';

-- 2) index pra acelerar duplicados
CREATE INDEX IF NOT EXISTS idx_kb_article_content_hash ON kb_article(content_hash);


-- Seed do catálogo de sistemas da KB
-- Seguro: insere se não existir e atualiza campos úteis se já existir.

INSERT INTO kb_system (code, name, description, is_active, created_at, updated_at)
VALUES
    ('GERAL', 'Geral', 'Artigos sem classificação específica ou de uso geral.', true, now(), now()),
    ('CONSISANET', 'ConsisaNet', 'ERP ConsisaNet (módulos serão tratados via kb_module).', true, now(), now()),
    ('QUINTO_EIXO', 'Quinto Eixo', 'Sistema Quinto Eixo (frota/gestão).', true, now(), now()),
    ('SGRH', 'SGRH', 'Sistema de gestão de recursos humanos.', true, now(), now()),
    ('NOTAON', 'NotaOn', 'Sistema NotaOn.', true, now(), now()),
    ('BIOJOB', 'BioJob', 'Sistema BioJob.', true, now(), now()),
    ('CONTA_SHOP', 'Conta Shop', 'Conta Shop / loja / portal relacionado.', true, now(), now()),
    ('ACOR', 'Açor', 'Sistema Açor.', true, now(), now()),
    ('ORDENA', 'Ordena', 'Sistema Ordena.', true, now(), now()),
    ('EDOC', 'eDoc', 'Sistema eDoc.', true, now(), now()),
    ('CAPTURA', 'Captura', 'Sistema Captura.', true, now(), now()),
    ('CLOUD_EDI', 'CLOUD/EDI', 'Integrações Cloud/EDI.', true, now(), now())
    ON CONFLICT (code) DO UPDATE
                              SET
                                  name = EXCLUDED.name,
                              description = EXCLUDED.description,
                              is_active = EXCLUDED.is_active,
                              updated_at = now();

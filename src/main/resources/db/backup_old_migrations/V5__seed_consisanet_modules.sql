INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'FISCAL', 'Fiscal' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'FINANCEIRO', 'Financeiro' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'FATURAMENTO', 'Faturamento' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'UTILITÁRIOS', 'Utilitários' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'CONTABILIDADE', 'Contabilidade' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'DARF', 'Darf' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'CEREAIS', 'Cereais' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'PATRIMÔNIO', 'Patrimônio' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'INVENTÁRIO', 'Inventário' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'INDÚSTRIA', 'Indústria' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'CAIXA', 'Caixa' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'PROTOCOLO', 'Protocolo' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;

INSERT INTO kb_module(system_id, code, name)
SELECT s.id, 'ESCRITÓRIO', 'Escritório' FROM kb_system s WHERE s.code='CONSISANET'
    ON CONFLICT DO NOTHING;


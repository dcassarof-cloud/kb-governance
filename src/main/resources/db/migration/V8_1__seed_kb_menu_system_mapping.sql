-- =========================================================
-- SEED: KB MENU MAP (Movidesk menu -> kb_system)
-- Estratégia: não criar submódulos no kb_system.
-- Tudo que é "Consisanet - X" mapeia para o system CONSISANET.
-- =========================================================

-- (Opcional, mas recomendado) Garante que o sistema CONSISANET existe
-- Se já existir, não faz nada.
insert into kb_system (code, name, description, is_active, created_at, updated_at)
values ('CONSISANET', 'ConsisaNet', 'Sistema ConsisaNet (ERP)', true, now(), now())
    on conflict (code) do nothing;

-- Mapeamento: ConsisaNet - Faturamento -> CONSISANET
insert into kb_menu_map (source_menu_id, source_menu_name, system_id)
select 13281, 'Consisanet - Faturamento', id
from kb_system
where code = 'CONSISANET'
    on conflict (source_system, source_menu_id) do nothing;

-- Mapeamento: ConsisaNet - Escritório -> CONSISANET
-- (use o ID real que você viu na API; aqui é exemplo, ajuste se necessário)
insert into kb_menu_map (source_menu_id, source_menu_name, system_id)
select 18624, 'Consisanet - Escritório', id
from kb_system
where code = 'CONSISANET'
    on conflict (source_system, source_menu_id) do nothing;

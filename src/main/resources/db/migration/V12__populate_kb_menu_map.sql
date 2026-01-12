-- SGRH
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 7711, 'Menu SGRH',
        (select id from kb_system where code = 'SGRH'), true);

-- NOTAON
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 3113, 'Menu notaon',
        (select id from kb_system where code = 'NOTAON'), true);

-- QUINTO EIXO
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 3139, 'Menu Quinto Eixo',
        (select id from kb_system where code = 'QUINTO_EIXO'), true);

-- CONTA SHOP
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 7800, 'Conta Shop',
        (select id from kb_system where code = 'CONTA_SHOP'), true);

-- CONSISANET (todos os submenus)
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values
    ('movidesk', 13282, 'Consisanet - Fiscal',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 13284, 'Consisanet - Contabilidade',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 13283, 'Consisanet - Financeiro',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 18618, 'Consisanet - DARFs',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 18620, 'Consisanet - Patrimônio',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 18622, 'Consisanet - Caixa',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 18621, 'Consisanet - Inventários',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 18619, 'Consisanet - Cereais',
     (select id from kb_system where code = 'CONSISANET'), true),

    ('movidesk', 18623, 'Consisanet - Protocolos',
     (select id from kb_system where code = 'CONSISANET'), true);

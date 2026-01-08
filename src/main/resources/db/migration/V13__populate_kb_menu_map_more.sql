-- MENU CONSISANET (geral)
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 3114, 'Menu consisanet',
        (select id from kb_system where code = 'CONSISANET'), true);

-- BIOJOB
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 18419, 'Menu Biojob',
        (select id from kb_system where code = 'BIOJOB'), true);

-- CLOUD/EDI
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 18625, 'CLOUD/EDI',
        (select id from kb_system where code = 'CLOUD_EDI'), true);

-- AÇOR
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 12085, 'Açor',
        (select id from kb_system where code = 'ACOR'), true);

-- ORDENA
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 18628, 'Ordena',
        (select id from kb_system where code = 'ORDENA'), true);

-- EDOC
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 18627, 'Edoc',
        (select id from kb_system where code = 'EDOC'), true);

-- CAPTURA
insert into kb_menu_map (source_system, source_menu_id, source_menu_name, system_id, is_active)
values ('movidesk', 18626, 'Captura',
        (select id from kb_system where code = 'CAPTURA'), true);

package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.SystemDto;
import br.com.consisa.gov.kb.repository.KbSystemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemsService {

    private final KbSystemRepository repo;

    public SystemsService(KbSystemRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<SystemDto> listActive() {
        return repo.findByIsActiveTrueOrderByNameAsc().stream()
                .map(s -> new SystemDto(s.getId(), s.getCode(), s.getName(), s.getIsActive()))
                .toList();
    }
}

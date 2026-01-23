package br.com.consisa.gov.kb.service;

import br.com.consisa.gov.kb.dto.DuplicateGroupDto;
import br.com.consisa.gov.kb.dto.GovernanceIssueDto;
import br.com.consisa.gov.kb.dto.PageResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GovernanceService {

    public PageResponseDto<GovernanceIssueDto> listIssues(int page1Based, int size) {
        return new PageResponseDto<>(
                page1Based,
                size,
                0,
                0,
                List.of()
        );
    }

    public List<DuplicateGroupDto> listDuplicates() {
        return List.of();
    }
}

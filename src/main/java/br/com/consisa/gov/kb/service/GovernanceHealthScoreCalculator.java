package br.com.consisa.gov.kb.service;

import org.springframework.stereotype.Component;

@Component
public class GovernanceHealthScoreCalculator {

    public double calculate(long errorOpen, long warnOpen, long infoOpen) {
        double impact = (errorOpen * 5.0) + (warnOpen * 3.0) + (infoOpen * 1.0);
        return Math.max(0.0, 100.0 - impact);
    }
}

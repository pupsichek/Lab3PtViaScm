package com.example.pt.lab3.service.assistant;

import com.example.pt.lab3.domain.ContractPlayerIdWithJSessionId;
import com.example.pt.lab3.domain.PlayerInstance;
import com.example.pt.lab3.pojo.session.SessionInformation;
import com.example.pt.lab3.repository.ContractRepository;
import com.example.pt.lab3.service.enrich.SessionEnrichment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import static com.example.pt.lab3.configuration.GameConfiguration.OPTIMISTIC_RETRY;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurrentHttpSessionAssistant {
    private final ContractRepository contractRepository;
    private final SessionInformation sessionInformation;
    @Qualifier(OPTIMISTIC_RETRY)
    private final RetryTemplate retryTemplate;

    /**
     * Get current contract from current session
     * Contract set in com.example.pt.lab3.service.enrich.SessionEnrichment
     * @see SessionEnrichment#enrichSessionWithCurrentContract(javax.servlet.http.HttpSession, org.springframework.http.HttpHeaders)
     * @return current player
     */
    public ContractPlayerIdWithJSessionId getCurrentContract() {
        String jSessionId = getJSessionId();
        log.info("Current http session {}", jSessionId);
        return retryTemplate.execute(ctx -> contractRepository.mustFindById(jSessionId));
    }

    public PlayerInstance getCurrentPlayer() {
        return getCurrentContract().getPlayer();
    }

    private String getJSessionId() {
        return sessionInformation.getJSessionId();
    }
}

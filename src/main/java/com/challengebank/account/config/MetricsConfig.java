package com.challengebank.account.config;

import com.challengebank.account.model.enums.AccountStatus;
import com.challengebank.account.repository.AccountRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class MetricsConfig {

    @Inject
    MeterRegistry meterRegistry;

    @Inject
    AccountRepository accountRepository;

    void onStart(@Observes StartupEvent ev) {
        Gauge.builder("account.active.total",
                        accountRepository, repo -> repo.countByStatus(AccountStatus.ACTIVE))
                .description("Total number of active accounts")
                .register(meterRegistry);
    }
}

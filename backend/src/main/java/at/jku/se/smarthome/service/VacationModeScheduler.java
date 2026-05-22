package at.jku.se.smarthome.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled component that performs daily vacation mode state checks.
 *
 * <p>Runs once per day at midnight (cron {@code "0 0 0 * * *"}) and delegates
 * to {@link VacationModeService#checkAndUpdateStates()}, which enables or
 * disables the referenced schedule based on the active date window.</p>
 *
 * <p>Implements FR-21: Urlaubsmodus.</p>
 */
@Component
public class VacationModeScheduler {

    private final VacationModeService vacationModeService;

    /**
     * Constructs a {@code VacationModeScheduler} with the required service.
     *
     * @param vacationModeService the service that checks and updates vacation mode states
     */
    public VacationModeScheduler(VacationModeService vacationModeService) {
        this.vacationModeService = vacationModeService;
    }

    /**
     * Triggers the daily vacation mode state check at midnight.
     *
     * <p>Runs automatically via Spring's task scheduler (cron {@code "0 0 0 * * *"}).
     * Analogous to {@link RuleScheduler#runDueTimeRules()}.</p>
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyCheck() {
        vacationModeService.checkAndUpdateStates();
    }
}

package at.jku.se.smarthome.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled component that evaluates TIME-based automation rules once per minute.
 *
 * <p>Delegates to {@link RuleService#evaluateTimeRules()} which finds all enabled
 * {@link at.jku.se.smarthome.domain.TriggerType#TIME} rules whose configured hour,
 * minute, and day of week match the current time, then fires their actions.</p>
 *
 * <p>Implements US-012: zeitbasierter Trigger für Rule Engine.</p>
 */
@Component
public class RuleScheduler {

    private final RuleService ruleService;

    /**
     * Constructs a {@code RuleScheduler} with the required service.
     *
     * @param ruleService the service that evaluates and executes TIME rules
     */
    public RuleScheduler(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * Triggers evaluation of all due TIME rules at the start of every minute.
     *
     * <p>Runs automatically via Spring's task scheduler (cron {@code "0 * * * * *"}).
     * Analogous to {@link ScheduleService#runDueSchedules()}.</p>
     */
    @Scheduled(cron = "0 * * * * *")
    public void runDueTimeRules() {
        ruleService.evaluateTimeRules();
    }
}

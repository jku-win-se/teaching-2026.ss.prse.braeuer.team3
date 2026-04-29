package at.jku.se.smarthome.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuleSchedulerTest {

    @Mock
    private RuleService ruleService;

    @InjectMocks
    private RuleScheduler ruleScheduler;

    @Test
    void runDueTimeRules_delegatesToRuleService() {
        ruleScheduler.runDueTimeRules();

        verify(ruleService).evaluateTimeRules();
    }
}

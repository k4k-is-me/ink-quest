package k4k.inkquest.questing.services.taskConditionTesters;

import k4k.inkquest.domain.models.taskConditions.PredicateCondition;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PredicateConditionHandlerTest {
    private static final Identifier PREDICATE_ID = Identifier.of("test", "is_raining");

    private PredicateConditionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PredicateConditionHandler();
    }

    private static PredicateCondition condition() {
        return new PredicateCondition(PREDICATE_ID);
    }

    @Test
    void predicatePasses_testReturnsTrue() {
        var context = new StubConditionContext().withPredicate(PREDICATE_ID, true);
        assertTrue(handler.test(condition(), context));
    }

    @Test
    void predicateFails_testReturnsFalse() {
        var context = new StubConditionContext().withPredicate(PREDICATE_ID, false);
        assertFalse(handler.test(condition(), context));
    }

    @Test
    void predicateNotRegistered_testReturnsFalse() {
        var context = new StubConditionContext();
        assertFalse(handler.test(condition(), context));
    }

    @Test
    void predicatePasses_getCurrentValueReturnsOne() {
        var context = new StubConditionContext().withPredicate(PREDICATE_ID, true);
        assertEquals(1, handler.getCurrentValue(condition(), context));
    }

    @Test
    void predicateFails_getCurrentValueReturnsZero() {
        var context = new StubConditionContext().withPredicate(PREDICATE_ID, false);
        assertEquals(0, handler.getCurrentValue(condition(), context));
    }
}

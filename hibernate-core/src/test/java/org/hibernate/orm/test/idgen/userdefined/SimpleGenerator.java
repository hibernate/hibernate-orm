package org.hibernate.orm.test.idgen.userdefined;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;

/**
 * @author Yanming Zhou
 */
public class SimpleGenerator implements BeforeExecutionGenerator {

    private final AtomicLong sequence;

    public SimpleGenerator(AtomicLong sequence) {
        this.sequence = sequence;
    }

    @Override
    public Object generate(
            SharedSessionContractImplementor session,
            Object owner,
            Object currentValue,
            EventType eventType) {
        return sequence.getAndIncrement();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EnumSet.of( EventType.INSERT );
    }
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.lang.reflect.Member;
import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.GeneratorCreationContext;

/**
 * @author Steve Ebersole
 */
public class StaticValueGenerator implements BeforeExecutionGenerator {
	private final String staticValue;
	private final EnumSet<EventType> events;

	public StaticValueGenerator(StaticGeneration annotation, Member member, GeneratorCreationContext context) {
		this.staticValue = annotation.value();
		this.events = toEnumSet( annotation.event() );
	}

	private static EnumSet<EventType> toEnumSet(EventType[] events) {
		if ( events.length == 0 ) {
			return EnumSet.of( EventType.INSERT );
		}

		if ( events.length == 1 ) {
			return EnumSet.of ( events[0] );
		}

		assert events.length == 2;
		return EnumSet.allOf( EventType.class );
	}

	@Override
	public String generate(
			SharedSessionContractImplementor session,
			Object owner,
			Object currentValue,
			EventType eventType) {
		return staticValue;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return events;
	}
}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import java.util.EnumSet;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.id.Configurable;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

/**
 * @author Yanming Zhou
 */
public class SimpleGenerator implements BeforeExecutionGenerator, Configurable {

	private final AtomicLong sequence;

	private long step = 1;

	public SimpleGenerator(AtomicLong sequence) {
		this.sequence = sequence;
	}

	@Override
	public Object generate(
			SharedSessionContractImplementor session,
			Object owner,
			Object currentValue,
			EventType eventType) {
		return sequence.getAndAdd(step);
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return EnumSet.of( EventType.INSERT );
	}

	@Override
	public void configure(Type type, Properties parameters, ServiceRegistry serviceRegistry) throws MappingException {
		this.step = (Long) parameters.get("step");
	}
}

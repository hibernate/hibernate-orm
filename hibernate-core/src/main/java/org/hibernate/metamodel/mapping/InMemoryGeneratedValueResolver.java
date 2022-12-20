/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.EventType;
import org.hibernate.generator.BeforeExecutionGenerator;

/**
 * GeneratedValueResolver impl for in-memory generation
 *
 * @author Steve Ebersole
 */
@Internal
public class InMemoryGeneratedValueResolver implements GeneratedValueResolver {
	private final EventType eventType;
	private final BeforeExecutionGenerator generator;

	public InMemoryGeneratedValueResolver(BeforeExecutionGenerator generator, EventType eventType) {
		this.generator = generator;
		this.eventType = eventType;
	}

//	@Override
//	public GenerationTiming getGenerationTiming() {
//		return generationTiming;
//	}

	@Override
	public Object resolveGeneratedValue(Object[] row, Object entity, SharedSessionContractImplementor session, Object currentValue) {
		return generator.generate( session, entity, currentValue, eventType );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.exceptionhandling;

import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Identifier generator that adopts the id of an associated entity, mirroring the
 * legacy {@code foreign} strategy. Throws {@link IdentifierGenerationException}
 * when the referenced association is {@code null}, which exercises the identifier
 * generator failure handling pipeline.
 */
public class FkIdGenerator implements BeforeExecutionGenerator, AnnotationBasedGenerator<FkId> {

	private String propertyName;

	@Override
	public void initialize(FkId annotation, GeneratorCreationContext context) {
		this.propertyName = annotation.property();
	}

	@Override
	public Object generate(
			SharedSessionContractImplementor session,
			Object owner,
			Object currentValue,
			EventType eventType) {
		final EntityPersister persister = session.getEntityPersister( null, owner );
		final Object associated = persister.getPropertyValue( owner, propertyName );
		if ( associated == null ) {
			throw new IdentifierGenerationException(
					"Could not assign id from null association '" + propertyName + "'" );
		}
		return session.getEntityPersister( null, associated ).getIdentifier( associated, session );
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return EventTypeSets.INSERT_ONLY;
	}
}

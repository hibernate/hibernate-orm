/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.EnumSet;

import org.hibernate.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.mapping.GeneratorDescriptor;

import static org.hibernate.id.IdentifierGeneratorHelper.getForeignId;

/// Describes identifier generation by copying the identifier of an associated
/// entity.
///
/// The descriptor retains only the names needed to perform generation, making
/// it safe to retain in an archived boot model. The generator itself is
/// nonexportable and is therefore normally created during SessionFactory
/// construction.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public record DerivedIdentifierGeneratorDescriptor(
		String entityName,
		String propertyName) implements GeneratorDescriptor {

	@Override
	public Generator createGenerator(GeneratorCreationContext context) {
		return new DerivedIdentifierGenerator( entityName, propertyName );
	}

	@Override
	public Class<? extends Generator> getGeneratorClass(GeneratorCreationContext context) {
		return DerivedIdentifierGenerator.class;
	}

	private record DerivedIdentifierGenerator(
			String entityName,
			String propertyName) implements BeforeExecutionGenerator {

		@Override
		public Object generate(
				SharedSessionContractImplementor session,
				Object owner,
				Object currentValue,
				EventType eventType) {
			return getForeignId( entityName, propertyName, session, owner );
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}

		@Override
		public boolean allowAssignedIdentifiers() {
			return true;
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.EnumSet;

import org.hibernate.Internal;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.Generator;
import org.hibernate.generator.GeneratorCreationContext;
import org.hibernate.generator.internal.GeneratedGeneration;
import org.hibernate.mapping.GeneratorDescriptor;

/// Archive-safe description of the built-in [GeneratedGeneration].
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public final class GeneratedValueGeneratorDescriptor implements GeneratorDescriptor {
	private final EnumSet<EventType> eventTypes;
	private final boolean writable;
	private final String sql;

	public GeneratedValueGeneratorDescriptor(Generated annotation) {
		eventTypes = EventTypeSets.fromArray( annotation.event() );
		writable = annotation.writable();
		sql = annotation.sql();
	}

	@Override
	public Generator createGenerator(GeneratorCreationContext context) {
		return new GeneratedGeneration(
				eventTypes,
				writable,
				sql,
				context.getType().getReturnedClass()
		);
	}

	@Override
	public Class<? extends Generator> getGeneratorClass(GeneratorCreationContext context) {
		return GeneratedGeneration.class;
	}
}

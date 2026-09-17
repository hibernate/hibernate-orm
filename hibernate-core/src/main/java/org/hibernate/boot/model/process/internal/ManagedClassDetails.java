/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.process.internal;

import org.hibernate.MappingException;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.jdk.JdkClassDetails;
import org.hibernate.models.spi.MutableClassDetailsRegistry;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.ClassDetailsRegistry;

/// Registers supplied handles without silently replacing another model.
///
/// @author Steve Ebersole
public final class ManagedClassDetails {
	private ManagedClassDetails() {
	}

	public static void register(ClassDetails details, ClassDetailsRegistry registry) {
		ManagedResourceValidation.validateClassName( details.getName() );
		final var existing = registry.findClassDetails( details.getName() );
		if ( existing != null && existing != details ) {
			throw new MappingException( "Conflicting ClassDetails for '" + details.getName() + "'" );
		}
		if ( existing == null ) {
			registry.as( MutableClassDetailsRegistry.class ).addClassDetails( details.getName(), details );
		}
	}

	public static ClassDetails resolve(Class<?> type, ModelsContext context) {
		ManagedResourceValidation.validateClassName( type.getName() );
		final var registry = context.getClassDetailsRegistry();
		final var existing = registry.findClassDetails( type.getName() );
		if ( existing != null ) {
			if ( !existing.isRealClass() || existing.toJavaClass() != type ) {
				throw new MappingException( "Conflicting class handle for '" + type.getName() + "'" );
			}
			return existing;
		}
		final var details = new JdkClassDetails( type, context );
		register( details, registry );
		return details;
	}

}

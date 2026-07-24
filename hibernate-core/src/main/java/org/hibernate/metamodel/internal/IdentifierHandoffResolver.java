/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;


import java.util.Objects;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.MappingRole;

import static org.hibernate.mapping.MappingRole.PartKind.IDENTIFIER;

/// Resolves applied identifier binding metadata from legacy identifier mapping
/// objects during runtime/JPA metamodel creation.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierHandoffResolver {
	private final RuntimeMappingHandoff runtimeMappingHandoff;

	public IdentifierHandoffResolver(RuntimeMappingHandoff runtimeMappingHandoff) {
		this.runtimeMappingHandoff = Objects.requireNonNull( runtimeMappingHandoff );
	}

	/// Determines whether the identifier property should be registered as the
	/// concrete specialization of a generic mapped-superclass identifier.
	public boolean isConcreteGenericIdentifier(PersistentClass persistentClass, Property identifierProperty) {
		final MappingRole role = identifierProperty.getMappingRole() == null
				? MappingRole.entity( persistentClass.getEntityName() ).append( IDENTIFIER )
				: identifierProperty.getMappingRole();
		final AttributeUsageHandoff handoff = runtimeMappingHandoff.findAttribute( role );
		return handoff != null && handoff.isConcreteGenericUsage();
	}
}

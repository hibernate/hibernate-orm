/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface EntityNameResolver {
	<T> EntityDomainType<T> resolveEntityName(String entityName);

	static <T> ManagedDomainType<T> managedType(String subtypeName, EntityNameResolver entityNameResolver) {
		final EntityDomainType<T> entityDomainType = entityNameResolver.resolveEntityName( subtypeName );
		if ( entityDomainType == null ) {
			throw new IllegalArgumentException( "Unknown managed type: " + subtypeName );
		}
		return entityDomainType;
	}
}

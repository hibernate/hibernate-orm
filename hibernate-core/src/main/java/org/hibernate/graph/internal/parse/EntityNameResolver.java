/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse;

import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface EntityNameResolver {
	<T> EntityDomainType<T> resolveEntityName(String entityName);
}

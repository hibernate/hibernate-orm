/*
 * SPDX-License-Identifier: Apache-2.0
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

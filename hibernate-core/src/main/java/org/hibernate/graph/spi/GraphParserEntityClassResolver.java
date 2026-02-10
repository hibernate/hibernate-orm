/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Gavin King
 *
 * @since 7.2
 */
@Incubating
@FunctionalInterface
public interface GraphParserEntityClassResolver {
	EntityDomainType<?> resolveEntityClass(Class<?> entityClass);
}

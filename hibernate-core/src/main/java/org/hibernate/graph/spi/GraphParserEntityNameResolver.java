/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 *
 * @since 7.2
 */
@Incubating
@FunctionalInterface
public interface GraphParserEntityNameResolver {
	EntityDomainType<?> resolveEntityName(String entityName);
}

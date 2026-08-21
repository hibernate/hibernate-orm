/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.graph.internal.parse.strategy;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.graph.spi.GraphImplementor;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.metamodel.model.domain.EntityDomainType;

public interface GraphParsingStrategy {
	<T> RootGraphImplementor<T> parse(
			EntityDomainType<T> entityDomainType,
			String graphText,
			SessionFactoryImplementor sessionFactory);

	@Deprecated(forRemoval = true)
	<T> RootGraphImplementor<T> parse(String graphText, SessionFactoryImplementor sessionFactory);

	void parseInto(GraphImplementor<?> graph, String graphText, SessionFactoryImplementor sessionFactory);

}

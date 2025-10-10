/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import org.hibernate.graph.GraphParser;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

@Jpa(
		annotatedClasses = {
				GraphParsingTestEntity.class,
				GraphParsingTestSubEntity.class
		}
)
public abstract class AbstractEntityGraphTest {

	protected <T> RootGraphImplementor<T> parseGraph(Class<T> entityType, String graphString, EntityManagerFactoryScope scope) {
		return scope.fromEntityManager(
				entityManager -> {
					return (RootGraphImplementor<T>) GraphParser.parse( entityType, graphString, entityManager );
				}
		);
	}

	protected <T> RootGraphImplementor<GraphParsingTestEntity> parseGraph(String graphString, EntityManagerFactoryScope scope) {
		return parseGraph( GraphParsingTestEntity.class, graphString, scope );
	}

}

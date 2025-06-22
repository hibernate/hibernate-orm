/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.parser;

import jakarta.persistence.EntityManager;

import org.hibernate.graph.GraphParser;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

public abstract class AbstractEntityGraphTest extends BaseEntityManagerFunctionalTestCase {

	public AbstractEntityGraphTest() {
		super();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ GraphParsingTestEntity.class, GraphParsingTestSubEntity.class };
	}

	protected <T> RootGraphImplementor<T> parseGraph(Class<T> entityType, String graphString) {
		EntityManager entityManager = getOrCreateEntityManager();
		return (RootGraphImplementor<T>) GraphParser.parse( entityType, graphString, entityManager );
	}

	protected <T> RootGraphImplementor<GraphParsingTestEntity> parseGraph(String graphString) {
		return parseGraph( GraphParsingTestEntity.class, graphString );
	}

}

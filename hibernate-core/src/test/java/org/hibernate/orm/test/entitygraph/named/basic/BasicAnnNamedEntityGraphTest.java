/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.basic;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Strong Liu
 */
@DomainModel(
		annotatedClasses = Person.class
)
@SessionFactory
public class BasicAnnNamedEntityGraphTest {

	@Test
	void testIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityManager em = session.unwrap( EntityManager.class );
					EntityGraph graph = em.getEntityGraph( "Person" );
					assertThat( graph, notNullValue() );
				}
		);
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

/**
 * Based on the test developed by Hans Desmet to reproduce the bug reported in HHH-9230
 *
 * @author Steve Ebersole
 */
@JiraKey(value = "HHH-9230")
@Jpa(annotatedClasses = {
		QueryWithInParamListAndNamedEntityGraphTest.Person.class
})
public class QueryWithInParamListAndNamedEntityGraphTest {

	@Test
	public void testInClause(EntityManagerFactoryScope scope) {
		// this test works
		scope.inTransaction(
				entityManager -> {
					Set<Long> ids = new HashSet<>();
					ids.add( 1L );
					ids.add( 2L );
					TypedQuery<Person> query = entityManager.createQuery( "select p from Person p where p.id  in :ids", Person.class );
					query.setParameter( "ids", ids );
					query.getResultList();
				}
		);
	}

	@Test
	public void testEntityGraph(EntityManagerFactoryScope scope) {
		// this test works
		scope.inTransaction(
				entityManager -> {
					TypedQuery<Person> query = entityManager.createQuery( "select p from Person p", Person.class );
					query.setHint( "javax.persistence.loadgraph", entityManager.createEntityGraph( "withBoss" ) );
					query.getResultList();
				}
		);
	}

	@Test
	public void testEntityGraphAndInClause(EntityManagerFactoryScope scope) {
		// this test fails
		scope.inTransaction(
				entityManager -> {
					Set<Long> ids = new HashSet<>();
					ids.add( 1L );
					ids.add( 2L );
					TypedQuery<Person> query = entityManager.createQuery( "select p from Person p where p.id  in :ids", Person.class );
					query.setHint( "javax.persistence.loadgraph", entityManager.createEntityGraph( "withBoss" ) );
					query.setParameter( "ids", ids );
					query.getResultList();
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	@NamedEntityGraph(name = "withBoss", attributeNodes = @NamedAttributeNode("boss"))
	public static class Person {
		@Id
		@GeneratedValue
		private long id;
		private String name;
		@ManyToOne
		@JoinColumn
		private Person boss;
	}
}

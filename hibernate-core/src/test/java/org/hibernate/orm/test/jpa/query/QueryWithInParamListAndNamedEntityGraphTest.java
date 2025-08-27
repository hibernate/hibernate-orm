/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;

import org.hibernate.jpa.SpecHints;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

	@BeforeAll
	void prepareTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person boss = new Person();
					boss.id = 2L;
					boss.name = "B";
					entityManager.persist( boss );
					Person person = new Person();
					person.id = 1L;
					person.name = "X";
					person.boss = boss;
					entityManager.persist( person );
				}
		);
	}

	@Test
	public void testInClause(EntityManagerFactoryScope scope) {
		// this test works
		scope.inTransaction(
				entityManager -> {
					Set<Long> ids = new HashSet<>();
					ids.add( 1L );
					ids.add( 2L );
					entityManager.createQuery( "select p from Person p where p.id in :ids", Person.class )
							.setParameter( "ids", ids )
							.getResultList();
				}
		);
	}

	@Test
	public void testEntityGraph(EntityManagerFactoryScope scope) {
		// this test works
		scope.inTransaction(
				entityManager -> {
					entityManager.createQuery( "select p from Person p", Person.class )
							.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH,
									entityManager.createEntityGraph( QueryWithInParamListAndNamedEntityGraphTest_.Person_.GRAPH_WITH_BOSS ) )
							.getResultList();
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
					entityManager.createQuery( "select p from Person p where p.id in :ids", Person.class )
							.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH,
									entityManager.createEntityGraph( QueryWithInParamListAndNamedEntityGraphTest_.Person_.GRAPH_WITH_BOSS ) )
							.setParameter( "ids", ids )
							.getResultList();
				}
		);
	}

	@Test
	public void testNamedEntityGraph(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person =
							entityManager.createQuery( "select p from Person p where p.boss is not null", Person.class )
									.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH,
											QueryWithInParamListAndNamedEntityGraphTest_.Person_.GRAPH_WITH_BOSS )
									.getSingleResult();
					assertTrue( isInitialized( person.boss ) );
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	@NamedEntityGraph(name = "withBoss",
			attributeNodes = @NamedAttributeNode("boss"))
	public static class Person {
		@Id
		private long id;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn
		private Person boss;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph.named.basic;

import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.TypedQuery;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Jpa(
		annotatedClasses = LoadFetchGraphWithEagerSelfReferencingEagerToOneTest.Sample.class,
		properties = @Setting(name = AvailableSettings.MAX_FETCH_DEPTH, value = "")
)
@JiraKey(value = "HHH-15391")
public class LoadFetchGraphWithEagerSelfReferencingEagerToOneTest {

	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Sample first = new Sample( 1, "first" );
					Sample second = new Sample( 2, "second" );
					Sample third = new Sample( 3, "third" );
					first.setParent( second );
					second.setParent( third );

					entityManager.persist( second );
					entityManager.persist( first );
					entityManager.persist( third );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testQuery2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {

					TypedQuery<Sample> query = entityManager.createQuery(
							"select s from Sample s where s.id = :id",
							Sample.class
					).setParameter( "id", 1 );
					query.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH, entityManager.getEntityGraph( "sample.name" ) );
					List<Sample> results = query.getResultList();
					assertThat( results.size(), is( 1 ) );
				}
		);
	}


	@Test
	public void testQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {

					TypedQuery<Sample> query = entityManager.createQuery(
							"select s from Sample s",
							Sample.class
					);
					query.setHint( SpecHints.HINT_SPEC_LOAD_GRAPH, entityManager.getEntityGraph( "sample.name" ) );
					List<Sample> results = query.getResultList();
					assertThat( results.size(), is( 3 ) );
				}
		);
	}

	@Test
	public void testFind(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Sample sample = entityManager.find(
							Sample.class,
							1,
							Collections.singletonMap(
									SpecHints.HINT_SPEC_LOAD_GRAPH,
									entityManager.getEntityGraph( "sample.name" )
							)
					);

					assertNotNull( sample );
				}
		);
	}

	@Entity(name = "Sample")
	@NamedEntityGraphs({
			@NamedEntityGraph(
					name = "sample.name",
					attributeNodes = { @NamedAttributeNode("name") })
	})
	public static class Sample {

		@Id
		private Integer id;

		@ManyToOne
		Sample parent;

		String name;

		public Sample() {
		}

		public Sample(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Sample getParent() {
			return parent;
		}

		public void setParent(Sample parent) {
			this.parent = parent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}

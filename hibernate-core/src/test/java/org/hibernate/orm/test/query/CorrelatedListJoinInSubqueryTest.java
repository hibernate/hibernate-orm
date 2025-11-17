/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.community.dialect.TiDBDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		CorrelatedListJoinInSubqueryTest.Property.class,
		CorrelatedListJoinInSubqueryTest.Entity1.class,
		CorrelatedListJoinInSubqueryTest.Entity2.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16888" )
@SkipForDialect( dialectClass = TiDBDialect.class, reason = "TiDB doesn't support subqueries in ON conditions yet" )
public class CorrelatedListJoinInSubqueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Entity1 entity1 = new Entity1( "key" );
			session.persist( entity1 );
			final Property property1 = new Property( "admin" );
			session.persist( property1 );
			final Property property2 = new Property( "name" );
			session.persist( property2 );
			final Entity2 entity2 = new Entity2( "key", List.of( property1, property2 ) );
			session.persist( entity2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Entity1" ).executeUpdate();
			session.createQuery( "from Entity2", Entity2.class ).getResultList().forEach( entity2 -> {
				entity2.getProperties().forEach( session::remove );
				entity2.getProperties().clear();
				session.remove( entity2 );
			} );
		} );
	}

	@Test
	public void testEntityJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Tuple> resultList = session.createQuery(
					"select e1, e2 " +
							"from Entity1 e1 " +
							"join Entity2 e2 on e1.uniqueKey = e2.uniqueKey and exists (" +
							"select 1 from e2.properties p where p.name = :propertyName)",
					Tuple.class
			).setParameter( "propertyName", "admin" ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@Test
	public void testCrossJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Tuple> resultList = session.createQuery(
					"select e1, e2 " +
							"from Entity1 e1, Entity2 e2 " +
							"where e1.uniqueKey = e2.uniqueKey and exists (" +
							"select 1 from e2.properties p where p.name = :propertyName)",
					Tuple.class
			).setParameter( "propertyName", "admin" ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@Entity( name = "Property" )
	public static class Property {
		@Id
		@GeneratedValue
		private Long id;
		private String name;

		public Property() {
		}

		public Property(String name) {
			this.name = name;
		}
	}

	@Entity( name = "Entity1" )
	public static class Entity1 {
		@Id
		@GeneratedValue
		private Long id;
		private String uniqueKey;

		public Entity1() {
		}

		public Entity1(String uniqueKey) {
			this.uniqueKey = uniqueKey;
		}
	}

	@Entity( name = "Entity2" )
	public static class Entity2 {

		@Id
		@GeneratedValue
		private Long id;

		private String uniqueKey;

		@ManyToMany( fetch = FetchType.LAZY )
		private List<Property> properties;

		public Entity2() {
		}

		public Entity2(String uniqueKey, List<Property> properties) {
			this.uniqueKey = uniqueKey;
			this.properties = properties;
		}

		public List<Property> getProperties() {
			return properties;
		}
	}
}

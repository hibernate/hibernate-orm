/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TypedQuery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		BatchFetchInstantiationTest.EntityA.class,
		BatchFetchInstantiationTest.EntityB.class,
		BatchFetchInstantiationTest.MyPojo.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2" ) )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16559" )
public class BatchFetchInstantiationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityB entityB = new EntityB( "entity_b" );
			session.persist( entityB );
			session.persist( new EntityA( 1L, entityB ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
		} );
	}

	@Test
	public void testNormalSelect(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TypedQuery<EntityA> query = session.createQuery(
					"select t from EntityA t where t.id = ?1",
					EntityA.class
			).setParameter( 1, 1L );
			final EntityA result = query.getSingleResult();
			assertThat( result.getEntityB().getName() ).isEqualTo( "entity_b" );
		} );
	}

	@Test
	public void testDynamicInstantiation(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TypedQuery<MyPojo> query2 = session.createQuery(
					String.format( "select new %s(t) from EntityA t where t.id = ?1", MyPojo.class.getName() ),
					MyPojo.class
			).setParameter( 1, 1L );
			final MyPojo pojo = query2.getSingleResult();
			assertThat( pojo.getName() ).isEqualTo( "entity_b" );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		private Long id;

		@ManyToOne
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(Long id, EntityB entityB) {
			this.id = id;
			this.entityB = entityB;
		}

		public EntityB getEntityB() {
			return entityB;
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public EntityB() {
		}

		public EntityB(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public static class MyPojo {
		private final String name;

		public MyPojo(EntityA a) {
			this.name = a.getEntityB().getName();
		}

		public String getName() {
			return name;
		}
	}
}

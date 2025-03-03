/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		MappedSuperclassAttributeQueryTest.AbstractSuperclass.class,
		MappedSuperclassAttributeQueryTest.EntityA.class,
		MappedSuperclassAttributeQueryTest.EntityB.class,
		MappedSuperclassAttributeQueryTest.EntityC.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16543" )
public class MappedSuperclassAttributeQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = new EntityA();
			entityA.setData( "data" );
			session.persist( entityA );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery(
				String.format( "delete from %s", AbstractSuperclass.class.getName() )
		).executeUpdate() );
	}

	@Test
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<AbstractSuperclass> resultList = session.createQuery(
					String.format( "select t from %s t where t.data = 'data'", AbstractSuperclass.class.getName() ),
					AbstractSuperclass.class
			).getResultList();
			assertThat( resultList ).hasSize( 1 );
			assertThat( resultList.get( 0 ) ).isInstanceOf( EntityA.class );
			assertThat( resultList.get( 0 ).getData() ).isEqualTo( "data" );
		} );
	}

	@MappedSuperclass
	public abstract static class AbstractSuperclass {
		@Id
		@GeneratedValue
		private Integer id;

		private String data;

		public Integer getId() {
			return id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity( name = "EntityA" )
	public static class EntityA extends AbstractSuperclass {
	}

	@Entity( name = "EntityB" )
	public static class EntityB extends AbstractSuperclass {
	}

	@Entity( name = "EntityC" )
	public static class EntityC extends AbstractSuperclass {
	}
}

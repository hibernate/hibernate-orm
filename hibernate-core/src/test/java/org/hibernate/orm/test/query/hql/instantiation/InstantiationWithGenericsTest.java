/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.instantiation;

import java.io.Serializable;

import org.hibernate.annotations.Imported;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		InstantiationWithGenericsTest.AbstractEntity.class,
		InstantiationWithGenericsTest.ConcreteEntity.class,
		InstantiationWithGenericsTest.ConstructorDto.class,
		InstantiationWithGenericsTest.InjectionDto.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18218" )
public class InstantiationWithGenericsTest {
	@Test
	public void testConstructor(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select new ConstructorDto(e.id, e.data) from ConcreteEntity e",
				ConstructorDto.class
		).getSingleResult() ).extracting( ConstructorDto::getId, ConstructorDto::getData )
				.containsExactly( 1L, "entity_1" ) );
	}

	@Test
	public void testImplicitConstructor(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select e.id, e.data from ConcreteEntity e",
				ConstructorDto.class
		).getSingleResult() ).extracting( ConstructorDto::getId, ConstructorDto::getData )
				.containsExactly( 1L, "entity_1" ) );
	}

	@Test
	public void testInjection(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select new InjectionDto(e.id as id, e.data as data) from ConcreteEntity e",
				InjectionDto.class
		).getSingleResult() ).extracting( InjectionDto::getId, InjectionDto::getData )
				.containsExactly( 1L, "entity_1" ) );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ConcreteEntity entity = new ConcreteEntity();
			entity.setId( 1L );
			entity.setData( "entity_1" );
			session.persist( entity );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ConcreteEntity" ).executeUpdate() );
	}

	@MappedSuperclass
	static abstract class AbstractEntity<K extends Serializable> {
		@Id
		protected K id;

		protected K getId() {
			return id;
		}

		protected void setId(K id) {
			this.id = id;
		}
	}

	@Entity( name = "ConcreteEntity" )
	static class ConcreteEntity extends AbstractEntity<Long> {
		protected String data;

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Imported
	public static class ConstructorDto {
		private final Long id;

		private final String data;

		public ConstructorDto(Long id, String data) {
			this.id = id;
			this.data = data;
		}

		public Long getId() {
			return id;
		}

		public String getData() {
			return data;
		}
	}

	@Imported
	public static class InjectionDto {
		private long id;

		private String data;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}

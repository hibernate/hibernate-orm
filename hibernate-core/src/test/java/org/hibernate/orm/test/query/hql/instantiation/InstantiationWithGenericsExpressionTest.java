/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql.instantiation;

import java.io.Serializable;

import org.hibernate.annotations.Imported;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		InstantiationWithGenericsExpressionTest.AbstractEntity.class,
		InstantiationWithGenericsExpressionTest.ConcreteEntity.class,
		InstantiationWithGenericsExpressionTest.ConstructorDto.class,
		InstantiationWithGenericsExpressionTest.InjectionDto.class,
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18218")
public class InstantiationWithGenericsExpressionTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ConcreteEntity entity = new ConcreteEntity();
			entity.setId( 1 );
			entity.setGen( 1L );
			entity.setData( "entity_1" );
			session.persist( entity );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from ConcreteEntity" ).executeUpdate() );
	}

	@Test
	public void testConstructorBinaryExpression(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select new ConstructorDto(e.gen+e.gen, e.data) from ConcreteEntity e",
				ConstructorDto.class
		).getSingleResult() ).extracting( ConstructorDto::getGen, ConstructorDto::getData )
				.containsExactly( 2L, "entity_1" ) );
	}

	@Test
	public void testImplicitConstructorBinaryExpression(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select e.gen+e.gen, e.data from ConcreteEntity e",
				ConstructorDto.class
		).getSingleResult() ).extracting( ConstructorDto::getGen, ConstructorDto::getData )
				.containsExactly( 2L, "entity_1" ) );
	}

	@Test
	public void testInjectionBinaryExpression(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select new InjectionDto(e.gen+e.gen as gen, e.data as data) from ConcreteEntity e",
				InjectionDto.class
		).getSingleResult() ).extracting( InjectionDto::getGen, InjectionDto::getData )
				.containsExactly( 2L, "entity_1" ) );
	}

	@Test
	public void testConstructorUnaryExpression(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select new ConstructorDto(-e.gen, e.data) from ConcreteEntity e",
				ConstructorDto.class
		).getSingleResult() ).extracting( ConstructorDto::getGen, ConstructorDto::getData )
				.containsExactly( -1L, "entity_1" ) );
	}

	@Test
	public void testImplicitConstructorUnaryExpression(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select -e.gen, e.data from ConcreteEntity e",
				ConstructorDto.class
		).getSingleResult() ).extracting( ConstructorDto::getGen, ConstructorDto::getData )
				.containsExactly( -1L, "entity_1" ) );
	}

	@Test
	public void testInjectionUnaryExpression(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select new InjectionDto(-e.gen as gen, e.data as data) from ConcreteEntity e",
				InjectionDto.class
		).getSingleResult() ).extracting( InjectionDto::getGen, InjectionDto::getData )
				.containsExactly( -1L, "entity_1" ) );
	}

	@Test
	public void testConstructorFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select new ConstructorDto(abs(e.gen), e.data) from ConcreteEntity e",
					ConstructorDto.class
			).getSingleResult() ).extracting( ConstructorDto::getGen, ConstructorDto::getData )
					.containsExactly( 1L, "entity_1" );
		} );
	}

	@Test
	public void testImplicitFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select abs(e.gen), e.data from ConcreteEntity e",
					ConstructorDto.class
			).getSingleResult() ).extracting( ConstructorDto::getGen, ConstructorDto::getData )
					.containsExactly( 1L, "entity_1" );
		} );
	}

	@Test
	public void testInjectionFunction(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select new InjectionDto(abs(e.gen) as gen, e.data as data) from ConcreteEntity e",
					InjectionDto.class
			).getSingleResult() ).extracting( InjectionDto::getGen, InjectionDto::getData )
					.containsExactly( 1L, "entity_1" );
		} );
	}

	@MappedSuperclass
	static abstract class AbstractEntity<K extends Serializable> {
		@Id
		protected Integer id;

		protected K gen;

		public Integer getId() {
			return id;
		}

		public void setId(final Integer id) {
			this.id = id;
		}

		public K getGen() {
			return gen;
		}

		public void setGen(final K gen) {
			this.gen = gen;
		}
	}

	@Entity(name = "ConcreteEntity")
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
		private final Long gen;

		private final String data;

		public ConstructorDto(Long gen, String data) {
			this.gen = gen;
			this.data = data;
		}

		public Long getGen() {
			return gen;
		}

		public String getData() {
			return data;
		}
	}

	@Imported
	public static class InjectionDto {
		private long gen;

		private String data;

		public long getGen() {
			return gen;
		}

		public void setGen(final long gen) {
			this.gen = gen;
		}

		public String getData() {
			return data;
		}

		public void setData(String data) {
			this.data = data;
		}
	}
}

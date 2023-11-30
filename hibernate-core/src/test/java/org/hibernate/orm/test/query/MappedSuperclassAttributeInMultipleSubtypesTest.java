/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query;

import java.util.List;

import org.hibernate.query.PathException;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		BasicEntity.class,
		MappedSuperclassAttributeInMultipleSubtypesTest.BaseEntity.class,
		MappedSuperclassAttributeInMultipleSubtypesTest.MappedSuper.class,
		MappedSuperclassAttributeInMultipleSubtypesTest.ChildOne.class,
		MappedSuperclassAttributeInMultipleSubtypesTest.ChildTwo.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17491" )
public class MappedSuperclassAttributeInMultipleSubtypesTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BasicEntity basicEntity = new BasicEntity( 1, "basic" );
			session.persist( basicEntity );
			session.persist( new ChildOne( 1L, "test", 1, basicEntity ) );
			session.persist( new ChildTwo( 2L, "test", 1D, basicEntity ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from BaseEntity" ).executeUpdate();
			session.createMutationQuery( "delete from BasicEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testSameTypeAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<BaseEntity> resultList = session.createQuery(
					"from BaseEntity e where e.stringProp = 'test'",
					BaseEntity.class
			).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( BaseEntity::getId ) ).contains( 1L, 2L );
		} );
	}

	@Test
	public void testDifferentTypeAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"from BaseEntity e where e.otherProp = 1",
						BaseEntity.class
				).getResultList();
				fail( "This shouldn't work since the attribute is defined with different types" );
			}
			catch (Exception e) {
				final Throwable cause = e.getCause();
				assertThat( cause ).isInstanceOf( PathException.class );
				assertThat( cause.getMessage() ).contains( "Could not resolve attribute 'otherProp'" );
			}
		} );
	}

	@Test
	public void testToOneAttribute(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final BasicEntity basicEntity = session.find( BasicEntity.class, 1 );
			final List<BaseEntity> resultList = session.createQuery(
					"from BaseEntity e where e.toOneProp = :be",
					BaseEntity.class
			).setParameter( "be", basicEntity ).getResultList();
			assertThat( resultList ).hasSize( 2 );
			assertThat( resultList.stream().map( BaseEntity::getId ) ).contains( 1L, 2L );
		} );
	}

	@Entity( name = "BaseEntity" )
	public static class BaseEntity {
		@Id
		private Long id;

		public BaseEntity() {
		}

		public BaseEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@MappedSuperclass
	@SuppressWarnings( "unused" )
	public static abstract class MappedSuper extends BaseEntity {
		private String stringProp;

		private Integer otherProp;

		@ManyToOne
		private BasicEntity toOneProp;

		public MappedSuper() {
		}

		public MappedSuper(Long id, String stringProp, Integer otherProp, BasicEntity toOneProp) {
			super( id );
			this.stringProp = stringProp;
			this.otherProp = otherProp;
			this.toOneProp = toOneProp;
		}
	}

	@Entity( name = "ChildOne" )
	public static class ChildOne extends MappedSuper {
		public ChildOne() {
		}

		public ChildOne(Long id, String stringProp, Integer otherProp, BasicEntity toOneProp) {
			super( id, stringProp, otherProp, toOneProp );
		}
	}

	@Entity( name = "ChildTwo" )
	@SuppressWarnings( "unused" )
	public static class ChildTwo extends BaseEntity {
		private String stringProp;

		private Double otherProp;

		@ManyToOne
		private BasicEntity toOneProp;

		public ChildTwo() {
		}

		public ChildTwo(Long id, String stringProp, Double otherProp, BasicEntity toOneProp) {
			super( id );
			this.stringProp = stringProp;
			this.otherProp = otherProp;
			this.toOneProp = toOneProp;
		}
	}
}

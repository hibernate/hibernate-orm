/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.annotations.DiscriminatorFormula;
import org.hibernate.annotations.Imported;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddableInheritanceDiscriminatorFormulaTest.TestEntity.class,
		EmbeddableInheritanceDiscriminatorFormulaTest.FormulaEmbeddable.class,
		EmbeddableInheritanceDiscriminatorFormulaTest.ChildOneEmbeddable.class,
		EmbeddableInheritanceDiscriminatorFormulaTest.SubChildOneEmbeddable.class,
		EmbeddableInheritanceDiscriminatorFormulaTest.ChildTwoEmbeddable.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18382" )
public class EmbeddableInheritanceDiscriminatorFormulaTest {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.embeddable.kind ).isEqualTo( "kind_1" );
			assertThat( result.embeddable ).isExactlyInstanceOf( ChildOneEmbeddable.class );
			assertThat( ( (ChildOneEmbeddable) result.embeddable ).childOneProp ).isEqualTo( 1 );
		} );
	}

	@Test
	public void testQueryEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where id = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( result.embeddable.kind ).isEqualTo( "kind_2" );
			assertThat( result.embeddable ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result.embeddable ).childTwoProp ).isEqualTo( "2" );
		} );
	}

	@Test
	public void testQueryEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final FormulaEmbeddable result = session.createQuery(
					"select embeddable from TestEntity where id = 3",
					FormulaEmbeddable.class
			).getSingleResult();
			assertThat( result.kind ).isEqualTo( "sub_kind_1" );
			assertThat( result ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result ).subChildOneProp ).isEqualTo( 3.0 );
		} );
	}

	@Test
	public void testType(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select id from TestEntity where type(embeddable) = ChildOneEmbeddable",
				Long.class
		).getSingleResult() ).isEqualTo( 1L ) );
	}

	@Test
	public void testTreat(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select id from TestEntity where treat(embeddable as ChildTwoEmbeddable).childTwoProp = '2'",
				Long.class
		).getSingleResult() ).isEqualTo( 2L ) );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new TestEntity( 1L, new ChildOneEmbeddable( 1 ) ) );
			session.persist( new TestEntity( 2L, new ChildTwoEmbeddable( "2" ) ) );
			session.persist( new TestEntity( 3L, new SubChildOneEmbeddable( 3, 3.0 ) ) );
		} );
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private FormulaEmbeddable embeddable;

		public TestEntity() {
		}

		public TestEntity(Long id, FormulaEmbeddable embeddable) {
			this.embeddable = embeddable;
			this.id = id;
		}
	}

	@Embeddable
	@DiscriminatorFormula(
			value = "case when kind = 'kind_1' then 1 when kind = 'sub_kind_1' then 11 else 2 end",
			discriminatorType = DiscriminatorType.INTEGER
	)
	static class FormulaEmbeddable {
		protected String kind;

		public FormulaEmbeddable() {
		}

		public FormulaEmbeddable(String kind) {
			this.kind = kind;
		}
	}

	@Embeddable
	@DiscriminatorValue( "1" )
	@Imported( rename = "ChildOneEmbeddable" )
	static class ChildOneEmbeddable extends FormulaEmbeddable {
		protected Integer childOneProp;

		public ChildOneEmbeddable() {
		}

		public ChildOneEmbeddable(Integer childOneProp) {
			super( "kind_1" );
			this.childOneProp = childOneProp;
		}

		protected ChildOneEmbeddable(String kind, Integer childOneProp) {
			super( kind );
			this.childOneProp = childOneProp;
		}
	}

	@Embeddable
	@DiscriminatorValue( "11" )
	static class SubChildOneEmbeddable extends ChildOneEmbeddable {
		protected Double subChildOneProp;

		public SubChildOneEmbeddable() {
		}

		public SubChildOneEmbeddable(Integer childOneProp, Double subChildOneProp) {
			super( "sub_kind_1", childOneProp );
			this.subChildOneProp = subChildOneProp;
		}
	}

	@Embeddable
	@DiscriminatorValue( "2" )
	@Imported( rename = "ChildTwoEmbeddable" )
	static class ChildTwoEmbeddable extends FormulaEmbeddable {
		protected String childTwoProp;

		public ChildTwoEmbeddable() {
		}

		public ChildTwoEmbeddable(String childTwoProp) {
			super( "kind_2" );
			this.childTwoProp = childTwoProp;
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddableInheritanceMappedSuperclassAndGenericsTest.AbstractSuperclass.class,
		EmbeddableInheritanceMappedSuperclassAndGenericsTest.Range.class,
		EmbeddableInheritanceMappedSuperclassAndGenericsTest.IntegerRange.class,
		EmbeddableInheritanceMappedSuperclassAndGenericsTest.ToleranceRange.class,
		EmbeddableInheritanceMappedSuperclassAndGenericsTest.TestEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18172" )
public class EmbeddableInheritanceMappedSuperclassAndGenericsTest {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity entity = session.find( TestEntity.class, 1L );
			assertThat( entity.getRange().getName() ).isEqualTo( "tolerance_range" );
			assertThat( entity.getRange() ).isExactlyInstanceOf( ToleranceRange.class );
			assertThat( ( (ToleranceRange) entity.getRange() ).getTolerance() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testQueryEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity entity = session.createQuery(
					"from TestEntity where id = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( entity.getRange().getName() ).isEqualTo( "integer_range" );
			assertThat( entity.getRange() ).isExactlyInstanceOf( IntegerRange.class );
		} );
	}

	@Test
	public void testQueryEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Range<?> result = session.createQuery(
					"select range from TestEntity where id = 1",
					Range.class
			).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "tolerance_range" );
			assertThat( result ).isExactlyInstanceOf( ToleranceRange.class );
		} );
	}

	@Test
	public void testQueryJoinedEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Range<?> result = session.createQuery(
					"select r from TestEntity t join t.range r where id = 2",
					Range.class
			).getSingleResult();
			assertThat( result.getName() ).isEqualTo( "integer_range" );
			assertThat( result ).isExactlyInstanceOf( IntegerRange.class );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final IntegerRange range = new IntegerRange();
			range.setName( "new_range" );
			range.setMin( 1 );
			range.setMin( 2 );
			final TestEntity entity = new TestEntity( 3L, range );
			session.persist( entity );
			session.flush();
			entity.getRange().setName( "updated_range" );
			entity.getRange().setMax( 3 );
		} );
		scope.inTransaction( session -> {
			final TestEntity entity = session.find( TestEntity.class, 3L );
			assertThat( entity.getRange().getName() ).isEqualTo( "updated_range" );
			assertThat( entity.getRange().getMax() ).isEqualTo( 3 );
			final ToleranceRange tolerance = new ToleranceRange();
			tolerance.setName( "new_tolerance" );
			tolerance.setMin( 10 );
			tolerance.setMax( 20 );
			tolerance.setTolerance( 4 );
			entity.setRange( tolerance );
		} );
		scope.inTransaction( session -> assertThat( session.find( TestEntity.class, 3L ).getRange() )
				.isExactlyInstanceOf( ToleranceRange.class )
				.extracting( AbstractSuperclass::getName ).isEqualTo( "new_tolerance" ) );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ToleranceRange range1 = new ToleranceRange();
			range1.setName( "tolerance_range" );
			range1.setMin( 1 );
			range1.setMax( 10 );
			range1.setTolerance( 2 );
			session.persist( new TestEntity( 1L, range1 ) );
			final IntegerRange range2 = new IntegerRange();
			range2.setName( "integer_range" );
			range2.setMin( 10 );
			range2.setMax( 20 );
			session.persist( new TestEntity( 2L, range2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@MappedSuperclass
	static abstract class AbstractSuperclass {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	@DiscriminatorValue( "range" )
	static class Range<T> extends AbstractSuperclass {
		@Column( name = "min_col" )
		private T min;
		@Column( name = "max_col" )
		private T max;

		public T getMin() {
			return min;
		}

		public void setMin(T min) {
			this.min = min;
		}

		public T getMax() {
			return max;
		}

		public void setMax(T max) {
			this.max = max;
		}
	}

	@Embeddable
	@DiscriminatorValue( "integer" )
	static class IntegerRange extends Range<Integer> {
	}

	@Embeddable
	@DiscriminatorValue( "tolerance" )
	static class ToleranceRange extends IntegerRange {
		private Integer tolerance;

		public Integer getTolerance() {
			return tolerance;
		}

		public void setTolerance(Integer tolerance) {
			this.tolerance = tolerance;
		}
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private Range<Integer> range;

		public TestEntity() {
		}

		public TestEntity(Long id, Range<Integer> range) {
			this.id = id;
			this.range = range;
		}

		public Range<Integer> getRange() {
			return range;
		}

		public void setRange(Range<Integer> range) {
			this.range = range;
		}
	}
}

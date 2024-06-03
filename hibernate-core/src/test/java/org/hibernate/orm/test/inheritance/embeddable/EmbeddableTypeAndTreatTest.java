/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddableTypeAndTreatTest.TestEntity.class,
		SimpleEmbeddable.class,
		ParentEmbeddable.class,
		ChildOneEmbeddable.class,
		SubChildOneEmbeddable.class,
		ChildTwoEmbeddable.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class EmbeddableTypeAndTreatTest {
	@Test
	public void testType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select type(t.embeddable) from TestEntity t where t.id = 1",
					Class.class
			).getSingleResult() ).isEqualTo( ChildTwoEmbeddable.class );
			assertThat( session.createQuery(
					"select type(e) from TestEntity t join t.embeddable e where t.id = 2",
					Class.class
			).getSingleResult() ).isEqualTo( SubChildOneEmbeddable.class );
			assertThat( session.createQuery(
					"select t.id from TestEntity t where type(t.embeddable) = SubChildOneEmbeddable",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
			assertThat( session.createQuery(
					"select t.id from TestEntity t join t.embeddable e where type(e) = ChildTwoEmbeddable",
					Long.class
			).getSingleResult() ).isEqualTo( 1L );
			assertThat( session.createQuery(
					"select t.id from TestEntity t where type(t.embeddable) = SubChildOneEmbeddable or t.id = 1",
					Long.class
			).getResultList() ).hasSize( 2 );
			assertThat( session.createQuery(
					"select t.id from TestEntity t where type(t.embeddable) = SubChildOneEmbeddable and type(t.embeddable) = ChildTwoEmbeddable",
					Long.class
			).getResultList() ).hasSize( 0 );
		} );
	}

	@Test
	public void testTreat(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select treat(t.embeddable as SubChildOneEmbeddable) from TestEntity t",
					SubChildOneEmbeddable.class
			).getSingleResult().getSubChildOneProp() ).isEqualTo( 2.0 );
			assertThat( session.createQuery(
					"select treat(e as SubChildOneEmbeddable) from TestEntity t join t.embeddable e",
					SubChildOneEmbeddable.class
			).getSingleResult().getSubChildOneProp() ).isEqualTo( 2.0 );
			assertThat( session.createQuery(
					"select e from TestEntity t join treat(t.embeddable as SubChildOneEmbeddable) e",
					SubChildOneEmbeddable.class
			).getSingleResult().getSubChildOneProp() ).isEqualTo( 2.0 );
			assertThat( session.createQuery(
					"select t.id from TestEntity t where treat(t.embeddable as ChildTwoEmbeddable).childTwoProp = 1",
					Long.class
			).getSingleResult() ).isEqualTo( 1L );
			assertThat( session.createQuery(
					"select t.id from TestEntity t join t.embeddable e where treat(e as ChildTwoEmbeddable).childTwoProp = 1",
					Long.class
			).getSingleResult() ).isEqualTo( 1L );
			assertThat( session.createQuery(
					"select t.id from TestEntity t join treat(t.embeddable as ChildTwoEmbeddable) e where e.childTwoProp = 1",
					Long.class
			).getSingleResult() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testTreatJunctions(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select t.id from TestEntity t where treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0 and id = 2",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_type", 1 );
			inspector.clear();
			assertThat( session.createQuery(
					"select t.id from TestEntity t where treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0 or id = 1",
					Long.class
			).getResultList() ).hasSize( 2 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_type", 1 );
			inspector.clear();
			assertThat( session.createQuery(
					"select t.id from TestEntity t where treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0 and treat(t.embeddable as SubChildOneEmbeddable).childOneProp = 2",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_type", 1 );
			inspector.clear();
			assertThat( session.createQuery(
					"select t.id from TestEntity t where treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0 and id = 1",
					Long.class
			).getResultList() ).hasSize( 0 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_type", 1 );
			inspector.clear();
			assertThat( session.createQuery(
					"select t.id from TestEntity t where id = 1 or treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0",
					Long.class
			).getResultList() ).hasSize( 2 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_type", 1 );
			inspector.clear();
			assertThat( session.createQuery(
					"select t.id from TestEntity t where treat(t.embeddable as ChildTwoEmbeddable).childTwoProp = 1 or treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0",
					Long.class
			).getResultList() ).hasSize( 2 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_type", 2 );
			inspector.clear();
			assertThat( session.createQuery(
					"select t.id from TestEntity t where treat(t.embeddable as SubChildOneEmbeddable).childOneProp = 2 or treat(t.embeddable as SubChildOneEmbeddable).subChildOneProp = 2.0",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_type", 1 );
			inspector.clear();
		} );
	}

	@Test
	public void testNonInheritedEmbeddable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select type(t.simpleEmbeddable) from TestEntity t where t.id = 1",
					Class.class
			).getSingleResult() ).isEqualTo( SimpleEmbeddable.class );
			assertThat( session.createQuery(
					"select t.id from TestEntity t where type(t.simpleEmbeddable) = SimpleEmbeddable",
					Long.class
			).getResultList() ).hasSize( 3 );
			assertThat( session.createQuery(
					"select treat(t.simpleEmbeddable as SimpleEmbeddable) from TestEntity t where t.simpleEmbeddable is not null",
					SimpleEmbeddable.class
			).getSingleResult().getData() ).isEqualTo( "simple_embeddable_1" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity testEntity = new TestEntity( 1L, new ChildTwoEmbeddable( "embeddable_1", 1L ) );
			testEntity.setSimpleEmbeddable( new SimpleEmbeddable( "simple_embeddable_1" ) );
			session.persist( testEntity );
			session.persist( new TestEntity( 2L, new SubChildOneEmbeddable( "embeddable_2", 2, 2.0 ) ) );
			session.persist( new TestEntity( 3L, new ChildOneEmbeddable( "embeddable_3", 3 ) ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from TestEntity" ).executeUpdate() );
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private ParentEmbeddable embeddable;

		@Embedded
		private SimpleEmbeddable simpleEmbeddable;

		public TestEntity() {
		}

		public TestEntity(Long id, ParentEmbeddable embeddable) {
			this.id = id;
			this.embeddable = embeddable;
		}

		public Long getId() {
			return id;
		}

		public ParentEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public SimpleEmbeddable getSimpleEmbeddable() {
			return simpleEmbeddable;
		}

		public void setSimpleEmbeddable(SimpleEmbeddable simpleEmbeddable) {
			this.simpleEmbeddable = simpleEmbeddable;
		}
	}
}

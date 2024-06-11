/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ElementCollectionEmbeddableInheritanceTest.TestEntity.class,
		ParentEmbeddable.class,
		ChildOneEmbeddable.class,
		SubChildOneEmbeddable.class,
		ChildTwoEmbeddable.class,
} )
@SessionFactory
public class ElementCollectionEmbeddableInheritanceTest {
	@Test
	public void testFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getEmbeddables() ).hasSize( 2 );
			result.getEmbeddables().forEach( embeddable -> {
				if ( embeddable instanceof ChildOneEmbeddable ) {
					assertThat( embeddable.getParentProp() ).isEqualTo( "embeddable_1" );
					assertThat( embeddable ).isExactlyInstanceOf( ChildOneEmbeddable.class );
					assertThat( ( (ChildOneEmbeddable) embeddable ).getChildOneProp() ).isEqualTo( 1 );
				}
				else {
					assertThat( embeddable.getParentProp() ).isEqualTo( "embeddable_3" );
					assertThat( embeddable ).isExactlyInstanceOf( ParentEmbeddable.class );
				}
			} );
		} );
	}

	@Test
	public void testQueryEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where id = 2",
					TestEntity.class
			).getSingleResult();
			assertThat( result.getEmbeddables() ).hasSize( 1 );
			final ParentEmbeddable embeddable = result.getEmbeddables().get( 0 );
			assertThat( embeddable.getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( embeddable ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) embeddable ).getChildOneProp() ).isEqualTo( 2 );
			assertThat( ( (SubChildOneEmbeddable) embeddable ).getSubChildOneProp() ).isEqualTo( 2.0 );
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			assertThat( result.getEmbeddables() ).hasSize( 2 );
			result.getEmbeddables().forEach( embeddable -> {
				if ( embeddable instanceof ChildTwoEmbeddable ) {
					assertThat( embeddable ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
					assertThat( ( (ChildTwoEmbeddable) embeddable ).getChildTwoProp() ).isEqualTo( 4L );
				}
				else {
					assertThat( embeddable ).isExactlyInstanceOf( ChildOneEmbeddable.class );
					assertThat( ( (ChildOneEmbeddable) embeddable ).getChildOneProp() ).isEqualTo( 5 );
					// update values
					embeddable.setParentProp( "embeddable_5_new" );
					( (ChildOneEmbeddable) embeddable ).setChildOneProp( 55 );
				}
			} );
			result.getEmbeddables().add( new SubChildOneEmbeddable( "embeddable_6", 6, 6.0 ) );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			result.getEmbeddables().forEach( embeddable -> {
				if ( embeddable instanceof SubChildOneEmbeddable ) {
					assertThat( embeddable ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
					assertThat( embeddable.getParentProp() ).isEqualTo( "embeddable_6" );
					assertThat( ( (SubChildOneEmbeddable) embeddable ).getSubChildOneProp() ).isEqualTo( 6.0 );
				}
				else if ( embeddable instanceof ChildOneEmbeddable ) {
					assertThat( embeddable ).isExactlyInstanceOf( ChildOneEmbeddable.class );
					assertThat( embeddable.getParentProp() ).isEqualTo( "embeddable_5_new" );
					assertThat( ( (ChildOneEmbeddable) embeddable ).getChildOneProp() ).isEqualTo( 55 );
				}
			} );
		} );
	}

	@Test
	public void testType(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select type(element(t.embeddables)) from TestEntity t where id = 2",
					Class.class
			).getSingleResult() ).isEqualTo( SubChildOneEmbeddable.class );
			assertThat( session.createQuery(
					"select t.id from TestEntity t where type(element(t.embeddables)) = SubChildOneEmbeddable",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
			assertThat( session.createQuery(
					"select type(e) from TestEntity t join t.embeddables e where id = 2",
					Class.class
			).getSingleResult() ).isEqualTo( SubChildOneEmbeddable.class );
			assertThat( session.createQuery(
					"select t.id from TestEntity t join t.embeddables e where type(e) = SubChildOneEmbeddable",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
		} );
	}

	@Test
	public void testTreat(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createQuery(
					"select treat(e as SubChildOneEmbeddable) from TestEntity t join t.embeddables e",
					SubChildOneEmbeddable.class
			).getSingleResult().getSubChildOneProp() ).isEqualTo( 2.0 );
			assertThat( session.createQuery(
					"select t.id from TestEntity t join t.embeddables e where treat(e as SubChildOneEmbeddable).subChildOneProp = 2.0",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
			assertThat( session.createQuery(
					"select t.id from TestEntity t join treat(t.embeddables as SubChildOneEmbeddable) e where e.subChildOneProp = 2.0",
					Long.class
			).getSingleResult() ).isEqualTo( 2L );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity testEntity1 = new TestEntity( 1L );
			testEntity1.getEmbeddables().add( new ChildOneEmbeddable( "embeddable_1", 1 ) );
			testEntity1.getEmbeddables().add( new ParentEmbeddable( "embeddable_3" ) );
			session.persist( testEntity1 );
			final TestEntity testEntity2 = new TestEntity( 2L );
			testEntity2.getEmbeddables().add( new SubChildOneEmbeddable( "embeddable_2", 2, 2.0 ) );
			session.persist( testEntity2 );
			final TestEntity testEntity3 = new TestEntity( 3L );
			testEntity3.getEmbeddables().add( new ChildTwoEmbeddable( "embeddable_4", 4L ) );
			testEntity3.getEmbeddables().add( new ChildOneEmbeddable( "embeddable_5", 5 ) );
			session.persist( testEntity3 );
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

		@ElementCollection
		private List<ParentEmbeddable> embeddables = new ArrayList<>();

		public TestEntity() {
		}

		public TestEntity(Long id) {
			this.id = id;
		}

		public List<ParentEmbeddable> getEmbeddables() {
			return embeddables;
		}
	}
}

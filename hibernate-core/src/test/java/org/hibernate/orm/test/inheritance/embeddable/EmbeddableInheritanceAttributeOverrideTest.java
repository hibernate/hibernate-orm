/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance.embeddable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		EmbeddableInheritanceAttributeOverrideTest.TestEntity.class,
		ParentEmbeddable.class,
		ChildOneEmbeddable.class,
		SubChildOneEmbeddable.class,
		ChildTwoEmbeddable.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class EmbeddableInheritanceAttributeOverrideTest {
	@Test
	public void testQuery(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final TestEntity result = session.createQuery(
					"from TestEntity where id = 1",
					TestEntity.class
			).getSingleResult();
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_disc", 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", 1 );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_1" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( ChildTwoEmbeddable.class );
			assertThat( ( (ChildTwoEmbeddable) result.getEmbeddable() ).getChildTwoProp() ).isEqualTo( 1L );
		} );
		inspector.clear();
		scope.inTransaction( session -> {
			final ParentEmbeddable result = session.createQuery(
					"select embeddable from TestEntity where id = 2",
					ParentEmbeddable.class
			).getSingleResult();
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_disc", 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", 1 );
			assertThat( result.getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result ).getSubChildOneProp() ).isEqualTo( 2.0 );
		} );
	}

	@Test
	public void testFindElementCollection(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final TestEntity testEntity = session.find( TestEntity.class, 1L );
			assertThat( testEntity.getEmbeddables() ).hasSize( 2 ).allSatisfy( e -> {
				if ( e instanceof ChildOneEmbeddable ) {
					assertThat( e ).isExactlyInstanceOf( ChildOneEmbeddable.class )
							.extracting( ParentEmbeddable::getParentProp ).isEqualTo( "collection_1" );
				}
				else {
					assertThat( e ).isExactlyInstanceOf( ChildTwoEmbeddable.class )
							.extracting( ParentEmbeddable::getParentProp ).isEqualTo( "collection_2" );
				}
			} );
			inspector.assertExecutedCount( 2 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "embeddables_disc", 1 );
			inspector.assertNumberOfOccurrenceInQueryNoSpace( 1, "child_one_col", 1 );
		} );
	}


	@Test
	public void testUpdate(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_3" );
			inspector.clear();
			// update values
			( (ChildTwoEmbeddable) result.getEmbeddable() ).setChildTwoProp( 33L );
		} );
		inspector.assertIsUpdate( 0 );
		inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "child_two_col", 1 );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			assertThat( ( (ChildTwoEmbeddable) result.getEmbeddable() ).getChildTwoProp() ).isEqualTo( 33L );
			inspector.clear();
			result.setEmbeddable( new SubChildOneEmbeddable( "embeddable_3_new", 3, 3.0 ) );
		} );
		inspector.assertIsUpdate( 0 );
		inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "embeddable_disc", 1 );
		inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "sub_child_one_col", 1 );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_3_new" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( SubChildOneEmbeddable.class );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getChildOneProp() ).isEqualTo( 3 );
			assertThat( ( (SubChildOneEmbeddable) result.getEmbeddable() ).getSubChildOneProp() ).isEqualTo( 3.0 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final TestEntity testEntity = new TestEntity( 1L, new ChildTwoEmbeddable( "embeddable_1", 1L ) );
			testEntity.getEmbeddables().add( new ChildOneEmbeddable( "collection_1", 1 ) );
			testEntity.getEmbeddables().add( new ChildTwoEmbeddable( "collection_2", 1L ) );
			session.persist( testEntity );
			session.persist( new TestEntity( 2L, new SubChildOneEmbeddable( "embeddable_2", 2, 2.0 ) ) );
			session.persist( new TestEntity( 3L, new ChildTwoEmbeddable( "embeddable_3", 3L ) ) );
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
		@AttributeOverride( name = "{discriminator}", column = @Column( name = "embeddable_disc" ) )
		@AttributeOverride( name = "subChildOneProp", column = @Column( name = "sub_child_one_col" ) )
		@AttributeOverride( name = "childTwoProp", column = @Column( name = "child_two_col" ) )
		private ParentEmbeddable embeddable;

		@ElementCollection
		@AttributeOverride( name = "{discriminator}", column = @Column( name = "embeddables_disc" ) )
		@AttributeOverride( name = "childOneProp", column = @Column( name = "child_one_col" ) )
		private List<ParentEmbeddable> embeddables = new ArrayList<>();

		public TestEntity() {
		}

		public TestEntity(Long id, ParentEmbeddable embeddable) {
			this.id = id;
			this.embeddable = embeddable;
		}

		public ParentEmbeddable getEmbeddable() {
			return embeddable;
		}

		public void setEmbeddable(ParentEmbeddable embeddable) {
			this.embeddable = embeddable;
		}

		public List<ParentEmbeddable> getEmbeddables() {
			return embeddables;
		}
	}
}

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

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		ParentEmbeddable.class,
		EmbeddableInheritanceAssciationsTest.TestEntity.class,
		EmbeddableInheritanceAssciationsTest.AssociatedEntity.class,
		EmbeddableInheritanceAssciationsTest.AssociationChildOne.class,
		EmbeddableInheritanceAssciationsTest.AssociationSubChildOne.class,
		EmbeddableInheritanceAssciationsTest.AssociationChildTwo.class,
		EmbeddableInheritanceAssciationsTest.AssociationChildThree.class,
} )
@SessionFactory
public class EmbeddableInheritanceAssciationsTest {
	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// insert
			final AssociatedEntity associated = session.find( AssociatedEntity.class, 1L );
			session.persist( new TestEntity( 1L, new AssociationChildOne( "embeddable_1", associated ) ) );
		} );
		scope.inTransaction( session -> {
			// queries
			final AssociatedEntity result = session.createQuery(
					"select embeddable.manyToOne from TestEntity where id = 1",
					AssociatedEntity.class
			).getSingleResult();
			assertThat( result.getId() ).isEqualTo( 1L );
			assertThat( result.getName() ).isEqualTo( "associated_1" );
		} );
		scope.inTransaction( session -> {
			// find
			final TestEntity result = session.find( TestEntity.class, 1L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_1" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( AssociationChildOne.class );
			final AssociationChildOne embeddable = (AssociationChildOne) result.getEmbeddable();
			assertThat( embeddable.getManyToOne().getId() ).isEqualTo( 1L );
			assertThat( embeddable.getManyToOne().getName() ).isEqualTo( "associated_1" );
			// update
			final AssociatedEntity newAssociated = new AssociatedEntity( 11L, "associated_1_new" );
			session.persist( newAssociated );
			embeddable.setManyToOne( newAssociated );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 1L );
			final AssociationChildOne embeddable = (AssociationChildOne) result.getEmbeddable();
			assertThat( embeddable.getManyToOne().getId() ).isEqualTo( 11L );
			assertThat( embeddable.getManyToOne().getName() ).isEqualTo( "associated_1_new" );
		} );
	}

	@Test
	public void testManyToOneSubtype(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// insert
			final AssociatedEntity associated = session.find( AssociatedEntity.class, 2L );
			session.persist( new TestEntity( 2L, new AssociationSubChildOne( "embeddable_2", associated ) ) );
		} );
		scope.inTransaction( session -> {
			// find
			final TestEntity result = session.find( TestEntity.class, 2L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_2" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( AssociationSubChildOne.class );
			final AssociationSubChildOne embeddable = (AssociationSubChildOne) result.getEmbeddable();
			assertThat( embeddable.getManyToOne().getId() ).isEqualTo( 2L );
			assertThat( embeddable.getManyToOne().getName() ).isEqualTo( "associated_2" );
		} );
	}

	@Test
	public void testManyToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// insert
			final AssociationChildTwo associationChildTwo = new AssociationChildTwo( "embeddable_3" );
			associationChildTwo.getManyToMany().add( session.find( AssociatedEntity.class, 3L ) );
			associationChildTwo.getManyToMany().add( session.find( AssociatedEntity.class, 4L ) );
			session.persist( new TestEntity( 3L, associationChildTwo ) );
		} );
		scope.inTransaction( session -> {
			// queries
			final List<AssociatedEntity> resultList = session.createQuery(
					"select embeddable.manyToMany from TestEntity",
					AssociatedEntity.class
			).getResultList();
			final Integer size = session.createQuery(
					"select size(embeddable.manyToMany) from TestEntity where id = 3",
					Integer.class
			).getSingleResult();
			assertThat( resultList ).hasSize( 2 ).hasSize( size )
					.extracting( AssociatedEntity::getName )
					.containsOnly( "associated_3", "associated_4" );
		} );
		scope.inTransaction( session -> {
			// find
			final TestEntity result = session.find( TestEntity.class, 3L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_3" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( AssociationChildTwo.class );
			final AssociationChildTwo embeddable = (AssociationChildTwo) result.getEmbeddable();
			assertThat( embeddable.getManyToMany() ).hasSize( 2 )
					.extracting( AssociatedEntity::getName )
					.containsOnly( "associated_3", "associated_4" );
			// update
			embeddable.getManyToMany().remove( 1 );
			final AssociatedEntity newAssociated = new AssociatedEntity( 44L, "associated_4_new" );
			session.persist( newAssociated );
			embeddable.getManyToMany().add( newAssociated );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 3L );
			final AssociationChildTwo embeddable = (AssociationChildTwo) result.getEmbeddable();
			assertThat( embeddable.getManyToMany() ).hasSize( 2 )
					.extracting( AssociatedEntity::getName )
					.containsOnly( "associated_3", "associated_4_new" );
		} );
	}

	@Test
	public void testOneToMany(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// insert
			final AssociationChildThree associationChildTwo = new AssociationChildThree( "embeddable_3" );
			associationChildTwo.getOneToMany().add( session.find( AssociatedEntity.class, 5L ) );
			associationChildTwo.getOneToMany().add( session.find( AssociatedEntity.class, 6L ) );
			session.persist( new TestEntity( 4L, associationChildTwo ) );
		} );
		scope.inTransaction( session -> {
			// queries
			final List<AssociatedEntity> resultList = session.createQuery(
					"select embeddable.oneToMany from TestEntity",
					AssociatedEntity.class
			).getResultList();
			final Integer size = session.createQuery(
					"select size(embeddable.oneToMany) from TestEntity where id = 4",
					Integer.class
			).getSingleResult();
			assertThat( resultList ).hasSize( 2 ).hasSize( size )
					.extracting( AssociatedEntity::getName )
					.containsOnly( "associated_5", "associated_6" );
		} );
		scope.inTransaction( session -> {
			// find
			final TestEntity result = session.find( TestEntity.class, 4L );
			assertThat( result.getEmbeddable().getParentProp() ).isEqualTo( "embeddable_3" );
			assertThat( result.getEmbeddable() ).isExactlyInstanceOf( AssociationChildThree.class );
			final AssociationChildThree embeddable = (AssociationChildThree) result.getEmbeddable();
			assertThat( embeddable.getOneToMany() ).hasSize( 2 )
					.extracting( AssociatedEntity::getName )
					.containsOnly( "associated_5", "associated_6" );
			// update
			embeddable.getOneToMany().remove( 0 );
			final AssociatedEntity newAssociated = new AssociatedEntity( 55L, "associated_5_new" );
			session.persist( newAssociated );
			embeddable.getOneToMany().add( 0, newAssociated );
		} );
		scope.inTransaction( session -> {
			final TestEntity result = session.find( TestEntity.class, 4L );
			final AssociationChildThree embeddable = (AssociationChildThree) result.getEmbeddable();
			assertThat( embeddable.getOneToMany() ).hasSize( 2 )
					.extracting( AssociatedEntity::getName )
					.containsOnly( "associated_5_new", "associated_6" );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new AssociatedEntity( 1L, "associated_1" ) );
			session.persist( new AssociatedEntity( 2L, "associated_2" ) );
			session.persist( new AssociatedEntity( 3L, "associated_3" ) );
			session.persist( new AssociatedEntity( 4L, "associated_4" ) );
			session.persist( new AssociatedEntity( 5L, "associated_5" ) );
			session.persist( new AssociatedEntity( 6L, "associated_6" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createQuery( "from TestEntity where size(embeddable.oneToMany) > 0", TestEntity.class )
					.getResultList()
					.forEach( t -> ( (AssociationChildThree) t.getEmbeddable() ).getOneToMany().clear() );
			session.flush();
			session.createMutationQuery( "delete from TestEntity" ).executeUpdate();
			session.createMutationQuery( "delete from AssociatedEntity" ).executeUpdate();
		} );
	}

	@Entity( name = "TestEntity" )
	static class TestEntity {
		@Id
		private Long id;

		@Embedded
		private ParentEmbeddable embeddable;

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
	}

	@Entity( name = "AssociatedEntity" )
	static class AssociatedEntity {
		@Id
		private Long id;

		private String name;

		public AssociatedEntity() {
		}

		public AssociatedEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_one" )
	static class AssociationChildOne extends ParentEmbeddable {
		@ManyToOne
		private AssociatedEntity manyToOne;

		public AssociationChildOne() {
		}

		public AssociationChildOne(String parentProp, AssociatedEntity manyToOne) {
			super( parentProp );
			this.manyToOne = manyToOne;
		}

		public AssociatedEntity getManyToOne() {
			return manyToOne;
		}

		public void setManyToOne(AssociatedEntity manyToOne) {
			this.manyToOne = manyToOne;
		}
	}

	@Embeddable
	@DiscriminatorValue( "sub_child_one" )
	static class AssociationSubChildOne extends AssociationChildOne {
		public AssociationSubChildOne() {
		}

		public AssociationSubChildOne(String parentProp, AssociatedEntity manyToOne) {
			super( parentProp, manyToOne );
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_two" )
	static class AssociationChildTwo extends ParentEmbeddable {
		@ManyToMany
		private List<AssociatedEntity> manyToMany = new ArrayList<>();

		public AssociationChildTwo() {
		}

		public AssociationChildTwo(String parentProp) {
			super( parentProp );
		}

		public List<AssociatedEntity> getManyToMany() {
			return manyToMany;
		}
	}

	@Embeddable
	@DiscriminatorValue( "child_three" )
	static class AssociationChildThree extends ParentEmbeddable {
		@OneToMany( fetch = FetchType.EAGER )
		@JoinColumn
		private List<AssociatedEntity> oneToMany = new ArrayList<>();

		public AssociationChildThree() {
		}

		public AssociationChildThree(String parentProp) {
			super( parentProp );
		}

		public List<AssociatedEntity> getOneToMany() {
			return oneToMany;
		}
	}
}

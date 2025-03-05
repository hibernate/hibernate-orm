/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.orm.test.jpa.metamodel.Thing;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity;
import org.hibernate.orm.test.jpa.metamodel.ThingWithQuantity_;
import org.hibernate.orm.test.jpa.ql.TreatKeywordTest.JoinedEntity;
import org.hibernate.orm.test.jpa.ql.TreatKeywordTest.JoinedEntitySubSubclass;
import org.hibernate.orm.test.jpa.ql.TreatKeywordTest.JoinedEntitySubSubclass2;
import org.hibernate.orm.test.jpa.ql.TreatKeywordTest.JoinedEntitySubclass;
import org.hibernate.orm.test.jpa.ql.TreatKeywordTest.JoinedEntitySubclass2;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {
				JoinedEntity.class, JoinedEntitySubclass.class, JoinedEntitySubSubclass.class,
				JoinedEntitySubclass2.class, JoinedEntitySubSubclass2.class,
				Animal.class, Elephant.class, Human.class, Thing.class, ThingWithQuantity.class,
				TreatKeywordTest.TreatAnimal.class, TreatKeywordTest.Dog.class, TreatKeywordTest.Dachshund.class, TreatKeywordTest.Greyhound.class,
				TreatKeywordTest.Person.class, TreatKeywordTest.Father.class, TreatKeywordTest.Mother.class, TreatKeywordTest.Grandmother.class
		}
)
public class TreatKeywordTest {

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Animal animal = new Animal();
					animal.setId( 100L );
					animal.setName( "2" );
					entityManager.persist( animal );
					Human human = new Human();
					human.setId( 200L );
					human.setName( "2" );
					entityManager.persist( human );
					Elephant elephant = new Elephant();
					elephant.setId( 300L );
					elephant.setName( "2" );
					entityManager.persist( elephant );
				}
		);
	}

	@Test
	public void basicTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Thing> criteria = builder.createQuery( Thing.class );
					Root<Thing> root = criteria.from( Thing.class );
					criteria.select( root );
					criteria.where(
							builder.equal(
									builder.treat( root, ThingWithQuantity.class ).get( ThingWithQuantity_.quantity ),
									2
							)
					);
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}

	@Test
	public void basicTest2(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Animal> criteria = builder.createQuery( Animal.class );
					Root<Animal> root = criteria.from( Animal.class );
					criteria.select( root );
					criteria.where(
							builder.equal(
									builder.treat( root, Human.class ).get( "name" ),
									"2"
							)
					);
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}

	@Test
	public void treatPathClassTest(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<String> criteria = builder.createQuery( String.class );
					Root<Animal> root = criteria.from( Animal.class );
					EntityType<Animal> Animal_ = entityManager.getMetamodel().entity( Animal.class );
					criteria.select( root.get( Animal_.getSingularAttribute( "name", String.class ) ) );

					criteria.where( builder.like( builder.treat( root, Human.class ).get( Human_.name ), "2%" ) );
					List<String> animalList = entityManager.createQuery( criteria ).getResultList();
					assertThat( animalList.size() ).as( "treat(Animal as Human) was ignored" ).isEqualTo( 1 )
					;

					CriteriaQuery<Long> idCriteria = builder.createQuery( Long.class );
					Root<Animal> idRoot = idCriteria.from( Animal.class );
					idCriteria.select( idRoot.get( Animal_.getSingularAttribute( "id", Long.class ) ) );

					idCriteria.where(
							builder.like(
									builder.treat( idRoot, Human.class )
											.get( Human_.name ), "2%"
							)
					);
					List<Long> animalIdList = entityManager.createQuery( idCriteria ).getResultList();
					assertThat( animalList.size() ).as( "treat(Animal as Human) was ignored" ).isEqualTo( 1 );
					assertThat( animalIdList.get( 0 ).longValue() ).isEqualTo( 200L );
				}
		);
	}

	@Test
	public void treatPathClassTestHqlControl(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					List<String> animalList = entityManager.createQuery(
									"select a.name from Animal a where treat (a as Human).name like '2%'", String.class )
							.getResultList();
					assertThat( animalList.size() ).as( "treat(Animal as Human) was ignored" ).isEqualTo( 1 );


					List<Long> animalIdList = entityManager.createQuery(
									"select a.id from Animal a where treat (a as Human).name like '2%'", Long.class )
							.getResultList();
					assertThat( animalList.size() ).as( "treat(Animal as Human) was ignored" ).isEqualTo( 1 );

					assertThat( animalIdList.get( 0 ).longValue() ).isEqualTo( 200L );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9549")
	public void treatRoot(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Human> criteria = builder.createQuery( Human.class );
					Root<Animal> root = criteria.from( Animal.class );
					criteria.select( builder.treat( root, Human.class ) );
					List<Human> humans = entityManager.createQuery( criteria ).getResultList();
					assertThat( humans.size() ).isEqualTo( 1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9549")
	public void treatRootReturnSuperclass(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Animal> criteria = builder.createQuery( Animal.class );
					Root<Animal> root = criteria.from( Animal.class );
					criteria.select( builder.treat( root, Human.class ) );
					List<Animal> animalsThatAreHuman = entityManager.createQuery( criteria ).getResultList();
					assertThat( animalsThatAreHuman.size() ).isEqualTo( 1 );
					assertThat( animalsThatAreHuman.get( 0 ) ).isInstanceOf( Human.class );
				}
		);
	}

	@Test
	public void testSelectSubclassPropertyFromDowncast(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
					Root<Thing> root = criteria.from( Thing.class );
					Root<ThingWithQuantity> subroot = builder.treat( root, ThingWithQuantity.class );
					criteria.select( subroot.<Integer>get( "quantity" ) );
					entityManager.createQuery( criteria ).getResultList();
				}
		);
	}


	@Test
	@JiraKey(value = "HHH-9411")
	public void testTreatWithRestrictionOnAbstractClass(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Greyhound greyhound = new Greyhound();
					Dachshund dachshund = new Dachshund();
					entityManager.persist( greyhound );
					entityManager.persist( dachshund );

					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<TreatAnimal> criteriaQuery = cb.createQuery( TreatAnimal.class );

					Root<TreatAnimal> animal = criteriaQuery.from( TreatAnimal.class );

					Root<Dog> dog = cb.treat( animal, Dog.class );

					// only fast dogs
					criteriaQuery.where( cb.isTrue( dog.<Boolean>get( "fast" ) ) );

					List<TreatAnimal> results = entityManager.createQuery( criteriaQuery ).getResultList();

					// we should only have a single Greyhound here, not slow long dogs!
					assertThat( results ).isEqualTo( List.of( greyhound ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-16657")
	public void testTypeFilterInSubquery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					JoinedEntitySubclass2 child1 = new JoinedEntitySubclass2( 3, "child1" );
					JoinedEntitySubSubclass2 child2 = new JoinedEntitySubSubclass2( 4, "child2" );
					JoinedEntitySubclass root1 = new JoinedEntitySubclass( 1, "root1", child1 );
					JoinedEntitySubSubclass root2 = new JoinedEntitySubSubclass( 2, "root2", child2 );
					entityManager.persist( child1 );
					entityManager.persist( child2 );
					entityManager.persist( root1 );
					entityManager.persist( root2 );

					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<String> query = cb.createQuery( String.class );
					Root<JoinedEntitySubclass> root = query.from( JoinedEntitySubclass.class );
					query.orderBy( cb.asc( root.get( "id" ) ) );
					Subquery<String> subquery = query.subquery( String.class );
					Root<JoinedEntitySubclass> subqueryRoot = subquery.correlate( root );
					Join<Object, Object> other = subqueryRoot.join( "other" );
					subquery.select( other.get( "name" ) );
					subquery.where( cb.equal( root.type(), cb.literal( JoinedEntitySubclass.class ) ) );
					query.select( subquery );

					List<String> results = entityManager.createQuery(
							"select (select o.name from j.other o where type(j) = JoinedEntitySubSubclass) from JoinedEntitySubclass j order by j.id",
							String.class
					).getResultList();

					assertThat( results.size() ).isEqualTo( 2 );
					assertThat( results.get( 0 ) ).isNull();
					assertThat( results.get( 1 ) ).isEqualTo( "child2" );
				}
		);
	}

	@Test
	@JiraKey("HHH-13765")
	public void treatRootSingleTableInheritance(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( new Person(1L, "Luigi") );

					entityManager.persist( new Mother(2L, "Anna") );

					entityManager.persist( new Grandmother(3L, "Elisabetta") );
				}
		);

		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder builder = entityManager.getCriteriaBuilder();
					CriteriaQuery<Person> criteria = builder.createQuery( Person.class );

					Root<Person> person = criteria.from( Person.class );
					criteria.select( person );
					criteria.where( builder.isNotNull( builder.treat( person, Mother.class ).get( "name" ) ) );

					List<Person> people = entityManager.createQuery( criteria ).getResultList();
					assertThat( people.size() ).isEqualTo( 2 );
				}
		);
	}

	@Entity
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	public static class Person {
		private Long id;
		private String name;

		public Person() {
		}

		public Person(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity
	public static class Father extends Person {
	}

	@Entity
	public static class Mother extends Person {

		public Mother() {
		}

		public Mother(Long id, String name) {
			super(id, name);
		}
	}

	@Entity
	public static class Grandmother extends Mother {
		public Grandmother() {
		}

		public Grandmother(Long id, String name) {
			super(id, name);
		}
	}

	@Entity(name = "TreatAnimal")
	public static abstract class TreatAnimal {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity(name = "Dog")
	public static abstract class Dog extends TreatAnimal {
		private boolean fast;

		protected Dog(boolean fast) {
			this.fast = fast;
		}

		public boolean isFast() {
			return fast;
		}

		public void setFast(boolean fast) {
			this.fast = fast;
		}
	}

	@Entity(name = "Dachshund")
	public static class Dachshund extends Dog {
		public Dachshund() {
			super( false );
		}
	}

	@Entity(name = "Greyhound")
	public static class Greyhound extends Dog {
		public Greyhound() {
			super( true );
		}
	}
}

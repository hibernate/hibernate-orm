/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import java.util.Arrays;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.metamodel.Thing;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity_;

import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class TreatKeywordTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Animal.class, Elephant.class, Human.class, Thing.class, ThingWithQuantity.class,
				TreatAnimal.class, Dog.class, Dachshund.class, Greyhound.class
		};
	}

	@Test
	public void basicTest() {
		EntityManager em = getOrCreateEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Thing> criteria = builder.createQuery( Thing.class );
		Root<Thing> root = criteria.from( Thing.class );
		criteria.select( root );
		criteria.where(
				builder.equal(
						builder.treat( root, ThingWithQuantity.class ).get( ThingWithQuantity_.quantity ),
						2
				)
		);
		em.createQuery( criteria ).getResultList();
		em.close();
	}

	@Test
	public void basicTest2() {
		EntityManager em = getOrCreateEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Animal> criteria = builder.createQuery( Animal.class );
		Root<Animal> root = criteria.from( Animal.class );
		criteria.select( root );
		criteria.where(
				builder.equal(
						builder.treat( root, Human.class ).get( "name" ),
						"2"
				)
		);
		em.createQuery( criteria ).getResultList();
		em.close();
	}

	@Test
	public void treatPathClassTest() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Animal animal = new Animal();
		animal.setId(100L);
		animal.setName("2");
		em.persist(animal);
		Human human = new Human();
		human.setId(200L);
		human.setName("2");
		em.persist(human);
		Elephant elephant = new Elephant();
		elephant.setId( 300L );
		elephant.setName( "2" );
		em.persist( elephant );
		em.getTransaction().commit();

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<String> criteria = builder.createQuery( String.class );
		Root<Animal> root = criteria.from( Animal.class );
		EntityType<Animal> Animal_ = em.getMetamodel().entity(Animal.class);
		criteria.select(root.get(Animal_.getSingularAttribute("name", String.class)));

		criteria.where(builder.like(builder.treat(root, Human.class).get( org.hibernate.jpa.test.criteria.Human_.name ), "2%"));
		List<String> animalList = em.createQuery( criteria ).getResultList();
		Assert.assertEquals("treat(Animal as Human) was ignored",1, animalList.size());

		CriteriaQuery<Long> idCriteria = builder.createQuery( Long.class );
		Root<Animal> idRoot = idCriteria.from( Animal.class );
		idCriteria.select( idRoot.get( Animal_.getSingularAttribute( "id", Long.class ) ) );

		idCriteria.where(
				builder.like(
						builder.treat( idRoot, Human.class )
								.get( org.hibernate.jpa.test.criteria.Human_.name ), "2%"
				)
		);
		List<Long> animalIdList = em.createQuery( idCriteria ).getResultList();
		Assert.assertEquals( "treat(Animal as Human) was ignored", 1, animalIdList.size() );
		Assert.assertEquals( 200L, animalIdList.get( 0 ).longValue() );

		em.close();
	}

	@Test
	public void treatPathClassTestHqlControl() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Animal animal = new Animal();
		animal.setId(100L);
		animal.setName("2");
		em.persist( animal );
		Human human = new Human();
		human.setId(200L);
		human.setName("2");
		em.persist(human);
		Elephant elephant = new Elephant();
		elephant.setId( 300L );
		elephant.setName( "2" );
		em.persist( elephant );
		em.getTransaction().commit();

		List<String> animalList = em.createQuery( "select a.name from Animal a where treat (a as Human).name like '2%'" ).getResultList();
		Assert.assertEquals( "treat(Animal as Human) was ignored", 1, animalList.size() );

		List<Long> animalIdList = em.createQuery( "select a.id from Animal a where treat (a as Human).name like '2%'" ).getResultList();
		Assert.assertEquals("treat(Animal as Human) was ignored",1, animalList.size());
		Assert.assertEquals( 200L, animalIdList.get( 0 ).longValue() );

		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9549" )
	public void treatRoot() {
		EntityManager em = getOrCreateEntityManager();

		em.getTransaction().begin();
		Animal animal = new Animal();
		animal.setId(100L);
		animal.setName("2");
		em.persist(animal);
		Human human = new Human();
		human.setId(200L);
		human.setName("2");
		em.persist(human);
		Elephant elephant = new Elephant();
		elephant.setId( 300L );
		elephant.setName( "2" );
		em.persist( elephant );
		em.getTransaction().commit();

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Human> criteria = builder.createQuery( Human.class );
		Root<Animal> root = criteria.from( Animal.class );
		criteria.select( builder.treat( root, Human.class ) );
		List<Human> humans = em.createQuery( criteria ).getResultList();
		Assert.assertEquals( 1, humans.size() );

		em.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9549" )
	public void treatRootReturnSuperclass() {
		EntityManager em = getOrCreateEntityManager();

		em.getTransaction().begin();
		Animal animal = new Animal();
		animal.setId(100L);
		animal.setName("2");
		em.persist(animal);
		Human human = new Human();
		human.setId(200L);
		human.setName("2");
		em.persist(human);
		Elephant elephant = new Elephant();
		elephant.setId( 300L );
		elephant.setName( "2" );
		em.persist( elephant );
		em.getTransaction().commit();

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Animal> criteria = builder.createQuery( Animal.class );
		Root<Animal> root = criteria.from( Animal.class );
		criteria.select( builder.treat( root, Human.class ) );
		List<Animal> animalsThatAreHuman = em.createQuery( criteria ).getResultList();
		Assert.assertEquals( 1, animalsThatAreHuman.size() );
		Assert.assertTrue( Human.class.isInstance( animalsThatAreHuman.get( 0 ) ) );

		em.close();
	}

	@Test
	public void testSelectSubclassPropertyFromDowncast() {
		EntityManager em = getOrCreateEntityManager();
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Integer> criteria = builder.createQuery( Integer.class );
		Root<Thing> root = criteria.from( Thing.class );
		Root<ThingWithQuantity> subroot = builder.treat( root, ThingWithQuantity.class );
		criteria.select( subroot.<Integer>get( "quantity" ) );
		em.createQuery( criteria ).getResultList();
		em.close();
	}


	@Test
	@TestForIssue(jiraKey = "HHH-9411")
	public void testTreatWithRestrictionOnAbstractClass() {
		EntityManager em = getOrCreateEntityManager();
		EntityTransaction entityTransaction = em.getTransaction();
		entityTransaction.begin();

		Greyhound greyhound = new Greyhound();
		Dachshund dachshund = new Dachshund();
		em.persist( greyhound );
		em.persist( dachshund );

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<TreatAnimal> criteriaQuery = cb.createQuery( TreatAnimal.class );

		Root<TreatAnimal> animal = criteriaQuery.from( TreatAnimal.class );

		Root<Dog> dog = cb.treat( animal, Dog.class );

		// only fast dogs
		criteriaQuery.where( cb.isTrue( dog.<Boolean>get( "fast" ) ) );

		List<TreatAnimal> results = em.createQuery( criteriaQuery ).getResultList();

		// we should only have a single Greyhound here, not slow long dogs!
		assertEquals( Arrays.asList( greyhound ), results );

		entityTransaction.commit();
		em.close();
	}

	@Entity
	public static abstract class TreatAnimal {
		@Id
		@GeneratedValue
		private Long id;
	}

	@Entity
	public static abstract class Dog extends TreatAnimal {
		private boolean fast;

		protected Dog(boolean fast) {
			this.fast = fast;
		}

		public final boolean isFast() {
			return fast;
		}
	}

	@Entity
	public static class Dachshund extends Dog {
		public Dachshund() {
			super( false );
		}
	}

	@Entity
	public static class Greyhound extends Dog {
		public Greyhound() {
			super( true );
		}
	}
}

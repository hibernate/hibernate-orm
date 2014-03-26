/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.criteria;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.metamodel.Thing;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity_;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class TreatKeywordTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Animal.class, Human.class, Thing.class, ThingWithQuantity.class };
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
		em.getTransaction().commit();

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<String> criteria = builder.createQuery( String.class );
		Root<Animal> root = criteria.from( Animal.class );
		EntityType<Animal> Animal_ = em.getMetamodel().entity(Animal.class);
		criteria.select(root.get(Animal_.getSingularAttribute("name", String.class)));

		criteria.where(builder.like(builder.treat(root, Human.class).get(org.hibernate.jpa.test.criteria.Human_.name), "2%"));
		List<String> animalList = em.createQuery( criteria ).getResultList();
		Assert.assertEquals("treat(Animal as Human) was ignored",1, animalList.size());

		em.close();
	}

	@Test
	public void treatPathClassTestHqlControl() {
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
		em.getTransaction().commit();

		List<String> animalList = em.createQuery( "select a.name from Animal a where treat (a as Human).name like '2%'" ).getResultList();
		Assert.assertEquals("treat(Animal as Human) was ignored",1, animalList.size());

		em.close();
	}



}

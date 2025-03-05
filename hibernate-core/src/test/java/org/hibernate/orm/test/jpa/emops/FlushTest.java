/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.emops;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@Jpa(annotatedClasses = {
		Pet.class,
		Dog.class,
		Cat.class,
		Decorate.class
})
public class FlushTest {
	private static Set<String> names = namesSet();

	private static Set<String> namesSet() {
		HashSet<String> names = new HashSet<String>();
		names.add( "Toonses" );
		names.add( "Sox" );
		names.add( "Winnie" );
		names.add( "Junior" );
		return names;
	}

	@Test
	@JiraKey(value = "EJBTHREE-722")
	public void testFlushOnDetached(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					try {
						entityManager.getTransaction().begin();
						Pet p1 = createCat( "Toonses", 15.0, 9, entityManager );
						entityManager.flush();
						entityManager.clear();

						Pet p2 = createCat( "Sox", 10.0, 5, entityManager );
						entityManager.flush();
						entityManager.clear();

						Pet p3 = createDog( "Winnie", 70.0, 5, entityManager );
						entityManager.flush();
						entityManager.clear();

						Pet p4 = createDog( "Junior", 11.0, 1, entityManager );
						entityManager.flush();
						entityManager.clear();

						Decorate d1 = createDecorate( "Test", p1, entityManager );
						entityManager.flush();
						entityManager.clear();

						Decorate d2 = createDecorate( "Test2", p2, entityManager );
						entityManager.flush();
						entityManager.clear();

						List l = findByWeight( 14.0, entityManager );
						entityManager.flush();
						entityManager.clear();
						for ( Object o : l ) {
							Assertions.assertTrue( names.contains( ( (Pet) o ).getName() ) );
						}

						Collection<Decorate> founds = getDecorate( entityManager );
						entityManager.flush();
						entityManager.clear();
						for ( Decorate value : founds ) {
							Assertions.assertTrue( names.contains( value.getPet().getName() ) );
						}
						entityManager.getTransaction().rollback();
					}
					catch (Exception e) {
						if ( entityManager.getTransaction().isActive() ) {
							entityManager.getTransaction().rollback();
						}
						throw e;
					}
				}
		);
	}

	private Dog createDog(String name, double weight, int bones, EntityManager entityManager) {
		Dog dog = new Dog();
		dog.setName( name );
		dog.setWeight( weight );
		dog.setNumBones( bones );
		entityManager.persist( dog );
		return dog;
	}

	private Cat createCat(String name, double weight, int lives, EntityManager entityManager) {
		Cat cat = new Cat();
		cat.setName( name );
		cat.setWeight( weight );
		cat.setLives( lives );
		entityManager.persist( cat );
		return cat;
	}

	private List findByWeight(double weight, EntityManager entityManager) {
		return entityManager.createQuery(
				"select p from Pet p where p.weight < :weight" ).setParameter(
				"weight", weight ).getResultList();
	}

	private Decorate createDecorate(String name, Pet pet, EntityManager entityManager) {
		Decorate dec = new Decorate();
		dec.setName( name );
		dec.setPet( pet );
		entityManager.persist( dec );
		return dec;
	}

	private Collection<Decorate> getDecorate(EntityManager entityManager) {
		Collection<Decorate> founds = new ArrayList<Decorate>();
		Query query = entityManager.createQuery( "SELECT o FROM Decorate o" );
		List list = query.getResultList();
		for ( Object obj : list ) {
			if ( obj instanceof Decorate ) {
				Decorate decorate = (Decorate) obj;
				founds.add( decorate );
				decorate.getPet().getName(); //load
			}
		}
		return founds;
	}
}

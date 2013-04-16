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

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.metamodel.Thing;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity;
import org.hibernate.jpa.test.metamodel.ThingWithQuantity_;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

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

	@Entity
	@Table( name = "ANIMAL" )
	public static class Animal {
		private Long id;
		private Animal mother;
		private Animal father;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@ManyToOne
		public Animal getMother() {
			return mother;
		}

		public void setMother(Animal mother) {
			this.mother = mother;
		}

		@ManyToOne
		public Animal getFather() {
			return father;
		}

		public void setFather(Animal father) {
			this.father = father;
		}
	}

	@Entity
	@Table( name = "HUMAN" )
	public static class Human extends Animal {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}

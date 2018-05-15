/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.criteria.paths;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.jpa.test.metamodel.Entity1;
import org.hibernate.jpa.test.metamodel.Entity1_;
import org.hibernate.jpa.test.metamodel.Entity2;
import org.hibernate.jpa.test.metamodel.Entity2_;

/**
 * @author Gail Badner
 */
public class FetchAndJoinTest extends AbstractMetamodelSpecificTest {

	@Test
	public void testImplicitJoinFromExplicitCollectionJoin() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		final CriteriaBuilder builder = em.getCriteriaBuilder();
		final CriteriaQuery<Entity1> criteria = builder.createQuery(Entity1.class);

		final Root<Entity1> root = criteria.from(Entity1.class);
		final Join<Entity1, Entity2> entity2Join = root.join( Entity1_.entity2, JoinType.INNER); // illegal with fetch join

		final Fetch<Entity1, Entity2> entity2Fetch = root.fetch(Entity1_.entity2, JoinType.INNER); // <=== REMOVE
		entity2Fetch.fetch( Entity2_.entity3 ); // <=== REMOVE

		criteria.where(builder.equal(root.get(Entity1_.value), "test"),
				builder.equal(entity2Join.get(Entity2_.value), "test")); // illegal with fetch join

		em.createQuery(criteria).getResultList();

		em.getTransaction().commit();
		em.close();
	}
}

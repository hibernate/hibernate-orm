/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;

import org.hibernate.orm.test.jpa.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.orm.test.jpa.metamodel.Entity1;
import org.hibernate.orm.test.jpa.metamodel.Entity1_;
import org.hibernate.orm.test.jpa.metamodel.Entity2;
import org.hibernate.orm.test.jpa.metamodel.Entity2_;

import org.junit.jupiter.api.Test;

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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Jpa(annotatedClasses = {
		EntityWithCompositeId.class,
		CompositeId.class
})
public class CountEntityWithCompositeIdTest {

	@Test
	public void shouldCount(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Long> cq = cb.createQuery(Long.class);
					Root<EntityWithCompositeId> r = cq.from(EntityWithCompositeId.class);
					cq.multiselect(cb.count(r));
					assertThat(entityManager.createQuery(cq).getSingleResult().intValue(), is(0));
				}
		);
		scope.inTransaction(
				entityManager -> {
					HibernateCriteriaBuilder cb = (HibernateCriteriaBuilder) entityManager.getCriteriaBuilder();
					CriteriaQuery<Long> cq = cb.createQuery(Long.class);
					cq.from(EntityWithCompositeId.class);
					cq.select(cb.count());
					assertThat(entityManager.createQuery(cq).getSingleResult().intValue(), is(0));
				}
		);
	}
}

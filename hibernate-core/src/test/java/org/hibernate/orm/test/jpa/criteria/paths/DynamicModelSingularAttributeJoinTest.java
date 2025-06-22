/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * Test query paths (both HQL and Criteria) with dynamic models
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(
		integrationSettings = {@Setting(name = AvailableSettings.JPA_METAMODEL_POPULATION, value = "enabled")},
		xmlMappings = {"org/hibernate/orm/test/jpa/criteria/paths/dynamic-model.hbm.xml"}
)
public class DynamicModelSingularAttributeJoinTest {
	@Test
	void testHql(EntityManagerFactoryScope scope) {
		scope.inTransaction( (entityManager) -> {
			entityManager.createQuery( "select d.id from Distribution d" ).getResultList();
			entityManager.createQuery( "select p.id from Policy p" ).getResultList();
			entityManager.createQuery( "select p.id from Distribution d join d.policy p" ).getResultList();
		} );
	}

	@Test
	public void testCriteria(EntityManagerFactoryScope scope) {
		final EntityType<?> distributionType = scope.getEntityManagerFactory().getMetamodel().entity( "Distribution" );
		final EntityType<?> policyType = scope.getEntityManagerFactory().getMetamodel().entity( "Policy" );

		scope.inEntityManager( (entityManager) -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery( Integer.class );
			final Root<?> root = criteriaQuery.from( distributionType );
			final Path<Integer> distributionId = root.get( "id" );
			criteriaQuery.select( distributionId );

			entityManager.createQuery( criteriaQuery ).getResultList();

		} );

		scope.inEntityManager( (entityManager) -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery( Integer.class );
			final Root<?> root = criteriaQuery.from( policyType );
			final Path<Integer> policyId = root.get( "id" );
			criteriaQuery.select( policyId );

			entityManager.createQuery( criteriaQuery ).getResultList();
		} );

		scope.inEntityManager( (entityManager) -> {
			final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

			final CriteriaQuery<Integer> criteriaQuery = criteriaBuilder.createQuery( Integer.class );
			final Root<?> root = criteriaQuery.from( distributionType );
			final From<?, ?> join = root.join( "policy" );
			final Path<Integer> policyId = join.get( "id" );
			criteriaQuery.select( policyId );

			entityManager.createQuery( criteriaQuery ).getResultList();
		} );
	}
}

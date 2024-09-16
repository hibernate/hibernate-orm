/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.criteria.paths;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Path;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.Test;

/**
 * @author Brad Koehn
 */
@Jpa(
		integrationSettings = { @Setting(name = AvailableSettings.JPA_METAMODEL_POPULATION, value = "enabled") },
		xmlMappings = { "org/hibernate/orm/test/jpa/criteria/paths/PolicyAndDistribution.hbm.xml" }
)
public class SingularAttributeJoinTest {

	@Test
	public void testEntityModeMapJoinCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
					jakarta.persistence.metamodel.EntityType distributionEntity = getEntityType(scope, "Distribution");
					From distributionFrom = criteriaQuery.from(distributionEntity);
					From policyJoin = distributionFrom.join("policy");
					Path policyId = policyJoin.get("policyId");
					criteriaQuery.select(policyId);
					TypedQuery typedQuery = entityManager.createQuery(criteriaQuery);
				}
		);
	}

	private jakarta.persistence.metamodel.EntityType getEntityType(EntityManagerFactoryScope scope, String entityName) {
		for(jakarta.persistence.metamodel.EntityType entityType : scope.getEntityManagerFactory().getMetamodel().getEntities()) {
			if (entityType.getName().equals("Distribution")) {
				return entityType;
			}
		}
		throw new IllegalStateException("Unable to find entity " + entityName);
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.paths;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;

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
                    javax.persistence.metamodel.EntityType distributionEntity = getEntityType(scope, "Distribution");
                    From distributionFrom = criteriaQuery.from(distributionEntity);
                    From policyJoin = distributionFrom.join("policy");
                    Path policyId = policyJoin.get("policyId");
                    criteriaQuery.select(policyId);
                    TypedQuery typedQuery = entityManager.createQuery(criteriaQuery);
                }
        );
    }

    private javax.persistence.metamodel.EntityType getEntityType(EntityManagerFactoryScope scope, String entityName) {
        for(javax.persistence.metamodel.EntityType entityType : scope.getEntityManagerFactory().getMetamodel().getEntities()) {
            if (entityType.getName().equals("Distribution")) {
                return entityType;
            }
        }
        throw new IllegalStateException("Unable to find entity " + entityName);
    }
}

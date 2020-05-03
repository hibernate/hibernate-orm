/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria.paths;

import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.Type;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.criteria.internal.CriteriaBuilderImpl;
import org.hibernate.query.criteria.internal.PathSource;
import org.hibernate.query.criteria.internal.path.SingularAttributeJoin;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * @author Brad Koehn
 */
public class SingularAttributeJoinTest extends BaseEntityManagerFunctionalTestCase {
    @Override
    protected String[] getMappings() {
        return new String[] {
                getClass().getPackage().getName().replace( '.', '/' ) + "/PolicyAndDistribution.hbm.xml"
        };
    }

    @Override
    protected void addConfigOptions(Map options) {
        super.addConfigOptions( options );

        // make sure that dynamic-map mode entity types are returned in the metamodel.
        options.put( AvailableSettings.JPA_METAMODEL_POPULATION, "enabled" );
    }

    /**
     * When building a join from a non-class based entity (EntityMode.MAP), make sure you get the Bindable from
     * the SingularAttribute as the join model. If you don't, you'll get the first non-classed based entity
     * you added to your configuration. Regression for HHH-9142.
     */
    @Test
    public void testEntityModeMapJoins() throws Exception {
        CriteriaBuilderImpl criteriaBuilder = mock( CriteriaBuilderImpl.class);
        PathSource pathSource = mock( PathSource.class);
        SingularAttribute joinAttribute = mock( SingularAttribute.class);
        when(joinAttribute.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.MANY_TO_ONE);
        Type joinType = mock( Type.class, withSettings().extraInterfaces( Bindable.class));
        when(joinAttribute.getType()).thenReturn(joinType);
        SingularAttributeJoin join = new SingularAttributeJoin(criteriaBuilder, null, pathSource, joinAttribute, JoinType.LEFT);

        assertEquals( joinType, join.getModel());
    }

    @Test
    public void testEntityModeMapJoinCriteriaQuery() throws Exception {
        final EntityManager entityManager = entityManagerFactory().createEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        jakarta.persistence.metamodel.EntityType distributionEntity = getEntityType("Distribution");
        From distributionFrom = criteriaQuery.from(distributionEntity);
        From policyJoin = distributionFrom.join("policy");
        Path policyId = policyJoin.get("policyId");
        criteriaQuery.select(policyId);
        TypedQuery typedQuery = entityManager.createQuery(criteriaQuery);
//        typedQuery.getResultList();
    }

    private jakarta.persistence.metamodel.EntityType getEntityType(String entityName) {
        for(jakarta.persistence.metamodel.EntityType entityType : entityManagerFactory().getMetamodel().getEntities()) {
            if (entityType.getName().equals("Distribution")) {
                return entityType;
            }
        }

        throw new IllegalStateException("Unable to find entity " + entityName);
    }
}

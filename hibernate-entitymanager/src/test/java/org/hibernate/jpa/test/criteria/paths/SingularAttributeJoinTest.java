/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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

import org.hibernate.EntityMode;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Mappings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.criteria.CriteriaBuilderImpl;
import org.hibernate.jpa.criteria.PathSource;
import org.hibernate.jpa.criteria.path.SingularAttributeJoin;
import org.hibernate.jpa.test.metamodel.AbstractMetamodelSpecificTest;
import org.hibernate.mapping.*;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.hibernate.tuple.entity.DynamicMapEntityTuplizer;
import org.hibernate.type.EntityType;
import org.junit.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Brad Koehn
 */
public class SingularAttributeJoinTest extends AbstractMetamodelSpecificTest {

    /**
     * When building a join from a non-class based entity (EntityMode.MAP), make sure you get the Bindable from
     * the SingularAttribute as the join model. If you don't, you'll get the first non-classed based entity
     * you added to your configuration. Regression for HHH-9142.
     */
    @Test
    public void testEntityModeMapJoins() throws Exception {
        CriteriaBuilderImpl criteriaBuilder = mock(CriteriaBuilderImpl.class);
        PathSource pathSource = mock(PathSource.class);
        SingularAttribute joinAttribute = mock(SingularAttribute.class);
        when(joinAttribute.getPersistentAttributeType()).thenReturn(Attribute.PersistentAttributeType.MANY_TO_ONE);
        Type joinType = mock(Type.class, withSettings().extraInterfaces(Bindable.class));
        when(joinAttribute.getType()).thenReturn(joinType);
        SingularAttributeJoin join = new SingularAttributeJoin(criteriaBuilder, null, pathSource, joinAttribute, JoinType.LEFT);

        assertEquals(joinType, join.getModel());
    }

    @Test
    public void testEntityModeMapJoinCriteriaQuery() throws Exception {
        final EntityManager entityManager = entityManagerFactory().createEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery();
        javax.persistence.metamodel.EntityType distributionEntity = getEntityType("Distribution");
        From distributionFrom = criteriaQuery.from(distributionEntity);
        From policyJoin = distributionFrom.join("policy");
        Path policyId = policyJoin.get("policyId");
        criteriaQuery.select(policyId);
        TypedQuery typedQuery = entityManager.createQuery(criteriaQuery);
//        typedQuery.getResultList();
    }

    private javax.persistence.metamodel.EntityType getEntityType(String entityName) {
        for(javax.persistence.metamodel.EntityType entityType : entityManagerFactory().getMetamodel().getEntities()) {
            if (entityType.getName().equals("Distribution")) {
                return entityType;
            }
        }

        throw new IllegalStateException("Unable to find entity " + entityName);
    }

    @Override
    protected Map buildSettings() {
        Map<Object, Object> config = super.buildSettings();

        IntegratorProvider integratorProvider = getIntegratorProvider();
        config.put(EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER, integratorProvider);

        return config;
    }

    private IntegratorProvider getIntegratorProvider() {
        return new IntegratorProvider() {

            @Override
            public List<Integrator> getIntegrators() {
                return Arrays.asList(getIntegrator());
            }
        };
    }

    private Integrator getIntegrator() {
        return new Integrator() {

            @Override
            public void integrate(Configuration configuration, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
                Mappings mappings = configuration.createMappings();
                addPolicy(mappings);
                addDistribution(mappings);
            }

            @Override
            public void integrate(MetadataImplementor metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

            }

            @Override
            public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

            }
        };
    }

    private void addPolicy(Mappings mappings) {
        RootClass policyRootClass = new RootClass();
        policyRootClass.addTuplizer(EntityMode.MAP, DynamicMapEntityTuplizer.class.getName());
        policyRootClass.setJpaEntityName("Policy");
        policyRootClass.setEntityName("Policy");
        Table policyTable = new Table("POLICY_TABLE");
        PrimaryKey policyPrimaryKey = new PrimaryKey();
        policyTable.setPrimaryKey(policyPrimaryKey);
        Column policyIdColumn = new Column("policyId");
        policyIdColumn.setSqlTypeCode(Types.INTEGER);
        policyTable.addColumn(policyIdColumn);
        SimpleValue identifierValue = new SimpleValue(mappings, policyTable);
        identifierValue.setTypeName("int");
        identifierValue.addColumn(policyIdColumn);
        identifierValue.setIdentifierGeneratorStrategy("assigned");
        policyPrimaryKey.addColumn(policyIdColumn);
        policyRootClass.setIdentifier(identifierValue);
        Property policyIdProperty = new Property();
        policyIdProperty.setValue(identifierValue);
        policyIdProperty.setName("policyId");
        policyRootClass.addProperty(policyIdProperty);

        policyRootClass.setTable(policyTable);
        mappings.addClass(policyRootClass);
        mappings.addTable(null, null, policyTable.getName(), null, false);
    }

    private void addDistribution(Mappings mappings) {
        RootClass distRootClass = new RootClass();
        distRootClass.addTuplizer(EntityMode.MAP, DynamicMapEntityTuplizer.class.getName());
        distRootClass.setEntityName("Distribution");
        distRootClass.setJpaEntityName("Distribution");
        Table distTable = new Table("DISTRIBUTION_TABLE");
        PrimaryKey distPrimaryKey = new PrimaryKey();
        distTable.setPrimaryKey(distPrimaryKey);
        Column distIdColumn = new Column("distributionId");
        distIdColumn.setSqlTypeCode(Types.INTEGER);
        distTable.addColumn(distIdColumn);
        SimpleValue identifierValue = new SimpleValue(mappings, distTable);
        identifierValue.setTypeName("int");
        identifierValue.addColumn(distIdColumn);
        identifierValue.setIdentifierGeneratorStrategy("assigned");
        distPrimaryKey.addColumn(distIdColumn);
        distRootClass.setIdentifier(identifierValue);
        Property distIdProperty = new Property();
        distIdProperty.setValue(identifierValue);
        distIdProperty.setName("distributionId");
        distRootClass.addProperty(distIdProperty);
        Property policyProperty = new Property();
        policyProperty.setName("policy");
        ManyToOne policyValue = new ManyToOne(mappings, distTable);
        policyProperty.setValue(policyValue);
        policyValue.setTypeName("Policy");
        policyValue.setReferencedEntityName("Policy");
        Column policyColumn = new Column("policyId");
        policyValue.addColumn(policyColumn);
        policyColumn.setSqlTypeCode(Types.INTEGER);
        distRootClass.addProperty(policyProperty);


        distRootClass.setTable(distTable);
        mappings.addClass(distRootClass);
        mappings.addTable(null, null, distTable.getName(), null, false);
    }

}

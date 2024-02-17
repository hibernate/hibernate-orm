/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.persister.entity.EntityPersister;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-9937")
@RunWith( BytecodeEnhancerRunner.class )
public class LazyBasicFieldNotInitializedTest extends BaseCoreFunctionalTestCase {

    private Long entityId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{TestEntity.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, false );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, true );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity entity = new TestEntity();
            entity.description = "desc";
            s.persist( entity );
            entityId = entity.id;
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity entity = s.get( TestEntity.class, entityId );
            Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );

            EntityPersister entityPersister = sessionFactory().getRuntimeMetamodels()
                    .getMappingMetamodel()
                    .getEntityDescriptor( TestEntity.class );

            boolean[] propertyLaziness = entityPersister.getPropertyLaziness();
            assertEquals( 1, propertyLaziness.length );
            assertTrue( propertyLaziness[0] );

            // Make sure NonIdentifierAttribute#isLazy is consistent (HHH-10551)
            final AttributeMapping theBytesAttr = entityPersister.findAttributeMapping( "description" );
            assertThat( theBytesAttr ).isInstanceOf( BasicValuedModelPart.class );
            assertThat( theBytesAttr.getMappedFetchOptions().getTiming() ).isEqualTo( FetchTiming.DELAYED );
        } );
    }

    // --- //
    
    @Entity(name = "TestEntity")
    @Table( name = "TEST_ENTITY" )
    private static class TestEntity {

        @Id
        @GeneratedValue
        Long id;

        @Basic( fetch = FetchType.LAZY )
        String description;
    }
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.transaction.TransactionUtil.JPATransactionVoidFunction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Map;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@TestForIssue( jiraKey = "HHH-14244" )
@RunWith( BytecodeEnhancerRunner.class )
public class TestDirtyCheckOnLazy extends BaseEntityManagerFunctionalTestCase {

    private EntityWithLazyProperty entity;

    @Override
    public Class[] getAnnotatedClasses() {
        return new Class[]{EntityWithLazyProperty.class};
    }

    @Override
    protected void addConfigOptions(Map options) {
        options.put( AvailableSettings.CLASSLOADERS, getClass().getClassLoader() );
    }

    @Before
    public void prepare() throws Exception {
        EntityPersister ep = entityManagerFactory().getMetamodel().entityPersister( EntityWithLazyProperty.class.getName() );
        assertTrue( ep.getBytecodeEnhancementMetadata().isEnhancedForLazyLoading() );

        byte[] testArray = new byte[]{0x2A};

        doInJPA( this::entityManagerFactory, em -> {
            //persist the test entity.d
            entity = new EntityWithLazyProperty();
            entity.setSomeField( "TEST" );
            entity.setLazyData( testArray );
            em.persist( entity );
        } );

        checkLazyField( entity, testArray );
    }

    /**
     * Check the dirty check is actually taking place, preventing update of the lazy field
     */
    @Test
    public void testNoUpdate() {
        doInJPA( this::entityManagerFactory, new JPATransactionVoidFunction() {
            @Override
            public void accept(EntityManager em) {
                entity = em.find( EntityWithLazyProperty.class, entity.id );
                assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
                entity.setSomeField( "TEST1" );
            }

            @Override
            public void afterTransactionCompletion() {
                assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
            }
        } );
    }

    /**
     * Update a lazy field to a value and check the value is read back.
     */
    @Test
    public void testUpdate() {
        byte[] testArray = new byte[]{0x44};

        doInJPA( this::entityManagerFactory, new JPATransactionVoidFunction() {
            @Override
            public void accept(EntityManager em) {
                entity = em.find( EntityWithLazyProperty.class, entity.id );
                assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
                entity.setLazyData(testArray);
            }

            @Override
            public void afterTransactionCompletion() {
                assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
            }
        } );

        checkLazyField( entity, testArray );
    }

    /**
     * Update a lazy field to null and check the value is read back.
     */
    @Test
    public void testUpdateToNull() {
        byte[] testArray = null;

        doInJPA( this::entityManagerFactory, new JPATransactionVoidFunction() {
            @Override
            public void accept(EntityManager em) {
                entity = em.find( EntityWithLazyProperty.class, entity.id );
                assertFalse( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
                entity.setLazyData(testArray);
            }

            @Override
            public void afterTransactionCompletion() {
                assertTrue( Hibernate.isPropertyInitialized( entity, "lazyData" ) );
            }
        } );

        checkLazyField( entity, testArray );
    }


    private void checkLazyField(EntityWithLazyProperty entity, byte[] expected) {
        // reload the entity and check the lazy value matches what we expect.
        doInJPA( this::entityManagerFactory, em -> {
            EntityWithLazyProperty testEntity = em.find( EntityWithLazyProperty.class, entity.id );
            assertFalse( Hibernate.isPropertyInitialized( testEntity, "lazyData" ) );
            assertTrue( Arrays.equals( expected, testEntity.lazyData ) );
            assertTrue( Hibernate.isPropertyInitialized( testEntity, "lazyData" ) );
        } );
    }

    // --- //

    /**
     * Test entity with a lazy property which requires build time instrumentation.
     */
    @Entity
    @Table( name = "ENTITY_WITH_LAZY_PROPERTY" )
    private static class EntityWithLazyProperty {
        @Id
        @GeneratedValue
        private Long id;

        @Basic( fetch = FetchType.LAZY )
        private byte[] lazyData;

        private String someField;

        public void setLazyData(byte[] lazyData) {
            this.lazyData = lazyData;
        }

        public void setSomeField(String someField) {
            this.someField = someField;
        }
    }
}

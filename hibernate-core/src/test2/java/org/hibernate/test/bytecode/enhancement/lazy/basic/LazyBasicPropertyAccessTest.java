/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.basic;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@RunWith( BytecodeEnhancerRunner.class )
public class LazyBasicPropertyAccessTest extends BaseCoreFunctionalTestCase {

    private LazyEntity entity;

    private Long entityId;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{LazyEntity.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            LazyEntity entity = new LazyEntity();
            entity.description = "desc";
            s.persist( entity );
            entityId = entity.id;
        } );
    }

    @Test
    public void execute() {
        doInHibernate( this::sessionFactory, s -> {
            entity = s.get( LazyEntity.class, entityId );

            Assert.assertFalse( isPropertyInitialized( entity, "description" ) );
            checkDirtyTracking( entity );

            assertEquals( "desc", entity.description );
            assertTrue( isPropertyInitialized( entity, "description" ) );
        } );

        doInHibernate( this::sessionFactory, s -> {
            entity.description = "desc1";
            s.update( entity );

            // Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );
            checkDirtyTracking( entity, "description" );

            assertEquals( "desc1", entity.description );
            assertTrue( isPropertyInitialized( entity, "description" ) );
        } );

        doInHibernate( this::sessionFactory, s -> {
            entity = s.get( LazyEntity.class, entityId );
            assertEquals( "desc1", entity.description );
        } );

        doInHibernate( this::sessionFactory, s -> {
            entity.description = "desc2";
            LazyEntity mergedEntity = (LazyEntity) s.merge( entity );

            //Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );
            checkDirtyTracking( mergedEntity, "description" );

            assertEquals( "desc2", mergedEntity.description );
            assertTrue( isPropertyInitialized( mergedEntity, "description" ) );
        } );

        doInHibernate( this::sessionFactory, s -> {
            LazyEntity entity = s.get( LazyEntity.class, entityId );
            assertEquals( "desc2", entity.description );
        } );
    }

    // --- //

    @Entity
    @Access( AccessType.FIELD )
    @Table( name = "LAZY_PROPERTY_ENTITY" )
    private static class LazyEntity {

        @Id
        @GeneratedValue
        Long id;

        @Basic( fetch = FetchType.LAZY )
        String description;
    }
}

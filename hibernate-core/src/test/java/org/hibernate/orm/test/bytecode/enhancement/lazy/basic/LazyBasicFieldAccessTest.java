/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.basic;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@DomainModel(
        annotatedClasses = {
                LazyBasicFieldAccessTest.LazyEntity.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
                @Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
        }
)
@SessionFactory
@BytecodeEnhanced
public class LazyBasicFieldAccessTest {

    private LazyEntity entity;

    private Long entityId;

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            LazyEntity entity = new LazyEntity();
            entity.description = "desc";
            s.persist( entity );
            entityId = entity.id;
        } );
    }

    @Test
    public void testAttachedUpdate(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            entity = s.get( LazyEntity.class, entityId );

            assertFalse( isPropertyInitialized( entity, "description" ) );
            checkDirtyTracking( entity );

            assertEquals( "desc", entity.description );
            assertTrue( isPropertyInitialized( entity, "description" ) );
        } );

        scope.inTransaction( s -> {
            entity = s.get( LazyEntity.class, entityId );
            assertFalse( isPropertyInitialized( entity, "description" ) );
            entity.description = "desc1";

            checkDirtyTracking( entity, "description" );

            assertEquals( "desc1", entity.description );
            assertTrue( isPropertyInitialized( entity, "description" ) );
        } );

        scope.inTransaction( s -> {
            entity = s.get( LazyEntity.class, entityId );
            assertEquals( "desc1", entity.description );
        } );
    }

    @Test
    @JiraKey("HHH-11882")
    public void testDetachedUpdate(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            entity = s.get( LazyEntity.class, entityId );

            assertFalse( isPropertyInitialized( entity, "description" ) );
            checkDirtyTracking( entity );

            assertEquals( "desc", entity.description );
            assertTrue( isPropertyInitialized( entity, "description" ) );
        } );

        scope.inTransaction( s -> {
            entity.description = "desc1";
            s.update( entity );

            checkDirtyTracking( entity, "description" );

            assertEquals( "desc1", entity.description );
            assertTrue( isPropertyInitialized( entity, "description" ) );
        } );

        scope.inTransaction( s -> {
            entity = s.get( LazyEntity.class, entityId );
            assertEquals( "desc1", entity.description );
        } );

        scope.inTransaction( s -> {
            entity.description = "desc2";
            LazyEntity mergedEntity = (LazyEntity) s.merge( entity );

            //Assert.assertFalse( Hibernate.isPropertyInitialized( entity, "description" ) );
            checkDirtyTracking( mergedEntity, "description" );

            assertEquals( "desc2", mergedEntity.description );
            assertTrue( isPropertyInitialized( mergedEntity, "description" ) );
        } );

        scope.inTransaction( s -> {
            entity = s.get( LazyEntity.class, entityId );
            assertEquals( "desc2", entity.description );
        } );
    }

    // --- //

    @Entity
    @Access( AccessType.FIELD )
    @Table( name = "LAZY_PROPERTY_ENTITY" )
    static class LazyEntity {

        @Id
        @GeneratedValue
        Long id;

        @Basic( fetch = FetchType.LAZY )
        String description;
    }
}

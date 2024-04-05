/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.group;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.getFieldByReflection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 */
@JiraKey("HHH-11155")
@JiraKey("HHH-11506")
@DomainModel(
        annotatedClasses = {
                SimpleLazyGroupUpdateTest.TestEntity.class
        }
)
@ServiceRegistry(
        settings = {
                @Setting(name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false"),
                @Setting(name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true"),
        }
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ EnhancerTestContext.class, SimpleLazyGroupUpdateTest.NoDirtyCheckingContext.class })
public class SimpleLazyGroupUpdateTest {

    public static final String REALLY_BIG_STRING = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    @BeforeEach
    public void prepare(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            s.save( new TestEntity( 1L, "entity 1", "blah", REALLY_BIG_STRING ) );
        } );
    }

    @Test
    public void test(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            TestEntity entity = s.get( TestEntity.class, 1L );
            assertLoaded( entity, "name" );
            assertNotLoaded( entity, "lifeStory" );
            assertNotLoaded( entity, "reallyBigString" );

            entity.lifeStory = "blah blah blah";
            assertLoaded( entity, "name" );
            assertLoaded( entity, "lifeStory" );
            assertNotLoaded( entity, "reallyBigString" );
        } );

        scope.inTransaction( s -> {
            TestEntity entity = s.get( TestEntity.class, 1L );

            assertLoaded( entity, "name" );
            assertNotLoaded( entity, "lifeStory" );
            assertNotLoaded( entity, "reallyBigString" );
            assertEquals( "blah blah blah", entity.lifeStory );
            assertEquals( REALLY_BIG_STRING, entity.reallyBigString );
        } );
    }

    private void assertLoaded(Object owner, String name) {
        // NOTE we assume null == not-loaded
        Object fieldByReflection = getFieldByReflection( owner, name );
        assertNotNull( fieldByReflection, "Expecting field '" + name + "' to be loaded, but it was not" );
    }

    private void assertNotLoaded(Object owner, String name) {
        // NOTE we assume null == not-loaded
        Object fieldByReflection = getFieldByReflection( owner, name );
        assertNull( fieldByReflection, "Expecting field '" + name + "' to be not loaded, but it was" );
    }

    @AfterEach
    public void cleanup(SessionFactoryScope scope) {
        scope.inTransaction( s -> {
            s.createQuery( "delete TestEntity" ).executeUpdate();
        } );
    }

    // --- //

    @Entity( name = "TestEntity" )
    @Table( name = "TEST_ENTITY" )
    static class TestEntity {

        @Id
        Long id;

        String name;

        @Basic( fetch = FetchType.LAZY )
        @LazyGroup( "grp1" )
        String lifeStory;

        @Basic( fetch = FetchType.LAZY )
        @LazyGroup( "grp2" )
        String reallyBigString;

        TestEntity() {
        }

        TestEntity(Long id, String name, String lifeStory, String reallyBigString) {
            this.id = id;
            this.name = name;
            this.lifeStory = lifeStory;
            this.reallyBigString = reallyBigString;
        }
    }

    // --- //

    public static class NoDirtyCheckingContext extends EnhancerTestContext {

        @Override
        public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
            return false;
        }
    }
}

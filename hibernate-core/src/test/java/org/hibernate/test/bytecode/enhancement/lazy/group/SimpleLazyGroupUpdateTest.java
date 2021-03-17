/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import org.hibernate.annotations.LazyGroup;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.getFieldByReflection;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-11155, HHH-11506" )
@RunWith( BytecodeEnhancerRunner.class )
@CustomEnhancementContext( {EnhancerTestContext.class, SimpleLazyGroupUpdateTest.NoDirtyCheckingContext.class} )
public class SimpleLazyGroupUpdateTest extends BaseCoreFunctionalTestCase {

    public static final String REALLY_BIG_STRING = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class[]{TestEntity.class};
    }

    @Override
    protected void configure(Configuration configuration) {
        configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
        configuration.setProperty( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            s.save( new TestEntity( 1L, "entity 1", "blah", REALLY_BIG_STRING ) );
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity entity = s.get( TestEntity.class, 1L );
            assertLoaded( entity, "name" );
            assertNotLoaded( entity, "lifeStory" );
            assertNotLoaded( entity, "reallyBigString" );

            entity.lifeStory = "blah blah blah";
            assertLoaded( entity, "name" );
            assertLoaded( entity, "lifeStory" );
            assertNotLoaded( entity, "reallyBigString" );
        } );

        doInHibernate( this::sessionFactory, s -> {
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
        assertNotNull( "Expecting field '" + name + "' to be loaded, but it was not", fieldByReflection );
    }

    private void assertNotLoaded(Object owner, String name) {
        // NOTE we assume null == not-loaded
        Object fieldByReflection = getFieldByReflection( owner, name );
        assertNull( "Expecting field '" + name + "' to be not loaded, but it was", fieldByReflection );
    }

    @After
    public void cleanup() {
        doInHibernate( this::sessionFactory, s -> {
            s.createQuery( "delete TestEntity" ).executeUpdate();
        } );
    }

    // --- //

    @Entity( name = "TestEntity" )
    @Table( name = "TEST_ENTITY" )
    private static class TestEntity {

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

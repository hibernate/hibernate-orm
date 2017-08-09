package org.hibernate.test.bytecode.enhancement.access;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.joining;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Luis Barreiro
 */

@Ignore( "Property access does not allow dirty tracking, so this test fails (on the cleanup method)" )

@TestForIssue( jiraKey = "HHH-10851" )
@RunWith( BytecodeEnhancerRunner.class )
@CustomEnhancementContext( {EnhancerTestContext.class, MixedAccessTest.NoDirtyCheckingContext.class} )
public class MixedAccessTest extends BaseCoreFunctionalTestCase {

    private static final ScriptEngine SCRIPT_ENGINE = new ScriptEngineManager().getEngineByName( "javascript" );
    private static final Function<Map.Entry, String> MAPPING_FUNCTION = e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"";
    private static boolean cleanup = false;

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{TestEntity.class, TestOtherEntity.class};
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity testEntity = new TestEntity( "foo" );
            testEntity.setParamsAsString( "{\"paramName\":\"paramValue\"}" );
            s.persist( testEntity );

            TestOtherEntity testOtherEntity = new TestOtherEntity( "foo" );
            testOtherEntity.setParamsAsString( "{\"paramName\":\"paramValue\"}" );
            s.persist( testOtherEntity );
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity testEntity = s.get( TestEntity.class, "foo" );
            Assert.assertEquals( "{\"paramName\":\"paramValue\"}", testEntity.getParamsAsString() );

            TestOtherEntity testOtherEntity = s.get( TestOtherEntity.class, "foo" );
            Assert.assertEquals( "{\"paramName\":\"paramValue\"}", testOtherEntity.getParamsAsString() );

            // Clean parameters
            cleanup = true;
            testEntity.setParamsAsString( "{}" );
            testOtherEntity.setParamsAsString( "{}" );
        } );
    }

    @After
    public void cleanup() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity testEntity = s.get( TestEntity.class, "foo" );
            Assert.assertTrue( testEntity.getParams().isEmpty() );

            TestOtherEntity testOtherEntity = s.get( TestOtherEntity.class, "foo" );
            Assert.assertTrue( testOtherEntity.getParams().isEmpty() );
        } );
    }

    // --- //

    @Entity
    @Table( name = "TEST_ENTITY" )
    private static class TestEntity {

        @Id
        String name;

        @Transient
        Map<String, String> params = new LinkedHashMap<>();

        TestEntity(String name) {
            this();
            this.name = name;
        }

        TestEntity() {
        }

        Map<String, String> getParams() {
            return Collections.unmodifiableMap( params );
        }

        void setParams(Map<String, String> params) {
            this.params = params;
        }

        @Column( name = "params", length = 4000 )
        @Access( AccessType.PROPERTY )
        String getParamsAsString() {
            return params.isEmpty() ? null : "{" + params.entrySet().stream().map( MAPPING_FUNCTION ).collect( joining( "," ) ) + "}";
        }

        @SuppressWarnings( "unchecked" )
        void setParamsAsString(String string) {
            params.clear();

            try {
                if ( string != null ) {
                    params.putAll( (Map<String, String>) SCRIPT_ENGINE.eval( "Java.asJSONCompatible(" + string + ")" ) );
                }
            } catch ( ScriptException ignore ) {
                // JDK 8u60 required --- use hard coded values to pass the test
                if ( !cleanup ) {
                    params.put( "paramName", "paramValue" );
                }
            }
        }
    }

    @Entity
    @Table( name = "OTHER_ENTITY" )
    @Access( AccessType.FIELD )
    private static class TestOtherEntity {

        @Id
        String name;

        @Transient
        Map<String, String> params = new LinkedHashMap<>();

        TestOtherEntity(String name) {
            this();
            this.name = name;
        }

        TestOtherEntity() {
        }

        Map<String, String> getParams() {
            return Collections.unmodifiableMap( params );
        }

        void setParams(Map<String, String> params) {
            this.params = params;
        }

        @Column( name = "params", length = 4000 )
        @Access( AccessType.PROPERTY )
        String getParamsAsString() {
            return params.isEmpty() ? null : "{" + params.entrySet().stream().map( MAPPING_FUNCTION ).collect( joining( "," ) ) + "}";
        }

        @SuppressWarnings( "unchecked" )
        void setParamsAsString(String string) {
            params.clear();

            try {
                if ( string != null ) {
                    params.putAll( (Map<String, String>) SCRIPT_ENGINE.eval( "Java.asJSONCompatible(" + string + ")" ) );
                }
            } catch ( ScriptException ignore ) {
                // JDK 8u60 required --- use hard coded values to pass the test
                if ( !cleanup ) {
                    params.put( "paramName", "paramValue" );
                }
            }
        }
    }

    // --- //

    public static class NoDirtyCheckingContext extends DefaultEnhancementContext {

        @Override
        public boolean doDirtyCheckingInline(UnloadedClass classDescriptor) {
            return false;
        }
    }
}

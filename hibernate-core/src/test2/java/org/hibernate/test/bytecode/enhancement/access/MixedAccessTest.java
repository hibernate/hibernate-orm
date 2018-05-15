package org.hibernate.test.bytecode.enhancement.access;

import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * Requires a custom enhancement context to disable dirty checking as bytecode enhancement is not expected to fully work with AccessType.PROPERTY
 * In particular, the properties changed are not marked dirty and therefore not updated in the DB (failing the checks in @After method)
 *
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-10851" )
@RunWith( BytecodeEnhancerRunner.class )
@CustomEnhancementContext( MixedAccessTest.NoDirtyCheckingContext.class )
public class MixedAccessTest extends BaseCoreFunctionalTestCase {

    private static final Pattern PARAM_PATTERN = Pattern.compile( "\\{\\\"(.*)\\\"\\:\\\"(.*)\\\"\\}" );
    private static final Function<Map.Entry, String> MAPPING_FUNCTION = e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"";
    private static final String ID = "foo", PARAM_KEY = "paramName", PARAM_VAL = "paramValue", PARAMS_AS_STR = "{\"" + PARAM_KEY + "\":\"" + PARAM_VAL + "\"}";

    @Override
    public Class<?>[] getAnnotatedClasses() {
        return new Class<?>[]{TestEntity.class, TestOtherEntity.class};
    }

    @Before
    public void prepare() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity testEntity = new TestEntity( ID );
            testEntity.setParamsAsString( PARAMS_AS_STR );
            s.persist( testEntity );

            TestOtherEntity testOtherEntity = new TestOtherEntity( ID );
            testOtherEntity.setParamsAsString( PARAMS_AS_STR );
            s.persist( testOtherEntity );
        } );
    }

    @Test
    public void test() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity testEntity = s.get( TestEntity.class, ID );
            Assert.assertEquals( PARAMS_AS_STR, testEntity.getParamsAsString() );

            TestOtherEntity testOtherEntity = s.get( TestOtherEntity.class, ID );
            Assert.assertEquals( PARAMS_AS_STR, testOtherEntity.getParamsAsString() );

            // Clean parameters
            testEntity.setParamsAsString( "{}" );
            testOtherEntity.setParamsAsString( "{}" );
        } );
    }

    @After
    public void cleanup() {
        doInHibernate( this::sessionFactory, s -> {
            TestEntity testEntity = s.get( TestEntity.class, ID );
            Assert.assertTrue( testEntity.getParams().isEmpty() );

            TestOtherEntity testOtherEntity = s.get( TestOtherEntity.class, ID );
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
            this.params.clear();
            this.params.putAll( params );
        }

        @Column( name = "params", length = 4000 )
        @Access( AccessType.PROPERTY )
        String getParamsAsString() {
            return "{" + params.entrySet().stream().map( MAPPING_FUNCTION ).collect( joining( "," ) ) + "}";
        }

        @SuppressWarnings( "unchecked" )
        void setParamsAsString(String string) {
            Matcher matcher = PARAM_PATTERN.matcher( string );

            params.clear();
            if ( matcher.matches() && matcher.groupCount() > 1 ) {
                params.put( matcher.group( 1 ), matcher.group( 2 ) );
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
            this.params.clear();
            this.params.putAll( params );
        }

        @Column( name = "params", length = 4000 )
        @Access( AccessType.PROPERTY )
        String getParamsAsString() {
            return "{" + params.entrySet().stream().map( MAPPING_FUNCTION ).collect( joining( "," ) ) + "}";
        }

        @SuppressWarnings( "unchecked" )
        void setParamsAsString(String string) {
            Matcher matcher = PARAM_PATTERN.matcher( string );

            params.clear();
            if ( matcher.matches() && matcher.groupCount() > 1 ) {
                params.put( matcher.group( 1 ), matcher.group( 2 ) );
            }
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

package org.hibernate.test.bytecode.enhancement.basic;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Version;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.clearDirtyTracking;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assume.assumeTrue;

/**
 * @author Luis Barreiro
 * @author Craig Andrews
 */
@TestForIssue( jiraKey = "HHH-11284" )
@RunWith( BytecodeEnhancerRunner.class )
@CustomEnhancementContext( {EnhancerTestContext.class, InheritedTest.EagerEnhancementContext.class} )
public class InheritedTest {

    @Test
    public void test() {
        Employee charles = new Employee( "Charles", "Engineer" );
        charles.setOca( 1002 );

        // Check that both types of class attributes are being dirty tracked
        checkDirtyTracking( charles, "title", "oca" );
        clearDirtyTracking( charles );

        // Let's give charles a promotion, this time using method references
        charles.setOca( 99 );
        charles.setTitle( "Manager" );

        checkDirtyTracking( charles, "title", "oca" );

        Contractor bob = new Contractor( "Bob", 100 );
        bob.setOca( 1003 );

        // Check that both types of class attributes are being dirty tracked
        checkDirtyTracking( bob, "rate", "oca" );
        clearDirtyTracking( bob );

        // Let's give bob a rate increase, this time using method references
        bob.setOca( 88 );
        bob.setRate( 200 );

        checkDirtyTracking( bob, "rate", "oca" );
    }

    // Adapted from BasicEnhancementTest#basicExtendedEnhancementTest
    @Test
    @TestForIssue(jiraKey = "HHH-14006")
    public void extendedEnhancementTest() {
        // This test only works if lazy loading bytecode enhancement is enabled,
        // otherwise extended bytecode enhancement does not do anything we can check.
        assumeTrue( PersistentAttributeInterceptable.class.isAssignableFrom( Employee.class ) );

        Employee entity = new Employee();
        ( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );

        Object decoy = new Object();
        // This accesses "anUnspecifiedObject" on a variable of type Employee,
        // but "anUnspecifiedObject" is defined the superclass Person.
        // Such "virtual" access used to break extended bytecode enhancement.
        entity.anUnspecifiedObject = decoy;

        Object gotByReflection = EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" );
        assertNotSame( decoy, gotByReflection );
        assertSame( ObjectAttributeMarkerInterceptor.WRITE_MARKER, gotByReflection );

        Object gotByEnhancedDirectAccess = entity.anUnspecifiedObject;
        assertNotSame( decoy, gotByEnhancedDirectAccess );
        assertSame( ObjectAttributeMarkerInterceptor.READ_MARKER, gotByEnhancedDirectAccess );
    }

    // --- //

    @Entity
    private static abstract class Person {

        Object anUnspecifiedObject;

        @Id
        String name;

        @Version
        long oca;

        Person() {
        }

        Person(String name) {
            this();
            this.name = name;
        }

        void setOca(long l) {
            this.oca = l;
        }
    }

    @Entity
    private static class Employee extends Person {

        String title;

        Employee() {
        }

        Employee(String name, String title) {
            super( name );
            this.title = title;
        }

        void setTitle(String title) {
            this.title = title;
        }
    }

    @Entity
    private static class Contractor extends Person {

        Integer rate;

        Contractor() {
        }

        Contractor(String name, Integer rate) {
            super( name );
            this.rate = rate;
        }

        void setRate(Integer rate) {
            this.rate = rate;
        }
    }

    // --- //

    public static class EagerEnhancementContext extends DefaultEnhancementContext {
        @Override
        public boolean hasLazyLoadableAttributes(UnloadedClass classDescriptor) {
            // HHH-10981 - Without lazy loading, the generation of getters and setters has a different code path
            return false;
        }
    }
}

package org.hibernate.orm.test.bytecode.enhancement.basic;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.clearDirtyTracking;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Luis Barreiro
 */
@JiraKey( "HHH-10646" )
@BytecodeEnhanced
@CustomEnhancementContext( {EnhancerTestContext.class, MappedSuperclassTest.EagerEnhancementContext.class} )
public class MappedSuperclassTest {

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
    }

    // Adapted from BasicEnhancementTest#basicExtendedEnhancementTest
    @Test
    @JiraKey("HHH-14006")
    public void extendedEnhancementTest() {
        // This test only works if lazy loading bytecode enhancement is enabled,
        // otherwise extended bytecode enhancement does not do anything we can check.
        assumeTrue( PersistentAttributeInterceptable.class.isAssignableFrom( Employee.class ) );

        Employee entity = new Employee();
        ( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );

        Object decoy = new Object();
        // This accesses "name" on a variable of type Employee,
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

    @MappedSuperclass
    private static class Person {

        Object anUnspecifiedObject;

        @Id
        String name;

        @Version
        Long oca;

        Person(String name) {
            this.name = name;
        }

        Person() {
        }

        void setOca(long l) {
            this.oca = l;
        }
    }

    @Entity
    private static class Employee extends Person {

        String title;

        Employee(String name, String title) {
            super( name );
            this.title = title;
        }

        Employee() {
        }

        void setTitle(String title) {
            this.title = title;
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

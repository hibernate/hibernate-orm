package org.hibernate.test.bytecode.enhancement.basic;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestContext;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.clearDirtyTracking;

/**
 * @author Luis Barreiro
 */
@TestForIssue( jiraKey = "HHH-10646" )
@RunWith( BytecodeEnhancerRunner.class )
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

    // --- //

    @MappedSuperclass
    private static class Person {

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

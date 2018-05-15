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
import javax.persistence.Version;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.clearDirtyTracking;

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

    // --- //

    @Entity
    private static abstract class Person {

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

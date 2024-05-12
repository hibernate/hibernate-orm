/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.association;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

/**
 * @author Luis Barreiro
 */
@BytecodeEnhanced
public class OneToOneAssociationTest {

    @Test
    public void test() {
        User user = new User();
        user.setLogin( SafeRandomUUIDGenerator.safeRandomUUIDAsString() );

        Customer customer = new Customer();
        customer.setUser( user );

        assertEquals( customer, user.getCustomer() );

        // check dirty tracking is set automatically with bi-directional association management
        EnhancerTestUtils.checkDirtyTracking( user, "login", "customer" );

        User anotherUser = new User();
        anotherUser.setLogin( SafeRandomUUIDGenerator.safeRandomUUIDAsString() );

        customer.setUser( anotherUser );

        assertNull( user.getCustomer() );
        assertEquals( customer, anotherUser.getCustomer() );

        user.setCustomer( new Customer() );

        assertEquals( user, user.getCustomer().getUser() );
    }

    @Test
    public void testSetNull() {
        User user = new User();
        user.setLogin( SafeRandomUUIDGenerator.safeRandomUUIDAsString() );

        Customer customer = new Customer();
        customer.setUser( user );

        assertEquals( customer, user.getCustomer() );

        // check dirty tracking is set automatically with bi-directional association management
        EnhancerTestUtils.checkDirtyTracking( user, "login", "customer" );

        user.setCustomer( null );

        assertNull( user.getCustomer() );
        assertNull( customer.getUser() );
    }

    // --- //

    @Entity
    private static class Customer {

        @Id
        Long id;

        @OneToOne
        User user;

        User getUser() {
            return user;
        }

        void setUser(User newUser) {
            user = newUser;
        }
    }

    @Entity
    private static class User {

        @Id
        Long id;

        String login;

        String password;

        @OneToOne( mappedBy = "user" )
        Customer customer;

        void setLogin(String login) {
            this.login = login;
        }

        Customer getCustomer() {
            return customer;
        }

        void setCustomer(Customer customer) {
            this.customer = customer;
        }
    }
}

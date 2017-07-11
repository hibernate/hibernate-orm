/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.util.UUID;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class OneToOneAssociationTest {

    @Test
    public void test() {
        User user = new User();
        user.setLogin( UUID.randomUUID().toString() );

        Customer customer = new Customer();
        customer.setUser( user );

        Assert.assertEquals( customer, user.getCustomer() );

        // check dirty tracking is set automatically with bi-directional association management
        EnhancerTestUtils.checkDirtyTracking( user, "login", "customer" );

        User anotherUser = new User();
        anotherUser.setLogin( UUID.randomUUID().toString() );

        customer.setUser( anotherUser );

        Assert.assertNull( user.getCustomer() );
        Assert.assertEquals( customer, anotherUser.getCustomer() );

        user.setCustomer( new Customer() );

        Assert.assertEquals( user, user.getCustomer().getUser() );
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

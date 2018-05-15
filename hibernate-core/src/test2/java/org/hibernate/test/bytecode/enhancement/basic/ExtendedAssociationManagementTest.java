/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.util.UUID;

import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.getFieldByReflection;
import static org.junit.Assert.assertEquals;

/**
 * @author Luis Barreiro
 */
@RunWith( BytecodeEnhancerRunner.class )
public class ExtendedAssociationManagementTest {

    @Test
    public void test() {
        User user = new User();
        user.login = UUID.randomUUID().toString();

        Customer customer = new Customer();
        customer.user = user;

        assertEquals( customer, getFieldByReflection( user, "customer" ) );

        // check dirty tracking is set automatically with bi-directional association management
        EnhancerTestUtils.checkDirtyTracking( user, "login", "customer" );

        User anotherUser = new User();
        anotherUser.login = UUID.randomUUID().toString();

        customer.user = anotherUser;

        Assert.assertNull( user.customer );
        assertEquals( customer, getFieldByReflection( anotherUser, "customer" ) );

        user.customer = new Customer();
        assertEquals( user, user.customer.user );
    }

    // --- //

    @Entity
    private static class Customer {

        @Id
        Long id;

        String firstName;

        String lastName;

        @OneToOne( fetch = FetchType.LAZY )
        User user;
    }

    @Entity
    private static class User {

        @Id
        Long id;

        String login;

        String password;

        @OneToOne( mappedBy = "user", fetch = FetchType.LAZY )
        Customer customer;
    }
}

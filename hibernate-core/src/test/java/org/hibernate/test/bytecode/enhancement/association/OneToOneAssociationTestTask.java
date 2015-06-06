/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.association;

import java.util.UUID;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.test.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class OneToOneAssociationTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Customer.class, User.class};
	}

	public void prepare() {
	}

	public void execute() {
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

	protected void cleanup() {
	}
}

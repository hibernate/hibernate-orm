/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.extended;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class ExtendedAssociationManagementTestTasK extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Customer.class, User.class};
	}

	public void prepare() {
	}

	public void execute() {
		User user = new User();
		user.login = UUID.randomUUID().toString();

		Customer customer = new Customer();
		customer.user = user;

		Assert.assertEquals( customer, EnhancerTestUtils.getFieldByReflection( user, "customer" ) );

		// check dirty tracking is set automatically with bi-directional association management
		EnhancerTestUtils.checkDirtyTracking( user, "login", "customer" );

		User anotherUser = new User();
		anotherUser.login = UUID.randomUUID().toString();

		customer.user = anotherUser;

		Assert.assertNull( user.customer );
		Assert.assertEquals( customer, EnhancerTestUtils.getFieldByReflection( anotherUser, "customer" ) );

		user.customer = new Customer();
		Assert.assertEquals( user, user.customer.user );
	}

	protected void cleanup() {
	}

	@Entity public class Customer {

		@Id public int id;

		@OneToOne(fetch = FetchType.LAZY) public User user;

		public String firstName;

		public String lastName;

		public int version;
	}

	@Entity public class User {

		@Id public int id;

		public String login;

		public String password;

		@OneToOne(mappedBy = "user", fetch = FetchType.LAZY) public Customer customer;
	}

}

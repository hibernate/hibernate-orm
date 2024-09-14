/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import org.hibernate.Hibernate;

import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class UserCollectionTypeAnnotationsVariantTest extends UserCollectionTypeTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { User.class, Email.class };
	}

	@Override
	protected void checkEmailAddressInitialization(User user) {
		assertTrue( Hibernate.isInitialized( user.getEmailAddresses() ) );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

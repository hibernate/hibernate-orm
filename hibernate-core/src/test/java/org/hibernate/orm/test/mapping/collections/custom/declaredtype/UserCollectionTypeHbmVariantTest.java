/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import org.hibernate.Hibernate;

import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
public class UserCollectionTypeHbmVariantTest extends UserCollectionTypeTest {
	@Override
	public String[] getMappings() {
		return new String[] { "mapping/collections/custom/declaredtype/UserPermissions.hbm.xml" };
	}

	@Override
	protected void checkEmailAddressInitialization(User user) {
		assertFalse( Hibernate.isInitialized( user.getEmailAddresses() ) );
	}
}

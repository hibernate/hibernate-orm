/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import org.hibernate.Hibernate;

import static org.junit.Assert.assertFalse;

/**
 * @author Steve Ebersole
 */
public class UserCollectionTypeHbmVariantTest extends UserCollectionTypeTest {
	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "/org/hibernate/orm/test/mapping/collections/custom/basic/UserPermissions.hbm.xml" };
	}

	@Override
	protected void checkEmailAddressInitialization(User user) {
		assertFalse( Hibernate.isInitialized( user.getEmailAddresses() ) );
	}
}

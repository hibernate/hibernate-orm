/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

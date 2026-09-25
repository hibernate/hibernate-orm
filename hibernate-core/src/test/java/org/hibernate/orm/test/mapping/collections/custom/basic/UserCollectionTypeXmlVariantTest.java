/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.basic;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = { "/org/hibernate/orm/test/mapping/collections/custom/basic/UserPermissions.orm.xml" },
		concurrencyStrategy = "nonstrict-read-write"
)
public class UserCollectionTypeXmlVariantTest extends UserCollectionTypeTest {

	@Override
	protected void checkEmailAddressInitialization(User user) {
		assertTrue( Hibernate.isInitialized( user.getEmailAddresses() ) );
	}

}

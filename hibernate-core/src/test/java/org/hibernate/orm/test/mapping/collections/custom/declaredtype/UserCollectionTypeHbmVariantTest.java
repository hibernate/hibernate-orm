/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = { "/org/hibernate/orm/test/mapping/collections/custom/declaredtype/UserPermissions.hbm.xml" },
		concurrencyStrategy = "nonstrict-read-write"
)
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.TRANSFORM_HBM_XML, value = "true")
)
public class UserCollectionTypeHbmVariantTest extends UserCollectionTypeTest {

	@Override
	protected void checkEmailAddressInitialization(User user) {
		assertFalse( Hibernate.isInitialized( user.getEmailAddresses() ) );
	}

}

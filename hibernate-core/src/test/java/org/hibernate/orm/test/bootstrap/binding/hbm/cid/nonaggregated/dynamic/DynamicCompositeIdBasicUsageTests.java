/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.hbm.cid.nonaggregated.dynamic;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.HBM2DDL_AUTO, value = "create-drop" )
)
@DomainModel( xmlMappings = "org/hibernate/orm/test/bootstrap/binding/hbm/cid/nonaggregated/dynamic/DynamicCompositeIdBasic.hbm.xml" )
@SessionFactory
public class DynamicCompositeIdBasicUsageTests {
	@Test
	public void testFullQueryReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "from DynamicCompositeIdBasic e where e.id.key1 = 1" ).list()
		);
	}

	@Test
	@FailureExpected( reason = "Do we want to allow this?" )
	public void testEmbeddedQueryReference(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "from DynamicCompositeIdBasic e where e.key1 = 1" ).list()
		);
	}
}

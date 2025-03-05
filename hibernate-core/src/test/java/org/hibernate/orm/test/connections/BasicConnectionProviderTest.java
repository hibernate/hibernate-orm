/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.connections;

import java.util.Map;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;

import org.hibernate.testing.RequiresDialect;

/**
 * Implementation of BasicConnectionProviderTest.
 *
 * @author Steve Ebersole
 */
@RequiresDialect(H2Dialect.class)
public class BasicConnectionProviderTest extends ConnectionManagementTestCase {
	@Override
	protected Session getSessionUnderTest() {
		Session session = openSession();
		session.beginTransaction();
		return session;
	}

	@Override
	protected void reconnect(Session session) {
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		super.addSettings( settings );
		settings.put( Environment.CONNECTION_HANDLING, PhysicalConnectionHandlingMode.DELAYED_ACQUISITION_AND_HOLD.toString() );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.Collections;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.boot.ServiceRegistryTestingImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class IdentifierHelperTest extends BaseUnitTestCase {
	@Test
	public void testAutoQuotingDisabled() {
		ServiceRegistry sr = ServiceRegistryTestingImpl.forUnitTesting(
				Collections.singletonMap(
						AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED,
						// true is the default, but to be sure...
						true
				)
		);
		Identifier identifier = sr.getService( JdbcEnvironment.class ).getIdentifierHelper().toIdentifier( "select" );
		assertTrue( identifier.isQuoted() );
		StandardServiceRegistryBuilder.destroy( sr );

		sr = ServiceRegistryTestingImpl.forUnitTesting(
				Collections.singletonMap(
						AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED,
						false
				)
		);
		identifier = sr.getService( JdbcEnvironment.class ).getIdentifierHelper().toIdentifier( "select" );
		assertFalse( identifier.isQuoted() );
		StandardServiceRegistryBuilder.destroy( sr );
	}
}

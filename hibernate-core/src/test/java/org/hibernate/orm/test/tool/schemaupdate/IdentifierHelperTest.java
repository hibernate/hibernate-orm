/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.Collections;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.naming.Identifier;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.boot.ServiceRegistryTestingImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Steve Ebersole
 */
public class IdentifierHelperTest {

	@Test
	public void testAutoQuotingDisabled() {
		boolean keyAutoquoitingEnabled = true;

		ServiceRegistry sr = createServiceRegistry( keyAutoquoitingEnabled );
		try {
			Identifier identifier = sr.getService( JdbcEnvironment.class )
					.getIdentifierHelper()
					.toIdentifier( "select" );
			assertTrue( identifier.isQuoted() );
			StandardServiceRegistryBuilder.destroy( sr );

			keyAutoquoitingEnabled = false;
			sr = createServiceRegistry( keyAutoquoitingEnabled );

			identifier = sr.getService( JdbcEnvironment.class ).getIdentifierHelper().toIdentifier( "select" );
			assertFalse( identifier.isQuoted() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( sr );
		}
	}

	private ServiceRegistryTestingImpl createServiceRegistry(boolean keyAutoquoitingEnabled) {
		return ServiceRegistryTestingImpl.forUnitTesting(
				Collections.singletonMap(
						AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED,
						// true is the default, but to be sure...
						keyAutoquoitingEnabled
				)
		);
	}
}

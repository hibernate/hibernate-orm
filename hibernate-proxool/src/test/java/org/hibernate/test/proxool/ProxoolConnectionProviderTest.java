/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.proxool;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.logicalcobwebs.proxool.ProxoolFacade;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.proxool.internal.ProxoolConnectionProvider;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify connection pools are closed, and that only the managed one is closed.
 *
 * @author Sanne Grinovero
 */
public class ProxoolConnectionProviderTest extends BaseUnitTestCase {

	@Test
	public void testPoolsClosed() {
		assertDefinedPools(); // zero-length-vararg used as parameter
		StandardServiceRegistry serviceRegistry = buildServiceRegistry( "pool-one" );
		ConnectionProvider providerOne = serviceRegistry.getService( ConnectionProvider.class );
		assertDefinedPools( "pool-one" );


		StandardServiceRegistry serviceRegistryTwo = buildServiceRegistry( "pool-two" );
		ConnectionProvider providerTwo = serviceRegistryTwo.getService( ConnectionProvider.class );
		assertDefinedPools( "pool-one", "pool-two" );
		
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
		assertDefinedPools( "pool-two" );

		StandardServiceRegistryBuilder.destroy( serviceRegistryTwo );
		assertDefinedPools();
	}

	private void assertDefinedPools(String... expectedPoolNames) {
		List<String> aliases = Arrays.asList( ProxoolFacade.getAliases() );
		assertEquals( expectedPoolNames.length,	aliases.size() );
		for (String poolName : expectedPoolNames) {
			assertTrue( "pool named " + poolName + " missing", aliases.contains( poolName ) );
		}
	}

	private StandardServiceRegistry buildServiceRegistry(String poolName){

		return new StandardServiceRegistryBuilder(  )
				.applySetting( Environment.PROXOOL_POOL_ALIAS, poolName )
				.applySetting( Environment.PROXOOL_PROPERTIES, poolName + ".properties" )
				.applySetting( Environment.CONNECTION_PROVIDER, ProxoolConnectionProvider.class.getName() )
				.build();

	}
}

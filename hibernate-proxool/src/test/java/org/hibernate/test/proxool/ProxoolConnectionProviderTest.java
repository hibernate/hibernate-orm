/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.naming;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class AcmeCorpPhysicalNamingStrategyTest {
	private AcmeCorpPhysicalNamingStrategy strategy = new AcmeCorpPhysicalNamingStrategy();
	private StandardServiceRegistry serviceRegistry;

	@Before
	public void prepareServiceRegistry() {
		serviceRegistry = new StandardServiceRegistryBuilder().build();
	}

	@After
	public void releaseServiceRegistry() {
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testTableNaming() {
		{
			Identifier in = Identifier.toIdentifier( "accountNumber" );
			Identifier out = strategy.toPhysicalTableName( in, serviceRegistry.getService( JdbcEnvironment.class ) );
			assertThat( out.getText(), equalTo( "acct_num" ) );

		}
	}
}

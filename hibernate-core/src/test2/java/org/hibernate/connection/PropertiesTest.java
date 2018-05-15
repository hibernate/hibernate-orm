/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.connection;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.engine.jdbc.connections.internal.ConnectionProviderInitiator;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author kbow
 */
public class PropertiesTest extends BaseUnitTestCase {
	@Test
	public void testProperties() throws Exception {
		final Properties props = new Properties();

		props.put("rpt.1.hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
		props.put("rpt.2.hibernate.connection.driver_class", "org.apache.derby.jdbc.ClientDriver");
		props.put("rpt.3.hibernate.connection.url", "jdbc:derby://localhost:1527/db/reports.db");
		props.put("rpt.4.hibernate.connection.username", "sa");
		props.put("rpt.5.hibernate.connection.password_enc", "76f271db3661fd50082e68d4b953fbee");
		props.put("rpt.6.hibernate.connection.password_enc", "76f271db3661fd50082e68d4b953fbee");
		props.put("hibernate.connection.create", "true");

		final Properties outputProps = ConnectionProviderInitiator.getConnectionProperties( props );
		Assert.assertEquals( 1, outputProps.size() );
		Assert.assertEquals( "true", outputProps.get( "create" ) );
	}

}

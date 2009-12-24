/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.connection;

import java.util.Properties;

import junit.framework.TestCase;

/**
 * Undocumented
 *
 * @author kbow
 */

public class PropertiesTest extends TestCase {
	public void testProperties() throws Exception {
		final Properties props = new Properties();

		props.put("rpt.1.hibernate.dialect", "org.hibernate.dialect.DerbyDialect");
		props.put("rpt.2.hibernate.connection.driver_class", "org.apache.derby.jdbc.ClientDriver");
		props.put("rpt.3.hibernate.connection.url", "jdbc:derby://localhost:1527/db/reports.db");
		props.put("rpt.4.hibernate.connection.username", "sa");
		props.put("rpt.5.hibernate.connection.password_enc", "76f271db3661fd50082e68d4b953fbee");
		props.put("rpt.6.hibernate.connection.password_enc", "76f271db3661fd50082e68d4b953fbee");
		props.put("hibernate.connection.create", "true");

		final Properties outputProps = ConnectionProviderFactory.getConnectionProperties(props);
		assertEquals(1, outputProps.size());
		assertEquals("true", outputProps.get("create"));
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.service.schema.internal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.service.schema.internal.ExistingDatabaseMetaDataImpl;
import org.hibernate.service.schema.spi.ExistingDatabaseMetaData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class ExistingDatabaseMetaDataImplTest extends BaseUnitTestCase {
	private JdbcEnvironment jdbcEnvironment;
	private Connection connection;

	@Before
	public void prepare() throws SQLException {
		Properties props = Environment.getProperties();
		connection = DriverManager.getConnection(
				props.getProperty( Environment.URL ),
				props.getProperty( Environment.USER ),
				props.getProperty( Environment.PASS )
		);
		connection.createStatement().execute( "CREATE SCHEMA another_schema" );

		connection.createStatement().execute( "CREATE TABLE t1 (name varchar)" );
		connection.createStatement().execute( "CREATE TABLE another_schema.t2 (name varchar)" );

		connection.createStatement().execute( "CREATE SEQUENCE seq1" );
		connection.createStatement().execute( "CREATE SEQUENCE db1.another_schema.seq2" );

		jdbcEnvironment = new JdbcEnvironmentImpl( connection.getMetaData(), Dialect.getDialect( props ), props );
	}

	@After
	public void release() {
		if ( connection != null ) {
			try {
				connection.close();
			}
			catch (SQLException ignore) {
			}
		}
	}

	@Test
	public void testGetTableMetadata() throws Exception {
		ExistingDatabaseMetaData databaseMetaData =
				ExistingDatabaseMetaDataImpl.builder( jdbcEnvironment, connection.getMetaData() ).prepareAll().build();

		ObjectName name = new ObjectName( null, null, "t1" );
		assertNotNull( databaseMetaData.getTableMetadata( name ) );

		name = new ObjectName( null, "another_schema", "t2" );
		assertNotNull( databaseMetaData.getTableMetadata( name ) );

		name = new ObjectName( null, null, "seq1" );
		assertNotNull( databaseMetaData.getSequenceMetadata( name ) );

		name = new ObjectName( null, "another_schema", "seq2" );
		assertNotNull( databaseMetaData.getSequenceMetadata( name ) );

		// knowing if identifiers coming back from the database are quoted is all dicked up...
		// see org.hibernate.engine.jdbc.env.internal.NormalizingIdentifierHelperImpl
		//
		// surely JDBC has a better way to determine this right?
	}
}

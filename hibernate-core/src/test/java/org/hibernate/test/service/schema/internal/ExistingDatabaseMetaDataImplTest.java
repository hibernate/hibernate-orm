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
import java.util.Iterator;
import java.util.Properties;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformationBuilder;
import org.hibernate.tool.schema.extract.spi.TableInformation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class ExistingDatabaseMetaDataImplTest extends BaseUnitTestCase {
	private ServiceRegistryImplementor serviceRegistry;
	private JdbcEnvironment jdbcEnvironment;
	private Connection connection;

	@Before
	public void prepare() throws SQLException {
		Properties props = Environment.getProperties();
		serviceRegistry = (ServiceRegistryImplementor) new StandardServiceRegistryBuilder().applySettings( props ).build();
		connection = DriverManager.getConnection(
				props.getProperty( Environment.URL ),
				props.getProperty( Environment.USER ),
				props.getProperty( Environment.PASS )
		);
		connection.createStatement().execute( "CREATE SCHEMA another_schema" );

		connection.createStatement().execute( "CREATE TABLE t1 (name varchar, primary key(name))" );
		connection.createStatement().execute( "CREATE TABLE another_schema.t2 (name varchar, primary key(name))" );

		connection.createStatement().execute( "CREATE SEQUENCE seq1" );
		connection.createStatement().execute( "CREATE SEQUENCE db1.another_schema.seq2" );

		jdbcEnvironment = new JdbcEnvironmentImpl( serviceRegistry, Dialect.getDialect( props ), connection.getMetaData() );
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
		if ( serviceRegistry != null ) {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testGetTableMetadata() throws Exception {
		DatabaseInformation databaseMetaData = new DatabaseInformationBuilder( jdbcEnvironment, connection )
				.prepareAll()
				.build();

		ObjectName name = new ObjectName( null, null, "t1" );
		TableInformation table = databaseMetaData.getTableInformation( name );
		assertNotNull( table );
		assertNotNull( table.getPrimaryKey() );
		ColumnInformation nameColumn = table.getColumn( Identifier.toIdentifier( "name" ) );
		assertNotNull( nameColumn );
		Iterator<ColumnInformation> pkColumns = table.getPrimaryKey().getColumns().iterator();
		assertTrue( pkColumns.hasNext() );
		ColumnInformation pkColumn = pkColumns.next();
		assertFalse( pkColumns.hasNext() );
		assertSame( nameColumn, pkColumn );


		name = new ObjectName( null, "another_schema", "t2" );
		assertNotNull( databaseMetaData.getTableInformation( name ) );
		assertNotNull( databaseMetaData.getTableInformation( name ).getPrimaryKey() );

		name = new ObjectName( null, null, "seq1" );
		assertNotNull( databaseMetaData.getSequenceInformation( name ) );

		name = new ObjectName( null, "another_schema", "seq2" );
		assertNotNull( databaseMetaData.getSequenceInformation( name ) );

		// knowing if identifiers coming back from the database are quoted is all dicked up...
		// see org.hibernate.engine.jdbc.env.internal.NormalizingIdentifierHelperImpl
		//
		// surely JDBC has a better way to determine this right?
	}
}

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
package org.hibernate.test.sql.storedproc;

import javax.persistence.ParameterMode;
import java.util.List;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.AuxiliaryDatabaseObject;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.ResultSetOutput;
import org.hibernate.result.Output;
import org.hibernate.dialect.H2Dialect;

import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.addAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public void addDialectScope(String dialectName) {
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return H2Dialect.class.isInstance( dialect );
					}

					@Override
					public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
						return "CREATE ALIAS findOneUser AS $$\n" +
								"import org.h2.tools.SimpleResultSet;\n" +
								"import java.sql.*;\n" +
								"@CODE\n" +
								"ResultSet findOneUser() {\n" +
								"    SimpleResultSet rs = new SimpleResultSet();\n" +
								"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
								"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
								"    rs.addRow(1, \"Steve\");\n" +
								"    return rs;\n" +
								"}\n" +
								"$$";
					}

					@Override
					public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
						return "DROP ALIAS findUser IF EXISTS";
					}
				}
		);

		configuration.addAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public void addDialectScope(String dialectName) {
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return H2Dialect.class.isInstance( dialect );
					}

					@Override
					public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
						return "CREATE ALIAS findUsers AS $$\n" +
								"import org.h2.tools.SimpleResultSet;\n" +
								"import java.sql.*;\n" +
								"@CODE\n" +
								"ResultSet findUsers() {\n" +
								"    SimpleResultSet rs = new SimpleResultSet();\n" +
								"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
								"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
								"    rs.addRow(1, \"Steve\");\n" +
								"    rs.addRow(2, \"John\");\n" +
								"    rs.addRow(3, \"Jane\");\n" +
								"    return rs;\n" +
								"}\n" +
								"$$";
					}

					@Override
					public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
						return "DROP ALIAS findUser IF EXISTS";
					}
				}
		);

		configuration.addAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public void addDialectScope(String dialectName) {
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return H2Dialect.class.isInstance( dialect );
					}

					@Override
					public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
						return "CREATE ALIAS findUserRange AS $$\n" +
								"import org.h2.tools.SimpleResultSet;\n" +
								"import java.sql.*;\n" +
								"@CODE\n" +
								"ResultSet findUserRange(int start, int end) {\n" +
								"    SimpleResultSet rs = new SimpleResultSet();\n" +
								"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
								"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
								"    for ( int i = start; i < end; i++ ) {\n" +
								"        rs.addRow(1, \"User \" + i );\n" +
								"    }\n" +
								"    return rs;\n" +
								"}\n" +
								"$$";
					}

					@Override
					public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
						return "DROP ALIAS findUser IF EXISTS";
					}
				}
		);
	}

	@Test
	public void baseTest() {
		Session session = openSession();
		session.beginTransaction();

		ProcedureCall procedureCall = session.createStoredProcedureCall( "user");
		ProcedureOutputs procedureOutputs = procedureCall.getOutputs();
		Output currentOutput = procedureOutputs.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		String name = (String) resultSetReturn.getSingleResult();
		assertEquals( "SA", name );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testGetSingleResultTuple() {
		Session session = openSession();
		session.beginTransaction();

		ProcedureCall query = session.createStoredProcedureCall( "findOneUser" );
		ProcedureOutputs procedureResult = query.getOutputs();
		Output currentOutput = procedureResult.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		Object result = resultSetReturn.getSingleResult();
		assertTyping( Object[].class, result );
		String name = (String) ( (Object[]) result )[1];
		assertEquals( "Steve", name );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testGetResultListTuple() {
		Session session = openSession();
		session.beginTransaction();

		ProcedureCall query = session.createStoredProcedureCall( "findUsers" );
		ProcedureOutputs procedureResult = query.getOutputs();
		Output currentOutput = procedureResult.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		List results = resultSetReturn.getResultList();
		assertEquals( 3, results.size() );

		for ( Object result : results ) {
			assertTyping( Object[].class, result );
			Integer id = (Integer) ( (Object[]) result )[0];
			String name = (String) ( (Object[]) result )[1];
			if ( id.equals( 1 ) ) {
				assertEquals( "Steve", name );
			}
			else if ( id.equals( 2 ) ) {
				assertEquals( "John", name );
			}
			else if ( id.equals( 3 ) ) {
				assertEquals( "Jane", name );
			}
			else {
				fail( "Unexpected id value found [" + id + "]" );
			}
		}

		session.getTransaction().commit();
		session.close();
	}

// A warning should be logged if database metadata indicates named parameters are not supported.
	@Test
	@FailureExpectedWithNewMetamodel
	public void testInParametersByName() {
		Session session = openSession();
		session.beginTransaction();

		ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
		query.registerParameter( "start", Integer.class, ParameterMode.IN ).bindValue( 1 );
		query.registerParameter( "end", Integer.class, ParameterMode.IN ).bindValue( 2 );
		ProcedureOutputs procedureResult = query.getOutputs();
		Output currentOutput = procedureResult.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		List results = resultSetReturn.getResultList();
		assertEquals( 1, results.size() );
		Object result = results.get( 0 );
		assertTyping( Object[].class, result );
		Integer id = (Integer) ( (Object[]) result )[0];
		String name = (String) ( (Object[]) result )[1];
		assertEquals( 1, (int) id );
		assertEquals( "User 1", name );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testInParametersByPosition() {
		Session session = openSession();
		session.beginTransaction();

		ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
		query.registerParameter( 1, Integer.class, ParameterMode.IN ).bindValue( 1 );
		query.registerParameter( 2, Integer.class, ParameterMode.IN ).bindValue( 2 );
		ProcedureOutputs procedureResult = query.getOutputs();
		Output currentOutput = procedureResult.getCurrent();
		assertNotNull( currentOutput );
		ResultSetOutput resultSetReturn = assertTyping( ResultSetOutput.class, currentOutput );
		List results = resultSetReturn.getResultList();
		assertEquals( 1, results.size() );
		Object result = results.get( 0 );
		assertTyping( Object[].class, result );
		Integer id = (Integer) ( (Object[]) result )[0];
		String name = (String) ( (Object[]) result )[1];
		assertEquals( 1, (int) id );
		assertEquals( "User 1", name );

		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testInParametersNotSet() {
		Session session = openSession();
		session.beginTransaction();

		// since the procedure does not define defaults for parameters this should result in SQLExceptions on
		// execution

		{
			ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
			query.registerParameter( 1, Integer.class, ParameterMode.IN );
			query.registerParameter( 2, Integer.class, ParameterMode.IN ).bindValue( 2 );
			try {
				query.getOutputs();
				fail( "Expecting failure due to missing parameter bind" );
			}
			catch (JDBCException expected) {
			}
		}

// H2 does not support named parameters
//		{
//			ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
//			query.registerParameter( "start", Integer.class, ParameterMode.IN );
//			query.registerParameter( "end", Integer.class, ParameterMode.IN ).bindValue( 2 );
//			try {
//				query.getOutputs();
//				fail( "Expecting failure due to missing parameter bind" );
//			}
//			catch (JDBCException expected) {
//			}
//		}

		session.getTransaction().commit();
		session.close();
	}
}

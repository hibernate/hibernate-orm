/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.storedproc;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedStoredProcedureQueries;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.ParameterMode;
import javax.persistence.QueryHint;
import javax.persistence.StoredProcedureParameter;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;
import org.hibernate.procedure.ProcedureOutputs;
import org.hibernate.result.Output;
import org.hibernate.result.ResultSetOutput;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.addAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public String getExportIdentifier() {
						return "function:findOneUser";
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return H2Dialect.class.isInstance( dialect );
					}

					@Override
					public boolean beforeTablesOnCreation() {
						return false;
					}

					@Override
					public String[] sqlCreateStrings(Dialect dialect) {
						return new String[] {
								"CREATE ALIAS findOneUser AS $$\n" +
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
										"$$"
						};
					}

					@Override
					public String[] sqlDropStrings(Dialect dialect) {
						return new String[] {
								"DROP ALIAS findUser IF EXISTS"
						};
					}
				}
		);

		configuration.addAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public String getExportIdentifier() {
						return "function:findUsers";
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return H2Dialect.class.isInstance( dialect );
					}

					@Override
					public boolean beforeTablesOnCreation() {
						return false;
					}

					@Override
					public String[] sqlCreateStrings(Dialect dialect) {
						return new String[] {
								"CREATE ALIAS findUsers AS $$\n" +
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
										"$$"
						};
					}

					@Override
					public String[] sqlDropStrings(Dialect dialect) {
						return new String[] {"DROP ALIAS findUser IF EXISTS"};
					}
				}
		);

		configuration.addAuxiliaryDatabaseObject(
				new AuxiliaryDatabaseObject() {
					@Override
					public String getExportIdentifier() {
						return "function:findUserRange";
					}

					@Override
					public boolean appliesToDialect(Dialect dialect) {
						return H2Dialect.class.isInstance( dialect );
					}

					@Override
					public boolean beforeTablesOnCreation() {
						return false;
					}

					@Override
					public String[] sqlCreateStrings(Dialect dialect) {
						return new String[] {
								"CREATE ALIAS findUserRange AS $$\n" +
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
										"$$"
						};
					}

					@Override
					public String[] sqlDropStrings(Dialect dialect) {
						return new String[] {"DROP ALIAS findUserRange IF EXISTS"};
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

	@Test
	public void testInParametersNotSetPass() {
		Session session = openSession();
		session.beginTransaction();

		// unlike #testInParametersNotSet here we are asking that the NULL be passed
		// so these executions should succeed


		ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
		query.registerParameter( 1, Integer.class, ParameterMode.IN ).enablePassingNulls( true );
		query.registerParameter( 2, Integer.class, ParameterMode.IN ).bindValue( 2 );
		query.getOutputs();

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

	@Test
	@SuppressWarnings("unchecked")
	public void testInParametersNullnessPassingInNamedQueriesViaHints() {
		Session session = openSession();
		session.beginTransaction();

		// similar to #testInParametersNotSet and #testInParametersNotSetPass in terms of testing
		// support for specifying whether to pass NULL argument values or not.  This version tests
		// named procedure support via hints.

		// first a fixture - this execution should fail
		{
			ProcedureCall query = session.getNamedProcedureCall( "findUserRangeNoNullPassing" );
			query.getParameterRegistration( 2 ).bindValue( 2 );
			try {
				query.getOutputs();
				fail( "Expecting failure due to missing parameter bind" );
			}
			catch (JDBCException ignore) {
			}
		}

		// here we enable NULL passing via hint through a named parameter
		{
			ProcedureCall query = session.getNamedProcedureCall( "findUserRangeNamedNullPassing" );
			query.getParameterRegistration( "secondArg" ).bindValue( 2 );
			query.getOutputs();
		}

		// here we enable NULL passing via hint through a named parameter
		{
			ProcedureCall query = session.getNamedProcedureCall( "findUserRangeOrdinalNullPassing" );
			query.getParameterRegistration( 2 ).bindValue( 2 );
			query.getOutputs();
		}

		session.getTransaction().commit();
		session.close();
	}

	@Entity
	@NamedStoredProcedureQueries( {
			@NamedStoredProcedureQuery(
					name = "findUserRangeNoNullPassing",
					procedureName = "findUserRange",
					parameters = {
							@StoredProcedureParameter( type = Integer.class ),
							@StoredProcedureParameter( type = Integer.class ),
					}
			),
			@NamedStoredProcedureQuery(
					name = "findUserRangeNamedNullPassing",
					procedureName = "findUserRange",
					hints = @QueryHint( name = "hibernate.proc.param_null_passing.firstArg", value = "true" ),
					parameters = {
							@StoredProcedureParameter( name = "firstArg", type = Integer.class ),
							@StoredProcedureParameter( name = "secondArg", type = Integer.class ),
					}
			),
			@NamedStoredProcedureQuery(
					name = "findUserRangeOrdinalNullPassing",
					procedureName = "findUserRange",
					hints = @QueryHint( name = "hibernate.proc.param_null_passing.1", value = "true" ),
					parameters = {
							@StoredProcedureParameter( type = Integer.class ),
							@StoredProcedureParameter( type = Integer.class ),
					}
			)
	} )
	public static class MyEntity {
		@Id
		public Integer id;
	}
}

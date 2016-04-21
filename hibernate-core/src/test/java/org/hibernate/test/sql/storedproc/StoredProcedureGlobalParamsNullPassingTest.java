/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.sql.storedproc;

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
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.procedure.ProcedureCall;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureGlobalParamsNullPassingTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.PROCEDURE_NULL_PARAM_PASSING, "true" );
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
	public void testInParametersSetPassGlobal() {
		Session session = openSession();
		session.beginTransaction();

		// AvailableSettings.PROCEDURE_NULL_PARAM_PASSING == "true"
		// so this execution should succeed

		ProcedureCall query = session.createStoredProcedureCall( "findUserRange" );
		query.registerParameter( 1, Integer.class, ParameterMode.IN );
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
	public void testInParametersNullnessPassingInNamedQueriesUsingGlobal() {
		Session session = openSession();
		session.beginTransaction();

		// AvailableSettings.PROCEDURE_NULL_PARAM_PASSING == "true"
		// so this execution should succeed

		// first a fixture - this execution should pass with
		ProcedureCall query = session.getNamedProcedureCall( "findUserRangeNoNullPassing" );
		query.getParameterRegistration( 2 ).bindValue( 2 );
		query.getOutputs();

		session.getTransaction().commit();
		session.close();
	}

	@Entity
	@NamedStoredProcedureQuery(
			name = "findUserRangeNoNullPassing",
			procedureName = "findUserRange",
			parameters = {
					@StoredProcedureParameter( type = Integer.class ),
					@StoredProcedureParameter( type = Integer.class ),
			}
	)

	public static class MyEntity {
		@Id
		public Integer id;
	}
}

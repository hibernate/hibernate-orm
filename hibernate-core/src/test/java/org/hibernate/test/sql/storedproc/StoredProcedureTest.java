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

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.StoredProcedureCall;
import org.hibernate.StoredProcedureOutputs;
import org.hibernate.StoredProcedureResultSetReturn;
import org.hibernate.StoredProcedureReturn;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.AuxiliaryDatabaseObject;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.junit4.ExtraAssertions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@RequiresDialect( H2Dialect.class )
public class StoredProcedureTest extends BaseCoreFunctionalTestCase {
// this is not working in H2
//	@Override
//	protected void configure(Configuration configuration) {
//		super.configure( configuration );
//		configuration.addAuxiliaryDatabaseObject(
//				new AuxiliaryDatabaseObject() {
//					@Override
//					public void addDialectScope(String dialectName) {
//					}
//
//					@Override
//					public boolean appliesToDialect(Dialect dialect) {
//						return H2Dialect.class.isInstance( dialect );
//					}
//
//					@Override
//					public String sqlCreateString(Dialect dialect, Mapping p, String defaultCatalog, String defaultSchema) {
//						return "CREATE ALIAS findUser AS $$\n" +
//								"import org.h2.tools.SimpleResultSet;\n" +
//								"import java.sql.*;\n" +
//								"@CODE\n" +
//								"ResultSet findUser() {\n" +
//								"    SimpleResultSet rs = new SimpleResultSet();\n" +
//								"    rs.addColumn(\"ID\", Types.INTEGER, 10, 0);\n" +
//								"    rs.addColumn(\"NAME\", Types.VARCHAR, 255, 0);\n" +
//								"    rs.addRow(1, \"Steve\");\n" +
//								"    return rs;\n" +
//								"}\n" +
//								"$$";
//					}
//
//					@Override
//					public String sqlDropString(Dialect dialect, String defaultCatalog, String defaultSchema) {
//						return "DROP ALIAS findUser IF EXISTS";
//					}
//				}
//		);
//	}

	@Test
	public void baseTest() {
		Session session = openSession();
		session.beginTransaction();

		StoredProcedureCall query = session.createStoredProcedureCall( "user");
		StoredProcedureOutputs outputs = query.getOutputs();
		assertTrue( "Checking StoredProcedureOutputs has more returns", outputs.hasMoreReturns() );
		StoredProcedureReturn nextReturn = outputs.getNextReturn();
		assertNotNull( nextReturn );
		ExtraAssertions.assertClassAssignability( StoredProcedureResultSetReturn.class, nextReturn.getClass() );
		StoredProcedureResultSetReturn resultSetReturn = (StoredProcedureResultSetReturn) nextReturn;
		String name = (String) resultSetReturn.getSingleResult();
		assertEquals( "SA", name );

		session.getTransaction().commit();
		session.close();
	}
}

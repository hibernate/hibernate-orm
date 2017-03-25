/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.c3p0;

import java.sql.Statement;

import org.hibernate.dialect.SQLServer2005Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests that checks the JDBC 4.2 compatibility of c3p0
 * 
 * @author Vlad Mihalcea
 */
@RequiresDialect(SQLServer2005Dialect.class)
public class JdbcCompatibilityTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-11308" )
	public void testJdbc41() {
		doInHibernate( this::sessionFactory, session -> {
			session.doWork( connection -> {
				//Connection#getSchema was added in Java 1.7
				String schema = connection.getSchema();
				assertNotNull(schema);
			} );
		} );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11308" )
	public void testJdbc42() {
		doInHibernate( this::sessionFactory, session -> {
			for ( int i = 0; i < 5; i++ ) {
				IrrelevantEntity entity = new IrrelevantEntity();
				entity.setName( getClass().getName() );
				session.persist( entity );
			}
			session.flush();
			session.doWork( connection -> {
				try( Statement statement = connection.createStatement()) {
					statement.executeUpdate( "DELETE FROM IrrelevantEntity" );
					assertEquals( 5, statement.getLargeUpdateCount());
				}
			} );
		} );
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ IrrelevantEntity.class };
	}
}

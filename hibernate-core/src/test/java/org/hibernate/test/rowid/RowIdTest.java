/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.rowid;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.Oracle9iDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.jdbc.Work;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Gavin King
 */
@RequiresDialect( value = Oracle9iDialect.class )
public class RowIdTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "rowid/Point.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public boolean createSchema() {
		return false;
	}

	public void afterSessionFactoryBuilt() {
		super.afterSessionFactoryBuilt();
		final Session session = sessionFactory().openSession();
		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						Statement st = ((SessionImplementor)session).getJdbcCoordinator().getStatementPreparer().createStatement();
						try {
							((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().execute( st, "drop table Point");
						}
						catch (Exception ignored) {
						}
						((SessionImplementor)session).getJdbcCoordinator().getResultSetReturn().execute( st, "create table Point (\"x\" number(19,2) not null, \"y\" number(19,2) not null, description varchar2(255) )");
						((SessionImplementor)session).getJdbcCoordinator().getResourceRegistry().release( st );
					}
				}
		);
		session.close();
	}

	@Test
	public void testRowId() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Point p = new Point( new BigDecimal(1.0), new BigDecimal(1.0) );
		s.persist(p);
		t.commit();
		s.clear();
		
		t = s.beginTransaction();
		p = (Point) s.createCriteria(Point.class).uniqueResult();
		p.setDescription("new desc");
		t.commit();
		s.clear();
		
		t = s.beginTransaction();
		p = (Point) s.createQuery("from Point").uniqueResult();
		p.setDescription("new new desc");
		t.commit();
		s.clear();
		
		t = s.beginTransaction();
		p = (Point) s.get(Point.class, p);
		p.setDescription("new new new desc");
		t.commit();
		s.close();
	}

}


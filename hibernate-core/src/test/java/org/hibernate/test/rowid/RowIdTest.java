/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
						Statement st = ((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().getStatementPreparer().createStatement();
						try {
							((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().execute( st, "drop table Point");
						}
						catch (Exception ignored) {
						}
						((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().getResultSetReturn().execute( st, "create table Point (\"x\" number(19,2) not null, \"y\" number(19,2) not null, description varchar2(255) )");
						((SessionImplementor)session).getTransactionCoordinator().getJdbcCoordinator().release( st );
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


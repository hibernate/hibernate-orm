/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.formulajoin;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class FormulaJoinTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "formulajoin/Master.hbm.xml" };
	}

	@Test
	public void testFormulaJoin() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Master master = new Master();
		master.setName("master 1");
		Detail current = new Detail();
		current.setCurrentVersion(true);
		current.setVersion(2);
		current.setDetails("details of master 1 blah blah");
		current.setMaster(master);
		master.setDetail(current);
		Detail past = new Detail();
		past.setCurrentVersion(false);
		past.setVersion(1);
		past.setDetails("old details of master 1 yada yada");
		past.setMaster(master);
		s.persist(master);
		s.persist(past);
		s.persist(current);
		tx.commit();
		s.close();
		
		if ( getDialect() instanceof PostgreSQLDialect  || getDialect() instanceof PostgreSQL81Dialect ) return;

		s = openSession();
		tx = s.beginTransaction();
		List l = s.createQuery("from Master m left join m.detail d").list();
		assertEquals( l.size(), 1 );
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Master m left join fetch m.detail").list();
		assertEquals( l.size(), 1 );
		Master m = (Master) l.get(0);
		assertEquals( "master 1", m.getDetail().getMaster().getName() );
		assertTrue( m==m.getDetail().getMaster() );
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Master m join fetch m.detail").list();
		assertEquals( l.size(), 1 );
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Detail d join fetch d.currentMaster.master").list();
		assertEquals( l.size(), 2 );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Detail d join fetch d.currentMaster.master m join fetch m.detail").list();
		assertEquals( l.size(), 2 );
		
		s.createQuery("delete from Detail").executeUpdate();
		s.createQuery("delete from Master").executeUpdate();
		
		tx.commit();
		s.close();

	}
}


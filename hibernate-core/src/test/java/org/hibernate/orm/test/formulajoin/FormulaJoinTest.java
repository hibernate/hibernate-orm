/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.formulajoin;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class FormulaJoinTest extends BaseCoreFunctionalTestCase {

	@Override
	public String[] getMappings() {
		return new String[] { "formulajoin/Root.hbm.xml" };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.JPA_METAMODEL_POPULATION, "enabled" );
	}

	@Test
	public void testFormulaJoin() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Root root = new Root();
		root.setName("root 1");
		Detail current = new Detail();
		current.setCurrentVersion(true);
		current.setVersion(2);
		current.setDetails("details of root 1 blah blah");
		current.setRoot( root );
		root.setDetail(current);
		Detail past = new Detail();
		past.setCurrentVersion(false);
		past.setVersion(1);
		past.setDetails("old details of root 1 yada yada");
		past.setRoot( root );
		s.persist( root );
		s.persist(past);
		s.persist(current);
		tx.commit();
		s.close();

		if ( getDialect() instanceof PostgreSQLDialect ) return;

		s = openSession();
		tx = s.beginTransaction();
		List l = s.createQuery("from Root m left join m.detail d", Object[].class).list();
		assertEquals( l.size(), 1 );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Root m left join fetch m.detail").list();
		assertEquals( l.size(), 1 );
		Root m = (Root) l.get(0);
		assertEquals( "root 1", m.getDetail().getRoot().getName() );
		assertTrue( m==m.getDetail().getRoot() );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Root m join fetch m.detail").list();
		assertEquals( l.size(), 1 );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Detail d join fetch d.currentRoot.root").list();
		assertEquals( l.size(), 2 );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Detail d join fetch d.root").list();
		assertEquals( l.size(), 2 );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Detail d join fetch d.currentRoot.root m join fetch m.detail").list();
		assertEquals( l.size(), 2 );
		tx.commit();

		s = openSession();
		tx = s.beginTransaction();
		l = s.createQuery("from Detail d join fetch d.root m join fetch m.detail").list();
		assertEquals( l.size(), 2 );

		s.createQuery("delete from Detail").executeUpdate();
		s.createQuery("delete from Root").executeUpdate();

		tx.commit();
		s.close();

	}
}

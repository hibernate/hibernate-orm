/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.ops;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Gavin King
 */
public class GetLoadTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
		cfg.setProperty(Environment.STATEMENT_BATCH_SIZE, "0");
	}

	@Override
	public String[] getMappings() {
		return new String[] { "ops/Node.hbm.xml", "ops/Employer.hbm.xml" };
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	public void testGetLoad() {
		clearCounts();
		
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Employer emp = new Employer();
		s.persist(emp);
		Node node = new Node("foo");
		Node parent = new Node("bar");
		parent.addChild(node);
		s.persist(parent);
		tx.commit();
		s.close();
		
		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.get(Employer.class, emp.getId());
		assertTrue( Hibernate.isInitialized(emp) );
		assertFalse( Hibernate.isInitialized(emp.getEmployees()) );
		node = (Node) s.get(Node.class, node.getName());
		assertTrue( Hibernate.isInitialized(node) );
		assertFalse( Hibernate.isInitialized(node.getChildren()) );
		assertFalse( Hibernate.isInitialized(node.getParent()) );
		assertNull( s.get(Node.class, "xyz") );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.load(Employer.class, emp.getId());
		emp.getId();
		assertFalse( Hibernate.isInitialized(emp) );
		node = (Node) s.load(Node.class, node.getName());
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized(node) );
		tx.commit();
		s.close();
	
		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.get("org.hibernate.test.ops.Employer", emp.getId());
		assertTrue( Hibernate.isInitialized(emp) );
		node = (Node) s.get("org.hibernate.test.ops.Node", node.getName());
		assertTrue( Hibernate.isInitialized(node) );
		tx.commit();
		s.close();

		s = openSession();
		tx = s.beginTransaction();
		emp = (Employer) s.load("org.hibernate.test.ops.Employer", emp.getId());
		emp.getId();
		assertFalse( Hibernate.isInitialized(emp) );
		node = (Node) s.load("org.hibernate.test.ops.Node", node.getName());
		assertEquals( node.getName(), "foo" );
		assertFalse( Hibernate.isInitialized(node) );
		tx.commit();
		s.close();
		
		assertFetchCount(0);
	}

	@Test
	public void testGetAfterDelete() {
		clearCounts();

		Session s = openSession();
		s.beginTransaction();
		Employer emp = new Employer();
		s.persist( emp );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( emp );
		emp = ( Employer ) s.get( Employee.class, emp.getId() );
		s.getTransaction().commit();
		s.close();

		assertNull( "get did not return null after delete", emp );
	}

	private void clearCounts() {
		sessionFactory().getStatistics().clear();
	}
	
	private void assertFetchCount(int count) {
		int fetches = (int) sessionFactory().getStatistics().getEntityFetchCount();
		assertEquals(count, fetches);
	}


}


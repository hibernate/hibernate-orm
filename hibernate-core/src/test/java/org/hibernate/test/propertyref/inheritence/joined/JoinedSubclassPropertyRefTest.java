/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2005-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.propertyref.inheritence.joined;

import org.junit.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@FailureExpectedWithNewMetamodel
public class JoinedSubclassPropertyRefTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "propertyref/inheritence/joined/Person.hbm.xml" };
	}

	@Test
	public void testPropertyRefToJoinedSubclass() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		Person p = new Person();
		p.setName("Gavin King");
		BankAccount acc = new BankAccount();
		acc.setBsb("0634");
		acc.setType('B');
		acc.setAccountNumber("xxx-123-abc");
		p.setBankAccount(acc);
		session.persist(p);
		tx.commit();
		session.close();

		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.get(Person.class, p.getId());
		assertNotNull( p.getBankAccount() );
		assertTrue( Hibernate.isInitialized( p.getBankAccount() ) );
		tx.commit();
		session.close();

		session = openSession();
		tx = session.beginTransaction();
		p = (Person) session.createCriteria(Person.class)
			.setFetchMode("bankAccount", FetchMode.JOIN)
			.uniqueResult();
		assertNotNull( p.getBankAccount() );
		assertTrue( Hibernate.isInitialized( p.getBankAccount() ) );
		tx.commit();
		session.close();

		session = openSession();
		tx = session.beginTransaction();
		session.delete(p);
		tx.commit();
		session.close();
	}

}


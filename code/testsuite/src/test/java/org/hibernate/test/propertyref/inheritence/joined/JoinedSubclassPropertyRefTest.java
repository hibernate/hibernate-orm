//$Id: PropertyRefTest.java 7275 2005-06-22 18:58:16Z oneovthafew $
package org.hibernate.test.propertyref.inheritence.joined;

import junit.framework.Test;

import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class JoinedSubclassPropertyRefTest extends FunctionalTestCase {
	
	public JoinedSubclassPropertyRefTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "propertyref/inheritence/joined/Person.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( JoinedSubclassPropertyRefTest.class );
	}
	
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


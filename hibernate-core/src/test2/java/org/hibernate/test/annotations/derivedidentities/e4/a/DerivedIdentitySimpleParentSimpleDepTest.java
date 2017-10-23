/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e4.a;

import java.util.Date;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentitySimpleParentSimpleDepTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "id", metadata() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person( "aaa" );
		s.persist( person );
		MedicalHistory medicalHistory = new MedicalHistory( person );
		s.persist( medicalHistory );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		medicalHistory = (MedicalHistory) s.get( MedicalHistory.class, "aaa" );
		assertEquals( person.ssn, medicalHistory.patient.ssn );
		medicalHistory.lastupdate = new Date();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		medicalHistory = (MedicalHistory) s.get( MedicalHistory.class, "aaa" );
		assertNotNull( medicalHistory.lastupdate );
		s.delete( medicalHistory );
		s.delete( medicalHistory.patient );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testManyToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "FinancialHistory", "patient_ssn", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "FinancialHistory", "id", metadata() ) );

		Session s = openSession();
		s.getTransaction().begin();
		Person person = new Person( "aaa" );
		s.persist( person );
		FinancialHistory financialHistory = new FinancialHistory( person );
		s.persist( financialHistory );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		financialHistory = (FinancialHistory) s.get( FinancialHistory.class, "aaa" );
		assertEquals( person.ssn, financialHistory.patient.ssn );
		financialHistory.lastUpdate = new Date();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		financialHistory = (FinancialHistory) s.get( FinancialHistory.class, "aaa" );
		assertNotNull( financialHistory.lastUpdate );
		s.delete( financialHistory );
		s.delete( financialHistory.patient );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testSimplePkValueLoading() {
		Session s = openSession();
		s.getTransaction().begin();
		Person e = new Person( "aaa" );
		s.persist( e );
		FinancialHistory d = new FinancialHistory( e );
		s.persist( d );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		FinancialHistory history = (FinancialHistory) s.get( FinancialHistory.class, "aaa" );
		assertNotNull( history );
		s.delete( history );
		s.delete( history.patient );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MedicalHistory.class,
				Simple.class,
				Person.class,
				FinancialHistory.class
		};
	}
}

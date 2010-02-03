package org.hibernate.test.annotations.derivedidentities.e4.a;

import java.util.Date;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentitySimpleParentSimpleDepTest extends TestCase {

	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "id", getCfg() ) );

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
		s.getTransaction().commit();
		s.close();
	}

	public void testManyToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "FinancialHistory", "patient_ssn", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "FinancialHistory", "id", getCfg() ) );

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
		s.getTransaction().commit();
		s.close();
	}

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
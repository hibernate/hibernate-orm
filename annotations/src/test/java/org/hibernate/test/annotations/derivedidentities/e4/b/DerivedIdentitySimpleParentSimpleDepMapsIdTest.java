package org.hibernate.test.annotations.derivedidentities.e4.b;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentitySimpleParentSimpleDepMapsIdTest extends TestCase {

	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "id", getCfg() ) );
		Person e = new Person();
		e.ssn = "aaa";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		MedicalHistory d = new MedicalHistory();
		d.patient = e;
		//d.id = "aaa"; //FIXME not needed when foreign is enabled
		s.persist( d );
		s.flush();
		s.clear();
		d = (MedicalHistory) s.get( MedicalHistory.class, d.id );
		assertEquals( d.id, d.patient.ssn );
		d.lastupdate = new Date();
		s.flush();
		s.clear();
		d = (MedicalHistory) s.get( MedicalHistory.class, d.id );
		assertNotNull( d.lastupdate );
		s.getTransaction().rollback();
		s.close();
	}

	public void testManyToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "FinancialHistory", "FK", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "FinancialHistory", "id", getCfg() ) );
		Person e = new Person();
		e.ssn = "aaa";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		FinancialHistory d = new FinancialHistory();
		d.patient = e;
		//d.id = "aaa"; //FIXME not needed when foreign is enabled
		s.persist( d );
		s.flush();
		s.clear();
		d = (FinancialHistory) s.get( FinancialHistory.class, d.id );
		assertEquals( d.id, d.patient.ssn );
		d.lastupdate = new Date();
		s.flush();
		s.clear();
		d = (FinancialHistory) s.get( FinancialHistory.class, d.id );
		assertNotNull( d.lastupdate );
		s.getTransaction().rollback();
		s.close();
	}

	public void testExplicitlyAssignedDependentIdAttributeValue() {
		// even though the id is by definition generated (using the "foreign" strategy), JPA
		// still does allow manually setting the generated id attribute value which providers
		// are expected to promptly disregard :?
		Session s = openSession();
		s.beginTransaction();
		Person person = new Person( "123456789" );
		MedicalHistory medicalHistory = new MedicalHistory( "987654321", person );
		s.persist( person );
		s.persist( medicalHistory );
		s.getTransaction().commit();
		s.close();

		// again, even though we specified an id value of "987654321" prior to persist,
		// Hibernate should have replaced that with the "123456789" from the associated
		// person
		assertEquals( person.ssn, medicalHistory.patient.ssn );
		assertEquals( person, medicalHistory.patient );
		assertEquals( person.ssn, medicalHistory.id );

		s = openSession();
		s.beginTransaction();
		// Should return null...
		MedicalHistory separateMedicalHistory = (MedicalHistory) s.get( MedicalHistory.class, "987654321" );
		assertNull( separateMedicalHistory );
		// Now we should find it...
		separateMedicalHistory = (MedicalHistory) s.get( MedicalHistory.class, "123456789" );
		assertNotNull( separateMedicalHistory );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.delete( medicalHistory );
		s.delete( person );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MedicalHistory.class,
				Person.class,
				FinancialHistory.class
		};
	}
}
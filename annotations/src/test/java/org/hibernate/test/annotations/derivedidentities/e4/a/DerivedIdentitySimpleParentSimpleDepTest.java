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
		Person e = new Person();
		e.ssn = "aaa";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		MedicalHistory d = new MedicalHistory();
		d.patient = e;
		s.persist( d );
		s.flush();
		s.clear();
		final Class<MedicalHistory> clazz = MedicalHistory.class;
		d = getDerivedClassById( e, s, clazz );
		assertEquals( e.ssn, d.patient.ssn );
		d.lastupdate = new Date();
		s.flush();
		s.clear();
		d = getDerivedClassById( e, s, clazz );
		assertNotNull( d.lastupdate );
		s.getTransaction().rollback();
		s.close();
	}

	private <T> T getDerivedClassById(Person e, Session s, Class<T> clazz) {
		return ( T )
				s.createQuery( "from " + clazz.getName() + " mh where mh.patient.ssn = :ssn")
					.setParameter( "ssn", e.ssn ).uniqueResult();
	}

	public void testManyToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "FinancialHistory", "patient_ssn", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "FinancialHistory", "id", getCfg() ) );
		Person e = new Person();
		e.ssn = "aaa";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		FinancialHistory d = new FinancialHistory();
		d.patient = e;
		s.persist( d );
		s.flush();
		s.clear();
		d = getDerivedClassById(e, s, FinancialHistory.class);
		assertEquals( e.ssn, d.patient.ssn );
		d.lastupdate = new Date();
		s.flush();
		s.clear();
		d = getDerivedClassById(e, s, FinancialHistory.class);
		assertNotNull( d.lastupdate );
		s.getTransaction().rollback();
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
package org.hibernate.test.annotations.derivedidentities.e4.b;

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
		d.id = "aaa"; //FIXME not needed when foreign is enabled
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
		d.id = "aaa"; //FIXME not needed when foreign is enabled
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

	@Override
	protected Class<?>[] getMappings() {
		return new Class<?>[] {
				MedicalHistory.class,
				Person.class,
				FinancialHistory.class
		};
	}
}
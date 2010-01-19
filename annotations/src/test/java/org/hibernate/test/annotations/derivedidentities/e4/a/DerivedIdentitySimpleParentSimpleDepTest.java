package org.hibernate.test.annotations.derivedidentities.e4.a;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.annotations.derivedidentities.e1.b.Dependent;
import org.hibernate.test.annotations.derivedidentities.e1.b.DependentId;
import org.hibernate.test.annotations.derivedidentities.e1.b.Employee;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentitySimpleParentSimpleDepTest extends TestCase {

	public void testIt() throws Exception {
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
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class<?>[] getMappings() {
		return new Class<?>[] {
				MedicalHistory.class,
				Person.class
		};
	}
}
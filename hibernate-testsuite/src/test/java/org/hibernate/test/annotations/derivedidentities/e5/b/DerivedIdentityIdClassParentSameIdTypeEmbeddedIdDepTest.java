package org.hibernate.test.annotations.derivedidentities.e5.b;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityIdClassParentSameIdTypeEmbeddedIdDepTest extends TestCase {

	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", getCfg() ) );
		Person e = new Person();
		e.firstName = "Emmanuel";
		e.lastName = "Bernard";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		MedicalHistory d = new MedicalHistory();
		d.patient = e;
		s.persist( d );
		s.flush();
		s.clear();
		d = (MedicalHistory) s.get( MedicalHistory.class, d.id );
		assertEquals( d.id.firstName, d.patient.firstName );
		s.getTransaction().rollback();
		s.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				MedicalHistory.class,
				Person.class
		};
	}
}

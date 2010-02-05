package org.hibernate.test.annotations.derivedidentities.e6.b;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityEmbeddedIdParentSameIdTypeEmbeddedIdDepTest extends TestCase {

	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", getCfg() ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", getCfg() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", getCfg() ) );
		Person e = new Person();
		e.id = new PersonId();
		e.id.firstName = "Emmanuel";
		e.id.lastName = "Bernard";
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		MedicalHistory d = new MedicalHistory();
//		d.id = new PersonId();
//		d.id.firstName = "Emmanuel"; //FIXME not needed when foreign is enabled
//		d.id.lastName = "Bernard"; //FIXME not needed when foreign is enabled
		d.patient = e;
		s.persist( d );
		s.flush();
		s.clear();
		d = (MedicalHistory) s.get( MedicalHistory.class, d.id );
		assertEquals( d.id.firstName, d.patient.id.firstName );
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
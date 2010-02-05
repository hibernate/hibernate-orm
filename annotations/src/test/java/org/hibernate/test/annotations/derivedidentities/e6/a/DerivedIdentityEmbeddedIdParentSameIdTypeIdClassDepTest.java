package org.hibernate.test.annotations.derivedidentities.e6.a;

import org.hibernate.Session;
import org.hibernate.junit.FailureExpected;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityEmbeddedIdParentSameIdTypeIdClassDepTest extends TestCase {

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
		d.patient = e;
		s.persist( d );
		s.flush();
		s.clear();
		PersonId pId = new PersonId();
		pId.firstName = e.id.firstName;
		pId.lastName = e.id.lastName;
		d = (MedicalHistory) s.get( MedicalHistory.class, pId );
		assertEquals( pId.firstName, d.patient.id.firstName );
		s.delete( d );
		s.delete( d.patient );
		s.getTransaction().commit();
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
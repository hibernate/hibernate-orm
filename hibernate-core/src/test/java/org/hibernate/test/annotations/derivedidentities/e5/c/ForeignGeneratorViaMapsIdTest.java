package org.hibernate.test.annotations.derivedidentities.e5.c;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;

/**
 * @author Emmanuel Bernard
 */
public class ForeignGeneratorViaMapsIdTest extends TestCase {

	public void testForeignGenerator() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "patient_id", getCfg() ) );
		Person e = new Person();
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		MedicalHistory d = new MedicalHistory();
		d.patient = e;
		s.persist( d );
		s.flush();
		s.clear();
		d = (MedicalHistory) s.get( MedicalHistory.class, e.id);
		assertEquals( e.id, d.id );
		s.delete( d );
		s.delete( d.patient );
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

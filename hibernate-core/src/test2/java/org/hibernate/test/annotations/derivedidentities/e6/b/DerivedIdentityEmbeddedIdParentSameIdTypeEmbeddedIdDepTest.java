/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e6.b;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class DerivedIdentityEmbeddedIdParentSameIdTypeEmbeddedIdDepTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testOneToOneExplicitJoinColumn() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK1", metadata() ) );
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "FK2", metadata() ) );
		assertTrue( ! SchemaUtil.isColumnPresent( "MedicalHistory", "firstname", metadata() ) );
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

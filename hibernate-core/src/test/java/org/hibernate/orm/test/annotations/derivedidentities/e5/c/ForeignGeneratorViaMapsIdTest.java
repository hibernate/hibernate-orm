/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.c;

import org.hibernate.Session;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.orm.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class ForeignGeneratorViaMapsIdTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testForeignGenerator() throws Exception {
		assertTrue( SchemaUtil.isColumnPresent( "MedicalHistory", "patient_id", metadata() ) );

		Person e = new Person();
		Session s = openSession(  );
		s.getTransaction().begin();
		s.persist( e );
		MedicalHistory d = new MedicalHistory();
		d.patient = e;
		s.persist( d );
		s.flush();
		s.clear();
		d = s.get( MedicalHistory.class, e.id);
		assertEquals( e.id, d.id );
		s.remove( d );
		s.remove( d.patient );
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

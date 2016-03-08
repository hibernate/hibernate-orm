/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.criteria;

import java.util.Collections;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Dragan Bozanovic (draganb)
 */
public class InTest extends BaseCoreFunctionalTestCase {

	public String[] getMappings() {
		return new String[]{ "criteria/Person.hbm.xml" };
	}

	@Test
	public void testIn() {
		Session session = openSession();
		Transaction tx = session.beginTransaction();
		session.save( new Woman() );
		session.save( new Man() );
		session.flush();
		tx.commit();
		session.close();
		session = openSession();
		tx = session.beginTransaction();
		List persons = session.createCriteria( Person.class ).add(
				Restrictions.in( "class", Collections.singleton( Woman.class ) ) ).list();
		assertEquals( 1, persons.size() );
		tx.rollback();
		session.close();
	}
}

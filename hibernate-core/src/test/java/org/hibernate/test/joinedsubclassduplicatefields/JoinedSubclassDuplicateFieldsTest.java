/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.joinedsubclassduplicatefields;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author pholvs
 */
public class JoinedSubclassDuplicateFieldsTest extends BaseCoreFunctionalTestCase {
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MySuperClass.class, SubClassA.class, SubClassB.class};
	}

	@Test
	public void queryOnSuperClass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		SubClassA objA = new SubClassA();
		objA.id = 1L;
		objA.fieldOnChild = "foobar";

		SubClassB objB = new SubClassB();
		objB.id = 2L;
		objB.fieldOnChild = "foobar";

		SubClassA objC = new SubClassA();
		objC.id = 3L;
		objC.fieldOnChild = "qwerty";

		SubClassB objD = new SubClassB();
		objD.id = 4L;
		objD.fieldOnChild = "zxcv";

		s.save(objA);
		s.save(objB);
		s.save(objC);
		s.save(objD);

		try {
			Query q = s.createQuery("from MySuperClass c"
					+ " where c.fieldOnChild = 'foobar'");
			List l = q.getResultList();
			assertEquals(2, l.size());
		} finally {

			s.delete(objA);
			s.delete(objB);
			s.delete(objC);
			s.delete(objD);

			t.commit();
			s.close();
		}
	}

	@Test
	public void queryConstrainedSubclass() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		SubClassA objA = new SubClassA();
		objA.id = 1L;
		objA.fieldOnChild = "foobar";

		SubClassB objB = new SubClassB();
		objB.id = 2L;
		objB.fieldOnChild = "foobar";

		SubClassA objC = new SubClassA();
		objC.id = 3L;
		objC.fieldOnChild = "qwerty";

		SubClassB objD = new SubClassB();
		objD.id = 4L;
		objD.fieldOnChild = "zxcv";

		s.save(objA);
		s.save(objB);
		s.save(objC);
		s.save(objD);

		try {
			Query q = s.createQuery("from MySuperClass c"
					+ " where c.fieldOnChild = 'foobar'"
					+ "and TYPE(c) = SubClassB");
			List l = q.getResultList();
			assertEquals(1, l.size());
		} finally {

			s.delete(objA);
			s.delete(objB);
			s.delete(objC);
			s.delete(objD);

			t.commit();
			s.close();
		}
	}
	
}


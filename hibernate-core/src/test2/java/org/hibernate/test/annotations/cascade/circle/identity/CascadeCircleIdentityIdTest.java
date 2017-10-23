/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.cascade.circle.identity;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class CascadeCircleIdentityIdTest extends BaseCoreFunctionalTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-5472" )
	public void testCascade() {
		A a = new A();
		B b = new B();
		C c = new C();
		D d = new D();
		E e = new E();
		F f = new F();
		G g = new G();
		H h = new H();

		a.getBCollection().add(b);
		b.setA(a);
		
		a.getCCollection().add(c);
		c.setA(a);
		
		b.getCCollection().add(c);
		c.setB(b);
		
		a.getDCollection().add(d);
		d.getACollection().add(a);
		
		d.getECollection().add(e);
		e.setF(f);
		
		f.getBCollection().add(b);
		b.setF(f);
		
		c.setG(g);
		g.getCCollection().add(c);
		
		f.setH(h);
		h.setG(g);
		
		Session s;
		s = openSession();
		s.getTransaction().begin();
		try {
			// Fails: says that C.b is null (even though it isn't). Doesn't fail if you persist c, g or h instead of a
			s.persist(a);
			s.flush();
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				A.class,
				B.class,
				C.class,
				D.class,
				E.class,
				F.class,
				G.class,
				H.class
		};
	}

}

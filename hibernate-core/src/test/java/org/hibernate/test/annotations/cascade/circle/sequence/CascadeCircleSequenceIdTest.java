/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.cascade.circle.sequence;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

@RequiresDialectFeature(DialectChecks.SupportsSequences.class)
public class CascadeCircleSequenceIdTest extends BaseCoreFunctionalTestCase {
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.unidir;

import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Gavin King
 */
public class BackrefTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "unidir/ParentChild.hbm.xml" };
	}
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		// No test needed at this time.  This was purely to test a
		// validation issue from HHH-5836.
		return new Class<?>[] { Parent1.class, Child1.class, Child2.class };
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	public void testBackRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("Marc");
		Parent p2 = new Parent("Nathalie");
		Child c = new Child("Elvira");
		Child c2 = new Child("Blase");
		p.getChildren().add(c);
		p.getChildren().add(c2);
		s.persist(p);
		s.persist(p2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Child) s.get(Child.class, "Elvira");
		c.setAge(2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = (Parent) s.get(Parent.class, "Marc");
		c = (Child) s.get(Child.class, "Elvira");
		c.setAge(18);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = (Parent) s.get(Parent.class, "Marc");
		p2 = (Parent) s.get(Parent.class, "Nathalie");
		c = (Child) s.get(Child.class, "Elvira");
		assertEquals( p.getChildren().indexOf(c), 0 );
		p.getChildren().remove(c);
		p2.getChildren().add(c);
		t.commit();

		s.close();
		s = openSession();
		t = s.beginTransaction();
		Parent p3 = new Parent("Marion");
		p3.getChildren().add( new Child("Gavin") );
		s.merge(p3);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete from Child" ).executeUpdate();
		s.createQuery( "delete from Parent" ).executeUpdate();
		t.commit();
		s.close();
	}

	@Test
	public void testBackRefToProxiedEntityOnMerge() {
		Session s = openSession();
		s.beginTransaction();
		Parent me = new Parent( "Steve" );
		me.getChildren().add( new Child( "Joe" ) );
  		s.persist( me );
		s.getTransaction().commit();
		s.close();

		// while detached, add a new element
		me.getChildren().add( new Child( "Cece" ) );
		me.getChildren().add( new Child( "Austin" ) );

		s = openSession();
		s.beginTransaction();
		// load 'me' to associate it with the new session as a proxy (this may have occurred as 'prior work'
		// to the reattachment below)...
		Object meProxy = s.load( Parent.class, me.getName() );
		assertFalse( Hibernate.isInitialized( meProxy ) );
		// now, do the reattchment...
		s.merge( me );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete from Child" ).executeUpdate();
		s.createQuery( "delete from Parent" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}


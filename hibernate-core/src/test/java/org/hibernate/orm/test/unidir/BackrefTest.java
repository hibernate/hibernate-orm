/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unidir;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.CollectionClassification;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Gavin King
 */
public class BackrefTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

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
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}

	@Test
	public void testBackRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("Marc", 123456789);
		Parent p2 = new Parent("Nathalie", 987654321 );
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
		Parent p3 = new Parent("Marion", 543216789);
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
		Parent me = new Parent( "Steve", 192837465 );
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
		Object meProxy = s.getReference( Parent.class, me.getName() );
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

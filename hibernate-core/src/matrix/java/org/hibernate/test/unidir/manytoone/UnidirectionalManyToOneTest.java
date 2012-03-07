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
package org.hibernate.test.unidir.manytoone;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Gavin King
 */
public class UnidirectionalManyToOneTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "unidir/manytoone/ParentChild.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( USE_NEW_METADATA_MAPPINGS, "true");
	}

	@Override
	public String getCacheConcurrencyStrategy() {
		return null;
	}

	@Test
	public void testUnidirectionalManyToOne() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("Marc");
		Parent p2 = new Parent("Nathalie");
		Child c = new Child("Elvira");
		Child c2 = new Child("Blase");
		c.setParent( p );
		c2.setParent( p );
		s.save( p );
		s.save(p2);
		s.save( c );
		s.save( c2 );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Child) s.get( Child.class, "Elvira");
		p = c.getParent();
		assertEquals( "Marc", p.getName() );
		c2 = (Child) s.get( Child.class, "Blase" );
		assertSame( p, c2.getParent() );
		p2 = (Parent) s.get(Parent.class, "Nathalie");
		c.setParent( p2 );
		t.commit();

		s = openSession();
		t = s.beginTransaction();
		c = (Child) s.get( Child.class, "Elvira");
		p2 = c.getParent();
		assertEquals( "Nathalie", p2.getName() );
		c2 = (Child) s.get( Child.class, "Blase" );
		p = c2.getParent();
		assertEquals( "Marc", p.getName() );
		t.commit();
		
		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete from org.hibernate.test.unidir.manytoone.Child" ).executeUpdate();
		s.createQuery( "delete from org.hibernate.test.unidir.manytoone.Parent" ).executeUpdate();
		t.commit();
		s.close();
	}
}


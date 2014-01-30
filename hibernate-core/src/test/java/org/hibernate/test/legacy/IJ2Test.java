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
package org.hibernate.test.legacy;
import java.io.Serializable;

import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class IJ2Test extends LegacyTestCase {

	public String[] getMappings() {
		return new String[] { "legacy/IJ2.hbm.xml" };
	}

	@SuppressWarnings( {"UnusedAssignment"})
	@Test
	@FailureExpectedWithNewMetamodel
	public void testUnionSubclass() throws Exception {
		Session s = sessionFactory().openSession();
		s.beginTransaction();
		I i = new I();
		i.setName( "i" );
		i.setType( 'a' );
		J j = new J();
		j.setName( "j" );
		j.setType( 'x' );
		j.setAmount( 1.0f );
		Serializable iid = s.save(i);
		Serializable jid = s.save(j);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( I.class );

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(I.class, jid);
		j = (J) s.get(J.class, jid);
		i = (I) s.get(I.class, iid);
		assertTrue( i.getClass()==I.class );
		j.setAmount( 0.5f );
		s.lock(i, LockMode.UPGRADE);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( I.class );

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(J.class, jid);
		j = (J) s.get(I.class, jid);
		i = (I) s.get(I.class, iid);
		assertTrue( i.getClass()==I.class );
		j.setAmount( 0.5f );
		s.lock(i, LockMode.UPGRADE);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( I.class );

		s = sessionFactory().openSession();
		s.beginTransaction();
		assertTrue( s.createQuery( "from I" ).list().size()==2 );
		assertTrue( s.createQuery( "from J" ).list().size()==1 );
		assertTrue( s.createQuery( "from J j where j.amount > 0 and j.name is not null" ).list().size()==1 );
		assertTrue( s.createQuery( "from I i where i.class = org.hibernate.test.legacy.I" ).list().size()==1 );
		assertTrue( s.createQuery( "from I i where i.class = J" ).list().size()==1 );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( I.class );

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(J.class, jid);
		i = (I) s.get(I.class, iid);
		K k = new K();
		Serializable kid = s.save(k);
		i.setParent(k);
		j.setParent(k);
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( I.class );

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(J.class, jid);
		i = (I) s.get(I.class, iid);
		k = (K) s.get(K.class, kid);
		System.out.println(k + "=" + i.getParent());
		assertTrue( i.getParent()==k );
		assertTrue( j.getParent()==k );
		assertTrue( k.getIs().size()==2 );
		s.getTransaction().commit();
		s.close();

		sessionFactory().getCache().evictEntityRegion( I.class );

		s = sessionFactory().openSession();
		s.beginTransaction();
		assertTrue( s.createQuery( "from K k inner join k.is i where i.name = 'j'" ).list().size()==1 );
		assertTrue( s.createQuery( "from K k inner join k.is i where i.name = 'i'" ).list().size()==1 );
		assertTrue( s.createQuery( "from K k left join fetch k.is" ).list().size()==2 );
		s.getTransaction().commit();
		s.close();

		s = sessionFactory().openSession();
		s.beginTransaction();
		j = (J) s.get(J.class, jid);
		i = (I) s.get(I.class, iid);
		k = (K) s.get(K.class, kid);
		s.delete(k);
		s.delete(j);
		s.delete(i);
		s.getTransaction().commit();
		s.close();
	}
}

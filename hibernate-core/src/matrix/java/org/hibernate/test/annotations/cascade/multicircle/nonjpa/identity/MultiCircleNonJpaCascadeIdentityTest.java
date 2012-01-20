/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.annotations.cascade.multicircle.nonjpa.identity;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * This test uses a complicated model that requires Hibernate to delay
 * inserts until non-nullable transient entity dependencies are resolved.
 *
 * All IDs are generated from identity columns.
 *
 * Hibernate cascade types are used (org.hibernate.annotations.CascadeType)..
 *
 * This test uses the following model:
 *
 * <code>
 *     ------------------------------ N G
 *     |
 *     |                                1
 *     |                                |
 *     |                                |
 *     |                                N
 *     |
 *     |         E N--------------0,1 * F
 *     |
 *     |         1                      N
 *     |         |                      |
 *     |         |                      |
 *     1         N                      |
 *     *                                |
 *     B * N---1 D * 1------------------
 *     *
 *     N         N
 *     |         |
 *     |         |
 *     1         |
 *               |
 *     C * 1-----
 *</code>
 *
 * In the diagram, all associations are bidirectional;
 * assocations marked with '*' cascade persist, save, merge operations to the
 * associated entities (e.g., B cascades persist to D, but D does not cascade
 * persist to B);
 *
 * b, c, d, e, f, and g are all transient unsaved that are associated with each other.
 *
 * When saving b, the entities are added to the ActionQueue in the following order:
 * c, d (depends on e), f (depends on d, g), e, b, g.
 *
 * Entities are inserted in the following order:
 * c, e, d, b, g, f.
 */
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class MultiCircleNonJpaCascadeIdentityTest extends BaseCoreFunctionalTestCase {
	private B b;
	private C c;
	private D d;
	private E e;
	private F f;
	private G g;

	@Before
	public void setup() {
		b = new B();
		c = new C();
		d = new D();
		e = new E();
		f = new F();
		g = new G();

		b.getGCollection().add( g );
		b.setC( c );
		b.setD( d );

		c.getBCollection().add( b );
		c.getDCollection().add( d );

		d.getBCollection().add( b );
		d.setC( c );
		d.setE( e );
		d.getFCollection().add( f );

		e.getDCollection().add( d );
		e.setF( f );

		f.getECollection().add( e );
		f.setD( d );
		f.setG( g );

		g.setB( b );
		g.getFCollection().add( f );
	}

	@After
	public void cleanup() {
		b.setC( null );
		b.setD( null );
		b.getGCollection().remove( g );

		c.getBCollection().remove( b );
		c.getDCollection().remove( d );

		d.getBCollection().remove( b );
		d.setC( null );
		d.setE( null );
		d.getFCollection().remove( f );

		e.getDCollection().remove( d );
		e.setF( null );

		f.setD( null );
		f.getECollection().remove( e );
		f.setG( null );

		g.setB( null );
		g.getFCollection().remove( f );

		Session s = openSession();
		s.getTransaction().begin();
		b = ( B ) s.merge( b );
		c = ( C ) s.merge( c );
		d = ( D ) s.merge( d );
		e = ( E ) s.merge( e );
		f = ( F ) s.merge( f );
		g = ( G ) s.merge( g );
		s.delete( f );
		s.delete( g );
		s.delete( b );
		s.delete( d );
		s.delete( e );
		s.delete( c );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testPersist() {
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( b );
		s.getTransaction().commit();
		s.close();

		check();
	}

	@Test
	public void testSave() {
		Session s = openSession();
		s.getTransaction().begin();
		s.save( b );
		s.getTransaction().commit();
		s.close();

		check();
	}

	@Test
	public void testSaveOrUpdate() {
		Session s = openSession();
		s.getTransaction().begin();
		s.saveOrUpdate( b );
		s.getTransaction().commit();
		s.close();

		check();
	}

	@Test
	public void testMerge() {
		Session s = openSession();
		s.getTransaction().begin();
		b = ( B ) s.merge( b );
		c = b.getC();
		d = b.getD();
		e = d.getE();
		f = e.getF();
		g = f.getG();
		s.getTransaction().commit();
		s.close();

		check();
	}

	private void check() {
		Session s = openSession();
		s.getTransaction().begin();
		B bRead = ( B ) s.get( B.class, b.getId() );
		Assert.assertEquals( b, bRead );

		G gRead = bRead.getGCollection().iterator().next();
		Assert.assertEquals( g, gRead );
		C cRead = bRead.getC();
		Assert.assertEquals( c, cRead );
		D dRead = bRead.getD();
		Assert.assertEquals( d, dRead );

		Assert.assertSame( bRead, cRead.getBCollection().iterator().next() );
		Assert.assertSame( dRead, cRead.getDCollection().iterator().next() );

		Assert.assertSame( bRead, dRead.getBCollection().iterator().next() );
		Assert.assertEquals( cRead, dRead.getC() );
		E eRead = dRead.getE();
		Assert.assertEquals( e, eRead );
		F fRead = dRead.getFCollection().iterator().next();
		Assert.assertEquals( f, fRead );

		Assert.assertSame( dRead, eRead.getDCollection().iterator().next() );
		Assert.assertSame( fRead, eRead.getF() );

		Assert.assertSame( eRead, fRead.getECollection().iterator().next() );
		Assert.assertSame( dRead, fRead.getD() );
		Assert.assertSame( gRead, fRead.getG());

		Assert.assertSame( bRead, gRead.getB() );
		Assert.assertSame( fRead, gRead.getFCollection().iterator().next() );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{
				B.class,
				C.class,
				D.class,
				E.class,
				F.class,
				G.class
		};
	}

}

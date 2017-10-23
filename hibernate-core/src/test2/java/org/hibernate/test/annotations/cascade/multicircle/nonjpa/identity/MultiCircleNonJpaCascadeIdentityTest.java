/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	private EntityB b;
	private EntityC c;
	private EntityD d;
	private EntityE e;
	private EntityF f;
	private EntityG g;

	@Before
	public void setup() {
		b = new EntityB();
		c = new EntityC();
		d = new EntityD();
		e = new EntityE();
		f = new EntityF();
		g = new EntityG();

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
		b = (EntityB) s.merge( b );
		c = (EntityC) s.merge( c );
		d = (EntityD) s.merge( d );
		e = (EntityE) s.merge( e );
		f = (EntityF) s.merge( f );
		g = (EntityG) s.merge( g );
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
		b = ( EntityB ) s.merge( b );
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
		EntityB bRead = (EntityB) s.get( EntityB.class, b.getId() );
		Assert.assertEquals( b, bRead );

		EntityG gRead = bRead.getGCollection().iterator().next();
		Assert.assertEquals( g, gRead );
		EntityC cRead = bRead.getC();
		Assert.assertEquals( c, cRead );
		EntityD dRead = bRead.getD();
		Assert.assertEquals( d, dRead );

		Assert.assertSame( bRead, cRead.getBCollection().iterator().next() );
		Assert.assertSame( dRead, cRead.getDCollection().iterator().next() );

		Assert.assertSame( bRead, dRead.getBCollection().iterator().next() );
		Assert.assertEquals( cRead, dRead.getC() );
		EntityE eRead = dRead.getE();
		Assert.assertEquals( e, eRead );
		EntityF fRead = dRead.getFCollection().iterator().next();
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
				EntityB.class,
				EntityC.class,
				EntityD.class,
				EntityE.class,
				EntityF.class,
				EntityG.class
		};
	}

}

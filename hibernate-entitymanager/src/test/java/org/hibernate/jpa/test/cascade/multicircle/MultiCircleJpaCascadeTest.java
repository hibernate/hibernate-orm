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
package org.hibernate.jpa.test.cascade.multicircle;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.TransientPropertyValueException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This test uses a complicated model that requires Hibernate to delay
 * inserts until non-nullable transient entity dependencies are resolved.
 *
 * All IDs are generated from a sequence.
 *
 * JPA cascade types are used (javax.persistence.CascadeType)..
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
public class MultiCircleJpaCascadeTest extends BaseEntityManagerFunctionalTestCase {
	private B b;
	private C c;
	private D d;
	private E e;
	private F f;
	private G g;
	private boolean skipCleanup;

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

		skipCleanup = false;
	}

	@After
	public void cleanup() {
		if ( ! skipCleanup ) {
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

			EntityManager em = getOrCreateEntityManager();
			em.getTransaction().begin();
			b = em.merge( b );
			c = em.merge( c );
			d = em.merge( d );
			e = em.merge( e );
			f = em.merge( f );
			g = em.merge( g );
			em.remove( f );
			em.remove( g );
			em.remove( b );
			em.remove( d );
			em.remove( e );
			em.remove( c );
			em.getTransaction().commit();
			em.close();
		}
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testPersist() {

		// no idea why this fails with new metamodel

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( b );
		em.getTransaction().commit();
		em.close();

		check();
	}

	@Test
	@FailureExpectedWithNewMetamodel
	public void testPersistNoCascadeToTransient() {

		// no idea why this fails with new metamodel

		skipCleanup = true;
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		try {
			em.persist( c );
			fail( "should have failed." );
		}
		catch( IllegalStateException ex ) {
			assertTrue( TransientPropertyValueException.class.isInstance( ex.getCause() ) );
			TransientPropertyValueException pve = (TransientPropertyValueException) ex.getCause();
			assertEquals( G.class.getName(), pve.getTransientEntityName() );
			assertEquals( F.class.getName(),  pve.getPropertyOwnerEntityName() );
			assertEquals( "g", pve.getPropertyName() );
		}
		em.getTransaction().rollback();
		em.close();
	}

	@Test
	@FailureExpected( jiraKey = "HHH-6999" )
	// fails on d.e; should pass
	public void testPersistThenUpdate() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( b );
		// remove old e from associations
		e.getDCollection().remove( d );
		d.setE( null );
		f.getECollection().remove( e );
		e.setF( null );
		// add new e to associations
		e = new E();
		e.getDCollection().add( d );
		f.getECollection().add( e );
		d.setE( e );
		e.setF( f );
		em.getTransaction().commit();
		em.close();

		check();
	}

	@Test
	public void testPersistThenUpdateNoCascadeToTransient() {
		// expected to fail, so nothing will be persisted.
		skipCleanup = true;

		// remove elements from collections and persist
		c.getBCollection().clear();
		c.getDCollection().clear();

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( c );
		// now add the elements back
		c.getBCollection().add( b );
		c.getDCollection().add( d );
		try {
			em.getTransaction().commit();
			fail( "should have thrown IllegalStateException");
		}
		catch ( RollbackException ex ) {
			assertTrue( ex.getCause() instanceof IllegalStateException );
			IllegalStateException ise = ( IllegalStateException ) ex.getCause();
			// should fail on entity g (due to no cascade to f.g);
			// instead it fails on entity e ( due to no cascade to d.e)
			// because e is not in the process of being saved yet.
			// when HHH-6999 is fixed, this test should be changed to
			// check for g and f.g
			assertTrue( ise.getCause() instanceof TransientPropertyValueException );
			TransientPropertyValueException tpve = ( TransientPropertyValueException ) ise.getCause();
			assertEquals( E.class.getName(), tpve.getTransientEntityName() );
			assertEquals( D.class.getName(), tpve.getPropertyOwnerEntityName() );
			assertEquals( "e", tpve.getPropertyName() );
		}
		em.close();
	}

	@Test
	public void testMerge() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		b = em.merge( b );
		c = b.getC();
		d = b.getD();
		e = d.getE();
		f = e.getF();
		g = f.getG();
		em.getTransaction().commit();
		em.close();

		check();
	}

	private void check() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		B bRead = em.find( B.class, b.getId() );
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

		em.getTransaction().commit();
		em.close();
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

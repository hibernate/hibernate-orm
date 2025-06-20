/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade.multicircle;

import jakarta.persistence.RollbackException;

import org.hibernate.TransientObjectException;
import org.hibernate.TransientPropertyValueException;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * This test uses a complicated model that requires Hibernate to delay
 * inserts until non-nullable transient entity dependencies are resolved.
 * <p>
 * All IDs are generated from a sequence.
 * <p>
 * JPA cascade types are used (jakarta.persistence.CascadeType).
 * <p>
 * This test uses the following model:
 *
 * <code>
 * ------------------------------ N G
 * |
 * |                                1
 * |                                |
 * |                                |
 * |                                N
 * |
 * |         E N--------------0,1 * F
 * |
 * |         1                      N
 * |         |                      |
 * |         |                      |
 * 1         N                      |
 * *                                |
 * B * N---1 D * 1------------------
 * *
 * N         N
 * |         |
 * |         |
 * 1         |
 * |
 * C * 1-----
 * </code>
 * <p>
 * In the diagram, all associations are bidirectional;
 * assocations marked with '*' cascade persist, save, merge operations to the
 * associated entities (e.g., B cascades persist to D, but D does not cascade
 * persist to B);
 * <p>
 * b, c, d, e, f, and g are all transient unsaved that are associated with each other.
 * <p>
 * When saving b, the entities are added to the ActionQueue in the following order:
 * c, d (depends on e), f (depends on d, g), e, b, g.
 * <p>
 * Entities are inserted in the following order:
 * c, e, d, b, g, f.
 */
@Jpa(
		annotatedClasses = {
				B.class,
				C.class,
				D.class,
				E.class,
				F.class,
				G.class
		}
)
public class MultiCircleJpaCascadeTest {
	private B b;
	private C c;
	private D d;
	private E e;
	private F f;
	private G g;

	@BeforeEach
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

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testPersist(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( b );
				}
		);

		check( scope );
	}

	@Test
	public void testPersistNoCascadeToTransient(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					entityManager.getTransaction().begin();
					try {
						entityManager.persist( c );
						fail( "should have failed." );
					}
					catch (IllegalStateException ex) {
						assertTrue( TransientPropertyValueException.class.isInstance( ex.getCause() ) );
						TransientPropertyValueException pve = (TransientPropertyValueException) ex.getCause();
						assertEquals( G.class.getName(), pve.getTransientEntityName() );
						assertEquals( F.class.getName(), pve.getPropertyOwnerEntityName() );
						assertEquals( "g", pve.getPropertyName() );
					} finally {
						entityManager.getTransaction().rollback();
						entityManager.close();
					}
				}
		);
	}

	@Test
	public void testPersistThenUpdate(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					entityManager.persist( b );
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
				}
		);

		check( scope );
	}

	@Test
	public void testPersistThenUpdateNoCascadeToTransient1(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					// remove elements from collections and persist
					c.getBCollection().clear();
					c.getDCollection().clear();

					entityManager.getTransaction().begin();
					entityManager.persist( c );
					// now add the elements back
					c.getBCollection().add( b );
					c.getDCollection().add( d );
					try {
						entityManager.getTransaction().commit();
						fail( "should have thrown IllegalStateException" );
					}
					catch (RollbackException ex) {
						assertTrue( ex.getCause() instanceof IllegalStateException );
						IllegalStateException ise = (IllegalStateException) ex.getCause();
						assertTyping( TransientObjectException.class, ise.getCause() );
//						String message = ise.getCause().getMessage();
//						assertTrue( message.contains("'org.hibernate.orm.test.jpa.cascade.multicircle.B'") );
					}
					finally {
						entityManager.getTransaction().rollback();
						entityManager.close();
					}
				}
		);
	}

	@Test
	public void testPersistThenUpdateNoCascadeToTransient2(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					// remove elements from collections and persist
					c.getBCollection().clear();
					c.getDCollection().clear();

					entityManager.getTransaction().begin();
					entityManager.persist( c );
					entityManager.persist( b );
					// now add the elements back
					c.getBCollection().add( b );
					c.getDCollection().add( d );
					entityManager.getTransaction().commit();
				}
		);
	}

	@Test
	public void testPersistThenUpdateNoCascadeToTransient3(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					// remove elements from collections and persist
					c.getBCollection().clear();
					c.getDCollection().clear();
					d.getBCollection().clear();

					entityManager.getTransaction().begin();
					entityManager.persist( c );
					// now add the elements back
					c.getDCollection().add( d );
					try {
						entityManager.getTransaction().commit();
						fail( "should have thrown IllegalStateException" );
					}
					catch (RollbackException ex) {
						assertTrue( ex.getCause() instanceof IllegalStateException );
						IllegalStateException ise = (IllegalStateException) ex.getCause();
						assertTyping( TransientObjectException.class, ise.getCause() );
						String message = ise.getCause().getMessage();
						assertTrue( message.contains("org.hibernate.orm.test.jpa.cascade.multicircle") );
					}
					finally {
						entityManager.getTransaction().rollback();
						entityManager.close();
					}
				}
		);
	}

	@Test
	public void testMerge(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					b = entityManager.merge( b );
					c = b.getC();
					d = b.getD();
					e = d.getE();
					f = e.getF();
					g = f.getG();
				}
		);

		check( scope );
	}

	private void check(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					B bRead = entityManager.find( B.class, b.getId() );
					assertEquals( b, bRead );

					G gRead = bRead.getGCollection().iterator().next();
					assertEquals( g, gRead );
					C cRead = bRead.getC();
					assertEquals( c, cRead );
					D dRead = bRead.getD();
					assertEquals( d, dRead );

					assertSame( bRead, cRead.getBCollection().iterator().next() );
					assertSame( dRead, cRead.getDCollection().iterator().next() );

					assertSame( bRead, dRead.getBCollection().iterator().next() );
					assertEquals( cRead, dRead.getC() );
					E eRead = dRead.getE();
					assertEquals( e, eRead );
					F fRead = dRead.getFCollection().iterator().next();
					assertEquals( f, fRead );

					assertSame( dRead, eRead.getDCollection().iterator().next() );
					assertSame( fRead, eRead.getF() );

					assertSame( eRead, fRead.getECollection().iterator().next() );
					assertSame( dRead, fRead.getD() );
					assertSame( gRead, fRead.getG() );

					assertSame( bRead, gRead.getB() );
					assertSame( fRead, gRead.getFCollection().iterator().next() );
				}
		);
	}
}

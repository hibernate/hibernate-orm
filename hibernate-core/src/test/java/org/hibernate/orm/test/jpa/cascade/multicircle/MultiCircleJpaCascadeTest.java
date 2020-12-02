/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.cascade.multicircle;

import javax.persistence.RollbackException;

import org.hibernate.TransientPropertyValueException;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.FailureExpected;
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
 * JPA cascade types are used (javax.persistence.CascadeType)..
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
	private boolean skipCleanup;

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

		skipCleanup = false;
	}

	@AfterEach
	public void cleanup(EntityManagerFactoryScope scope) {
		if ( !skipCleanup ) {
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

			scope.inTransaction(
					entityManager -> {
						b = entityManager.merge( b );
						c = entityManager.merge( c );
						d = entityManager.merge( d );
						e = entityManager.merge( e );
						f = entityManager.merge( f );
						g = entityManager.merge( g );
						entityManager.remove( f );
						entityManager.remove( g );
						entityManager.remove( b );
						entityManager.remove( d );
						entityManager.remove( e );
						entityManager.remove( c );
					}
			);
		}
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
		skipCleanup = true;
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
					}
					entityManager.getTransaction().rollback();
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-6999")	// remove skipCleanup below when this annotation is removed, added it to avoid failure in the cleanup
	// fails on d.e; should pass
	public void testPersistThenUpdate(EntityManagerFactoryScope scope) {
		skipCleanup = true;
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
	public void testPersistThenUpdateNoCascadeToTransient(EntityManagerFactoryScope scope) {
		// expected to fail, so nothing will be persisted.
		skipCleanup = true;

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
						// should fail on entity g (due to no cascade to f.g);
						// instead it fails on entity e ( due to no cascade to d.e)
						// because e is not in the process of being saved yet.
						// when HHH-6999 is fixed, this test should be changed to
						// check for g and f.g
						//noinspection ThrowableResultOfMethodCallIgnored
						TransientPropertyValueException tpve = assertTyping(
								TransientPropertyValueException.class,
								ise.getCause()
						);
						assertEquals( E.class.getName(), tpve.getTransientEntityName() );
						assertEquals( D.class.getName(), tpve.getPropertyOwnerEntityName() );
						assertEquals( "e", tpve.getPropertyName() );
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

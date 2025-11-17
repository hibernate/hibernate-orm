/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.orm.test.cascade.A;
import org.hibernate.orm.test.cascade.G;
import org.hibernate.orm.test.cascade.H;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ovidiu Feodorov
 * @author Gail Badner
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/cascade/MultiPathCascade.hbm.xml"
		}
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class MultiPathCascadeTest {


	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMultiPathMergeModifiedDetached(SessionFactoryScope scope) {
		// persist a simple A in the database
		A a = new A();
		a.setData( "Anna" );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		// modify detached entity
		modifyEntity( a );

		scope.inTransaction(
				session -> session.merge( a )
		);

		verifyModifications( scope, a.getId() );
	}

	@Test
	public void testMultiPathMergeModifiedDetachedIntoProxy(SessionFactoryScope scope) {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		// modify detached entity
		modifyEntity( a );

		scope.inTransaction(
				session -> {
					A aLoaded = session.getReference( A.class, new Long( a.getId() ) );
					assertInstanceOf( HibernateProxy.class, aLoaded );
					assertSame( aLoaded, session.merge( a ) );
				}
		);

		verifyModifications( scope, a.getId() );
	}

	@Test
	public void testMultiPathUpdateModifiedDetached(SessionFactoryScope scope) {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		// modify detached entity
		modifyEntity( a );

		A merged = scope.fromTransaction(
				session ->
						session.merge( a )
		);

		verifyModifications( scope, merged.getId() );
	}

	@Test
	public void testMultiPathGetAndModify(SessionFactoryScope scope) {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		scope.inTransaction(
				session -> {
					A result = session.get( A.class, new Long( a.getId() ) );
					modifyEntity( result );
				}
		);
		// retrieve the previously saved instance from the database, and update it

		verifyModifications( scope, a.getId() );
	}

	@Test
	public void testMultiPathMergeNonCascadedTransientEntityInCollection(SessionFactoryScope scope) {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		// modify detached entity
		modifyEntity( a );

		A merged = scope.fromTransaction(
				session ->
						(A) session.merge( a )
		);

		verifyModifications( scope, merged.getId() );

		// add a new (transient) G to collection in h
		// there is no cascade from H to the collection, so this should fail when merged
		assertEquals( 1, merged.getHs().size() );

		H h = (H) merged.getHs().iterator().next();

		G gNew = new G();
		gNew.setData( "Gail" );
		gNew.getHs().add( h );
		h.getGs().add( gNew );

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						session.merge( merged );
						session.merge( h );
						session.getTransaction().commit();
						fail( "should have thrown IllegalStateException" );
					}
					catch (IllegalStateException expected) {
						// expected
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	public void testMultiPathMergeNonCascadedTransientEntityInOneToOne(SessionFactoryScope scope) {
		// persist a simple A in the database
		A a = new A();
		a.setData( "Anna" );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		// modify detached entity
		modifyEntity( a );

		A merged = scope.fromTransaction(
				session ->
						(A) session.merge( a )
		);

		verifyModifications( scope, merged.getId() );

		// change the one-to-one association from g to be a new (transient) A
		// there is no cascade from G to A, so this should fail when merged
		G g = merged.getG();
		a.setG( null );
		A aNew = new A();
		aNew.setData( "Alice" );
		g.setA( aNew );
		aNew.setG( g );

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						session.merge( merged );
						session.merge( g );
						session.getTransaction().commit();
						fail( "should have thrown IllegalStateException" );
					}
					catch (IllegalStateException expected) {
						// expected
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	public void testMultiPathMergeNonCascadedTransientEntityInManyToOne(SessionFactoryScope scope) {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		scope.inTransaction(
				session ->
						session.persist( a )
		);

		// modify detached entity
		modifyEntity( a );

		A merged = scope.fromTransaction(
				session ->
						(A) session.merge( a )
		);

		verifyModifications( scope, a.getId() );

		// change the many-to-one association from h to be a new (transient) A
		// there is no cascade from H to A, so this should fail when merged
		assertEquals( 1, merged.getHs().size() );
		H h = (H) merged.getHs().iterator().next();
		merged.getHs().remove( h );
		A aNew = new A();
		aNew.setData( "Alice" );
		aNew.addH( h );

		scope.inSession(
				session -> {
					session.beginTransaction();
					try {
						session.merge( merged );
						session.merge( h );
						session.getTransaction().commit();
						fail( "should have thrown IllegalStateException" );
					}
					catch (IllegalStateException expected) {
						// expected
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	private void modifyEntity(A a) {
		// create a *circular* graph in detached entity
		a.setData( "Anthony" );

		G g = new G();
		g.setData( "Giovanni" );

		H h = new H();
		h.setData( "Hellen" );

		a.setG( g );
		g.setA( a );

		a.getHs().add( h );
		h.setA( a );

		g.getHs().add( h );
		h.getGs().add( g );
	}

	private void verifyModifications(SessionFactoryScope scope, long aId) {
		scope.inTransaction(
				session -> {
					// retrieve the A object and check it
					A a = session.get( A.class, new Long( aId ) );
					assertEquals( aId, a.getId() );
					assertEquals( "Anthony", a.getData() );
					assertNotNull( a.getG() );
					assertNotNull( a.getHs() );
					assertEquals( 1, a.getHs().size() );

					G gFromA = a.getG();
					H hFromA = (H) a.getHs().iterator().next();

					// check the G object
					assertEquals( "Giovanni", gFromA.getData() );
					assertSame( a, gFromA.getA() );
					assertNotNull( gFromA.getHs() );
					assertEquals( a.getHs(), gFromA.getHs() );
					assertSame( hFromA, gFromA.getHs().iterator().next() );

					// check the H object
					assertEquals( "Hellen", hFromA.getData() );
					assertSame( a, hFromA.getA() );
					assertNotNull( hFromA.getGs() );
					assertEquals( 1, hFromA.getGs().size() );
					assertSame( gFromA, hFromA.getGs().iterator().next() );
				}
		);

	}

}

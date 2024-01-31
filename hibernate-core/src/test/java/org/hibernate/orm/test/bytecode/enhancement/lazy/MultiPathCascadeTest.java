/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.orm.test.cascade.A;
import org.hibernate.orm.test.cascade.G;
import org.hibernate.orm.test.cascade.H;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Ovidiu Feodorov
 * @author Gail Badner
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class MultiPathCascadeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] {
				"org/hibernate/orm/test/cascade/MultiPathCascade.hbm.xml"
		};
	}

	@After
	public void cleanupTest() {
		inTransaction(
				session -> {
					session.createQuery( "delete from A" );
					session.createQuery( "delete from G" );
					session.createQuery( "delete from H" );
				}
		);
	}

	@Test
	public void testMultiPathMergeModifiedDetached() {
		// persist a simple A in the database
		A a = new A();
		a.setData( "Anna" );

		inTransaction(
				session ->
						session.save( a )
		);

		// modify detached entity
		modifyEntity( a );

		inTransaction(
				session -> session.merge( a )
		);

		verifyModifications( a.getId() );
	}

	@Test
	public void testMultiPathMergeModifiedDetachedIntoProxy() {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		inTransaction(
				session ->
						session.save( a )
		);

		// modify detached entity
		modifyEntity( a );

		inTransaction(
				session -> {
					A aLoaded = session.load( A.class, new Long( a.getId() ) );
					assertTrue( aLoaded instanceof HibernateProxy );
					assertSame( aLoaded, session.merge( a ) );
				}
		);

		verifyModifications( a.getId() );
	}

	@Test
	public void testMultiPathUpdateModifiedDetached() {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		inTransaction(
				session ->
						session.save( a )
		);

		// modify detached entity
		modifyEntity( a );

		inTransaction(
				session ->
						session.update( a )
		);

		verifyModifications( a.getId() );
	}

	@Test
	public void testMultiPathGetAndModify() {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		inTransaction(
				session ->
						session.save( a )
		);

		inTransaction(
				session -> {
					A result = session.get( A.class, new Long( a.getId() ) );
					modifyEntity( result );
				}
		);
		// retrieve the previously saved instance from the database, and update it

		verifyModifications( a.getId() );
	}

	@Test
	public void testMultiPathMergeNonCascadedTransientEntityInCollection() {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		inTransaction(
				session ->
						session.save( a )
		);

		// modify detached entity
		modifyEntity( a );

		A merged = fromTransaction(
				session ->
						(A) session.merge( a )
		);

		verifyModifications( merged.getId() );

		// add a new (transient) G to collection in h
		// there is no cascade from H to the collection, so this should fail when merged
		assertEquals( 1, merged.getHs().size() );

		H h = (H) merged.getHs().iterator().next();

		G gNew = new G();
		gNew.setData( "Gail" );
		gNew.getHs().add( h );
		h.getGs().add( gNew );

		inSession(
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
	public void testMultiPathMergeNonCascadedTransientEntityInOneToOne() {
		// persist a simple A in the database
		A a = new A();
		a.setData( "Anna" );

		inTransaction(
				session ->
						session.save( a )
		);

		// modify detached entity
		modifyEntity( a );

		A merged = fromTransaction(
				session ->
						(A) session.merge( a )
		);

		verifyModifications( merged.getId() );

		// change the one-to-one association from g to be a new (transient) A
		// there is no cascade from G to A, so this should fail when merged
		G g = merged.getG();
		a.setG( null );
		A aNew = new A();
		aNew.setData( "Alice" );
		g.setA( aNew );
		aNew.setG( g );

		inSession(
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
	public void testMultiPathMergeNonCascadedTransientEntityInManyToOne() {
		// persist a simple A in the database

		A a = new A();
		a.setData( "Anna" );

		inTransaction(
				session ->
						session.save( a )
		);

		// modify detached entity
		modifyEntity( a );

		A merged = fromTransaction(
				session ->
						(A) session.merge( a )
		);

		verifyModifications( a.getId() );

		// change the many-to-one association from h to be a new (transient) A
		// there is no cascade from H to A, so this should fail when merged
		assertEquals( 1, merged.getHs().size() );
		H h = (H) merged.getHs().iterator().next();
		merged.getHs().remove( h );
		A aNew = new A();
		aNew.setData( "Alice" );
		aNew.addH( h );

		inSession(
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

	private void verifyModifications(long aId) {
		inTransaction(
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.deletetransient;

import org.hibernate.orm.test.bytecode.enhancement.lazy.NoDirtyCheckingContext;
import org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking.DirtyCheckEnhancementContext;
import org.hibernate.orm.test.deletetransient.Address;
import org.hibernate.orm.test.deletetransient.Note;
import org.hibernate.orm.test.deletetransient.Person;
import org.hibernate.orm.test.deletetransient.Suite;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@RunWith(BytecodeEnhancerRunner.class)
@CustomEnhancementContext({ NoDirtyCheckingContext.class, DirtyCheckEnhancementContext.class })
public class DeleteTransientEntityTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "deletetransient/Person.hbm.xml" };
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testTransientEntityDeletionNoCascades() {
		inTransaction(
				session -> {
					session.remove( new Address() );
				}
		);
	}

	@Test
	public void testTransientEntityDeletionCascadingToTransientAssociation() {
		inTransaction(
				session -> {
					Person p = new Person();
					p.getAddresses().add( new Address() );
					session.persist( p );
				}
		);
	}

	@Test
	public void testTransientEntityDeleteCascadingToCircularity() {
		inTransaction(
				session -> {
					Person p1 = new Person();
					Person p2 = new Person();
					p1.getFriends().add( p2 );
					p2.getFriends().add( p1 );
					session.persist( p1 );
				}
		);
	}

	@Test
	public void testTransientEntityDeletionCascadingToDetachedAssociation() {
		Address address = new Address();
		inTransaction(
				session -> {
					address.setInfo( "123 Main St." );
					session.persist( address );
				}
		);

		inTransaction(
				session -> {
					Person p = new Person();
					p.getAddresses().add( address );
					session.delete( p );
				}
		);

		inTransaction(
				session -> {
					Long count = (Long) session.createQuery( "select count(*) from Address" ).list().get( 0 );
					assertEquals( "delete not cascaded properly across transient entity", 0, count.longValue() );

				}
		);
	}

	@Test
	public void testTransientEntityDeletionCascadingToPersistentAssociation() {
		Long id = fromTransaction(
				session -> {
					Address address = new Address();
					address.setInfo( "123 Main St." );
					session.persist( address );
					return address.getId();
				}
		);

		inTransaction(
				session -> {
					Address address = session.get( Address.class, id );
					Person p = new Person();
					p.getAddresses().add( address );
					session.delete( p );
				}
		);

		inTransaction(
				session -> {
					Long count = (Long) session.createQuery( "select count(*) from Address" ).list().get( 0 );
					assertEquals( "delete not cascaded properly across transient entity", 0, count.longValue() );
				}
		);
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testCascadeAllFromClearedPersistentAssnToTransientEntity() {
		final Person p = new Person();
		Address address = new Address();
		inTransaction(
				session -> {
					address.setInfo( "123 Main St." );
					p.getAddresses().add( address );
					session.save( p );
				}
		);

		inTransaction(
				session -> {
					Suite suite = new Suite();
					address.getSuites().add( suite );
					p.getAddresses().clear();
					session.saveOrUpdate( p );
				}
		);

		inTransaction(
				session -> {
					Person person =  session.get( p.getClass(), p.getId() );
					assertEquals( "persistent collection not cleared", 0, person.getAddresses().size() );
					Long count = (Long) session.createQuery( "select count(*) from Address" ).list().get( 0 );
					assertEquals( 1, count.longValue() );
					count = (Long) session.createQuery( "select count(*) from Suite" ).list().get( 0 );
					assertEquals( 0, count.longValue() );
				}
		);
	}

	@Test
	@SuppressWarnings({ "unchecked" })
	public void testCascadeAllDeleteOrphanFromClearedPersistentAssnToTransientEntity() {
		Address address = new Address();
		address.setInfo( "123 Main St." );
		Suite suite = new Suite();
		address.getSuites().add( suite );
		inTransaction(
				session -> {

					session.save( address );
				}
		);

		inTransaction(
				session -> {
					Note note = new Note();
					note.setDescription( "a description" );
					suite.getNotes().add( note );
					address.getSuites().clear();
					session.saveOrUpdate( address );
				}
		);


		inTransaction(
				session -> {
					Long count = (Long) session.createQuery( "select count(*) from Suite" ).list().get( 0 );
					assertEquals(
							"all-delete-orphan not cascaded properly to cleared persistent collection entities",
							0,
							count.longValue()
					);
					count = (Long) session.createQuery( "select count(*) from Note" ).list().get( 0 );
					assertEquals( 0, count.longValue() );
				}
		);
	}
}

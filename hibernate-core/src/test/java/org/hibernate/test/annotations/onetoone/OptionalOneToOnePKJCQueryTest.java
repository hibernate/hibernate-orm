/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@TestForIssue( jiraKey = "HHH-13875")
public class OptionalOneToOnePKJCQueryTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testOneToOneWithIdNamedId() {
		// Test with associated entity having ID named "id"
		doInHibernate( this::sessionFactory, session -> {
			BarWithIdNamedId bar = new BarWithIdNamedId();
			bar.id = 1L;
			bar.longValue = 2L;
			FooHasBarWithIdNamedId foo = new FooHasBarWithIdNamedId();
			foo.id  = 1L;
			foo.bar = bar;
			session.persist( bar );
			session.persist( foo );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithIdNamedId foo = session.createQuery(
					"from FooHasBarWithIdNamedId where bar.id = ?1",
					FooHasBarWithIdNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNotNull( foo );
			assertNotNull( foo.bar );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithIdNamedId foo = session.get( FooHasBarWithIdNamedId.class, 1L );
			session.delete( foo.bar );
			foo.bar = null;
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithIdNamedId foo = session.createQuery(
					"from FooHasBarWithIdNamedId where bar.id = ?1",
					FooHasBarWithIdNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNull( foo );
		});
	}

	@Test
	public void testOneToOneWithNoIdOrPropNamedId() {
		// Test with associated entity having ID not named "id", and with no property named "id"
		doInHibernate( this::sessionFactory, session -> {
			BarWithNoIdOrPropNamedId bar = new BarWithNoIdOrPropNamedId();
			bar.barId = 1L;
			bar.longValue = 2L;
			FooHasBarWithNoIdOrPropNamedId foo = new FooHasBarWithNoIdOrPropNamedId();
			foo.id  = 1L;
			foo.bar = bar;
			session.persist( bar );
			session.persist( foo );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
					"from FooHasBarWithNoIdOrPropNamedId where bar.barId = ?1",
					FooHasBarWithNoIdOrPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNotNull( foo );
			assertNotNull( foo.bar );
		});

		// Querying by the generic "id" should work the same as "barId".
		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
					"from FooHasBarWithNoIdOrPropNamedId where bar.id = ?1",
					FooHasBarWithNoIdOrPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNotNull( foo );
			assertNotNull( foo.bar );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.get( FooHasBarWithNoIdOrPropNamedId.class, 1L );
			session.delete( foo.bar );
			foo.bar = null;
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
					"from FooHasBarWithNoIdOrPropNamedId where bar.barId = ?1",
					FooHasBarWithNoIdOrPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNull( foo );
		});

		// Querying by the generic "id" should work the same as "barId".
		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
					"from FooHasBarWithNoIdOrPropNamedId where bar.id = ?1",
					FooHasBarWithNoIdOrPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNull( foo );
		});
	}

	@Test
	public void testOneToOneWithNonIdPropNamedId() {
		// Test with associated entity having a non-ID property named "id"
		doInHibernate( this::sessionFactory, session -> {
			BarWithNonIdPropNamedId bar = new BarWithNonIdPropNamedId();
			bar.barId = 1L;
			bar.id = 2L;
			FooHasBarWithNonIdPropNamedId foo = new FooHasBarWithNonIdPropNamedId();
			foo.id  = 1L;
			foo.bar = bar;
			session.persist( bar );
			session.persist( foo );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
					"from FooHasBarWithNonIdPropNamedId where bar.barId = ?1",
					FooHasBarWithNonIdPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNotNull( foo );
			assertNotNull( foo.bar );
		});

		// bar.id is a non-ID property.
		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
					"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
					FooHasBarWithNonIdPropNamedId.class
			).setParameter( 1, 2L )
					.uniqueResult();
			assertNotNull( foo );
			assertNotNull( foo.bar );
		});

		// bar.id is a non-ID property.
		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
					"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
					FooHasBarWithNonIdPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNull( foo );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.get( FooHasBarWithNonIdPropNamedId.class, 1L );
			session.delete( foo.bar );
			foo.bar = null;
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
					"from FooHasBarWithNonIdPropNamedId where bar.barId = ?1",
					FooHasBarWithNonIdPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNull( foo );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
					"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
					FooHasBarWithNonIdPropNamedId.class
			).setParameter( 1, 1L )
					.uniqueResult();
			assertNull( foo );
		});

		doInHibernate( this::sessionFactory, session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
					"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
					FooHasBarWithNonIdPropNamedId.class
			).setParameter( 1, 2L )
					.uniqueResult();
			assertNull( foo );
		});
	}

	@After
	public void cleanupData() {
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete from FooHasBarWithIdNamedId" ).executeUpdate();
			session.createQuery( "delete from FooHasBarWithNoIdOrPropNamedId" ).executeUpdate();
			session.createQuery( "delete from FooHasBarWithNonIdPropNamedId" ).executeUpdate();
			session.createQuery( "delete from BarWithIdNamedId" ).executeUpdate();
			session.createQuery( "delete from BarWithNoIdOrPropNamedId" ).executeUpdate();
			session.createQuery( "delete from BarWithNoIdOrPropNamedId" ).executeUpdate();
		});
	}

	@Override
	protected Class[] getAnnotatedClasses()
	{
		return new Class[] {
				FooHasBarWithIdNamedId.class,
				BarWithIdNamedId.class,
				FooHasBarWithNoIdOrPropNamedId.class,
				BarWithNoIdOrPropNamedId.class,
				FooHasBarWithNonIdPropNamedId.class,
				BarWithNonIdPropNamedId.class
		};
	}

	@Entity(name = "FooHasBarWithIdNamedId")
	public static class FooHasBarWithIdNamedId
	{
		@Id
		private long id;

		@OneToOne(optional = true)
		@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
		private BarWithIdNamedId bar;
	}

	@Entity(name = "BarWithIdNamedId")
	public static class BarWithIdNamedId {
		@Id
		private long id;
		private long longValue;
	}

	@Entity(name = "FooHasBarWithNoIdOrPropNamedId")
	@Table(name = "FooHasBarNoIdOrPropNamedId")
	public static class FooHasBarWithNoIdOrPropNamedId
	{
		@Id
		private long id;

		@OneToOne(optional = true)
		@PrimaryKeyJoinColumn()
		private BarWithNoIdOrPropNamedId bar;
	}

	@Entity(name = "BarWithNoIdOrPropNamedId")
	public static class BarWithNoIdOrPropNamedId {
		@Id
		private long barId;
		private long longValue;
	}

	@Entity(name = "FooHasBarWithNonIdPropNamedId")
	@Table(name = "FooHasBarNonIdPropNamedId")
	public static class FooHasBarWithNonIdPropNamedId
	{
		@Id
		private long id;

		@OneToOne(optional = true)
		@PrimaryKeyJoinColumn()
		private BarWithNonIdPropNamedId bar;
	}

	@Entity(name = "BarWithNonIdPropNamedId")
	public static class BarWithNonIdPropNamedId {
		@Id
		private long barId;
		private long id;
	}
}

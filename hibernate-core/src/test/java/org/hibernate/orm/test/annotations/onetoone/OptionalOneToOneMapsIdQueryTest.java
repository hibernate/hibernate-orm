/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey(value = "HHH-13875")
@DomainModel(
		annotatedClasses = {
				OptionalOneToOneMapsIdQueryTest.FooHasBarWithIdNamedId.class,
				OptionalOneToOneMapsIdQueryTest.BarWithIdNamedId.class,
				OptionalOneToOneMapsIdQueryTest.FooHasBarWithNoIdOrPropNamedId.class,
				OptionalOneToOneMapsIdQueryTest.BarWithNoIdOrPropNamedId.class,
				OptionalOneToOneMapsIdQueryTest.FooHasBarWithNonIdPropNamedId.class,
				OptionalOneToOneMapsIdQueryTest.BarWithNonIdPropNamedId.class
		}
)
@SessionFactory
public class OptionalOneToOneMapsIdQueryTest {

	@Test
	public void testOneToOneWithIdNamedId(SessionFactoryScope scope) {
		// Test with associated entity having ID named "id"
		scope.inTransaction( session -> {
			BarWithIdNamedId bar = new BarWithIdNamedId();
			bar.id = 1L;
			bar.longValue = 2L;
			FooHasBarWithIdNamedId foo = new FooHasBarWithIdNamedId();
			foo.id = 1L;
			foo.bar = bar;
			session.persist( bar );
			session.persist( foo );
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithIdNamedId foo = session.createQuery(
							"from FooHasBarWithIdNamedId where bar.id = ?1",
							FooHasBarWithIdNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNotNull();
			assertThat( foo.bar ).isNotNull();
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithIdNamedId foo = session.get( FooHasBarWithIdNamedId.class, 1L );
			session.remove( foo.bar );
			foo.bar = null;
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithIdNamedId foo = session.createQuery(
							"from FooHasBarWithIdNamedId where bar.id = ?1",
							FooHasBarWithIdNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNull();
		} );
	}

	@Test
	public void testOneToOneWithNoIdOrPropNamedId(SessionFactoryScope scope) {
		// Test with associated entity having ID not named "id", and with no property named "id"
		scope.inTransaction( session -> {
			BarWithNoIdOrPropNamedId bar = new BarWithNoIdOrPropNamedId();
			bar.barId = 1L;
			bar.longValue = 2L;
			FooHasBarWithNoIdOrPropNamedId foo = new FooHasBarWithNoIdOrPropNamedId();
			foo.id = 1L;
			foo.bar = bar;
			session.persist( bar );
			session.persist( foo );
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
							"from FooHasBarWithNoIdOrPropNamedId where bar.barId = ?1",
							FooHasBarWithNoIdOrPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNotNull();
			assertThat( foo.bar ).isNotNull();
		} );

		// Querying by the generic "id" should work the same as "barId".
		scope.inTransaction( session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
							"from FooHasBarWithNoIdOrPropNamedId where bar.id = ?1",
							FooHasBarWithNoIdOrPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNotNull();
			assertThat( foo.bar ).isNotNull();
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.get( FooHasBarWithNoIdOrPropNamedId.class, 1L );
			session.remove( foo.bar );
			foo.bar = null;
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
							"from FooHasBarWithNoIdOrPropNamedId where bar.barId = ?1",
							FooHasBarWithNoIdOrPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNull();
		} );

		// Querying by the generic "id" should work the same as "barId".
		scope.inTransaction( session -> {
			final FooHasBarWithNoIdOrPropNamedId foo = session.createQuery(
							"from FooHasBarWithNoIdOrPropNamedId where bar.id = ?1",
							FooHasBarWithNoIdOrPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNull();
		} );
	}

	@Test
	public void testOneToOneWithNonIdPropNamedId(SessionFactoryScope scope) {
		// Test with associated entity having a non-ID property named "id"
		scope.inTransaction( session -> {
			BarWithNonIdPropNamedId bar = new BarWithNonIdPropNamedId();
			bar.barId = 1L;
			bar.id = 2L;
			FooHasBarWithNonIdPropNamedId foo = new FooHasBarWithNonIdPropNamedId();
			foo.id = 1L;
			foo.bar = bar;
			session.persist( bar );
			session.persist( foo );
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
							"from FooHasBarWithNonIdPropNamedId where bar.barId = ?1",
							FooHasBarWithNonIdPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNotNull();
			assertThat( foo.bar ).isNotNull();
		} );

		// bar.id is a non-ID property.
		scope.inTransaction( session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
							"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
							FooHasBarWithNonIdPropNamedId.class
					).setParameter( 1, 2L )
					.uniqueResult();
			assertThat( foo ).isNotNull();
			assertThat( foo.bar ).isNotNull();
		} );

		// bar.id is a non-ID property.
		scope.inTransaction( session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
							"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
							FooHasBarWithNonIdPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNull();
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.get( FooHasBarWithNonIdPropNamedId.class, 1L );
			session.remove( foo.bar );
			foo.bar = null;
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
							"from FooHasBarWithNonIdPropNamedId where bar.barId = ?1",
							FooHasBarWithNonIdPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNull();
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
							"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
							FooHasBarWithNonIdPropNamedId.class
					).setParameter( 1, 1L )
					.uniqueResult();
			assertThat( foo ).isNull();
		} );

		scope.inTransaction( session -> {
			final FooHasBarWithNonIdPropNamedId foo = session.createQuery(
							"from FooHasBarWithNonIdPropNamedId where bar.id = ?1",
							FooHasBarWithNonIdPropNamedId.class
					).setParameter( 1, 2L )
					.uniqueResult();
			assertThat( foo ).isNull();
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "FooHasBarWithIdNamedId")
	public static class FooHasBarWithIdNamedId {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		@JoinColumn(name = "id")
		@NotFound(action = NotFoundAction.IGNORE)
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
	public static class FooHasBarWithNoIdOrPropNamedId {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		@JoinColumn(name = "id")
		@NotFound(action = NotFoundAction.IGNORE)
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
	public static class FooHasBarWithNonIdPropNamedId {
		@Id
		private Long id;

		@OneToOne
		@MapsId
		@JoinColumn(name = "id")
		@NotFound(action = NotFoundAction.IGNORE)
		private BarWithNonIdPropNamedId bar;
	}

	@Entity(name = "BarWithNonIdPropNamedId")
	public static class BarWithNonIdPropNamedId {
		@Id
		private long barId;
		private long id;
	}
}

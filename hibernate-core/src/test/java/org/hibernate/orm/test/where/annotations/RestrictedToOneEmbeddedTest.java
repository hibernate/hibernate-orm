/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedToOneTest.SqlTarget.class, RestrictedToOneTest.FilterTarget.class,
		RestrictedToOneEmbeddedTest.Owner.class })
@SessionFactory
class RestrictedToOneEmbeddedTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
		scope.getSessionFactory().getCache().evictAllRegions();
	}
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void embeddedReferencesSurviveUpdatesAndMerge(boolean withNote, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var sql = new RestrictedToOneTest.SqlTarget();
			sql.id = 2L;
			final var filter = new RestrictedToOneTest.FilterTarget();
			filter.id = 2L;
			session.persist( sql );
			session.persist( filter );
			final var owner = new Owner();
			owner.id = 1L;
			owner.details = new Details();
			owner.details.note = withNote ? "original" : null;
			owner.details.sql = sql;
			owner.details.filter = filter;
			session.persist( owner );
		} );
		final Owner detached = scope.fromTransaction( session -> {
			RestrictedToOneTest.enable( session );
			final Owner owner = session.find( Owner.class, 1L );
			if ( owner.details != null ) {
				assertThat( owner.details.sql ).isNull();
				assertThat( owner.details.filter ).isNull();
				owner.details.note = "changed";
			}
			owner.name = "updated";
			return owner;
		} );
		assertKeys( scope );
		detached.name = "merged";
		scope.inTransaction( session -> {
			RestrictedToOneTest.enable( session );
			session.merge( detached );
		} );
		assertKeys( scope );
	}
	private static void assertKeys(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Object[] row = session.createNativeQuery(
					"select sql_id, filter_id from restricted_embedded where id=1", Object[].class ).getSingleResult();
			assertThat( row ).containsExactly( 2L, 2L );
		} );
	}
	@Entity(name = "EmbeddedRestrictedOwner")
	@Table(name = "restricted_embedded")
	static class Owner {
		@Id Long id;
		String name;
		@Embedded Details details;
	}
	@Embeddable
	static class Details {
		String note;
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "sql_id")
		RestrictedToOneTest.SqlTarget sql;
		@ManyToOne
		@JoinColumn(name = "filter_id")
		RestrictedToOneTest.FilterTarget filter;
	}
}

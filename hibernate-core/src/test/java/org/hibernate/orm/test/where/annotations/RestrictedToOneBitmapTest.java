/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.where.annotations;

import java.time.LocalDateTime;

import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = { RestrictedToOneBitmapTest.Owner.class,
		RestrictedToOneTest.SqlTarget.class, RestrictedToOneTest.FilterTarget.class })
@SessionFactory(useCollectingStatementObserver = true)
@ServiceRegistry(settings = @Setting(name = "hibernate.check_nullability", value = "true"))
class RestrictedToOneBitmapTest {
	@AfterEach
	void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
		scope.getSessionFactory().getCache().evictAllRegions();
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void nestedReferencesAndSharedJoinRowsNeedNoKeys(boolean emptyComponent, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var sql = new RestrictedToOneTest.SqlTarget();
			sql.id = 2L;
			final var filter = new RestrictedToOneTest.FilterTarget();
			filter.id = 2L;
			session.persist( sql );
			session.persist( filter );
			final var owner = new Owner();
			owner.id = 1L;
			owner.name = "original";
			owner.details = new Details();
			owner.details.note = emptyComponent ? null : "original";
			owner.details.links = new Links();
			owner.details.links.sql = sql;
			owner.details.links.filter = filter;
			owner.computed = new Computed();
			owner.computed.sql = sql;
			owner.joined = sql;
			owner.linkNote = "original";
			session.persist( owner );
		} );
		final var inspector = scope.getCollectingStatementObserver();
		final Owner detached = scope.fromTransaction( session -> {
			RestrictedToOneTest.enable( session );
			final var owner = session.find( Owner.class, 1L );
			final var entry = session.getPersistenceContextInternal().getEntry( owner );
			final var state = entry.getExtraState( FilteredAssociationState.class );
			assertThat( state ).isNotNull();
			assertThat( state.retainsKeys() ).isFalse();
			assertThat( state.physicalState( entry.getLoadedState(), entry.getPersister() ) ).isSameAs( entry.getLoadedState() );
			assertThat( owner.joined ).isNull();
			if ( owner.details == null ) {
				owner.details = new Details();
			}
			owner.details.note = "changed";
			owner.computed = new Computed();
			owner.computed.note = "changed";
			owner.name = "changed";
			owner.linkNote = "changed";
			inspector.clear();
			session.flush();
			assertThat( owner.computed.modified ).isNotNull();
			assertThat( inspector.getSqlQueries() ).allSatisfy( sql -> {
				assertThat( sql.toLowerCase() ).doesNotContain( "merge into", "insert into" );
				assertThat( sql ).doesNotContain( "sql_id=", "filter_id=", "target_id=", "computed_id=" );
			} );
			owner.linkNote = null;
			session.flush();
			assertThat( state.isEmpty() ).isFalse();
			return owner;
		} );
		assertStored( scope );
		detached.name = "merged";
		scope.inTransaction( session -> {
			RestrictedToOneTest.enable( session );
			session.merge( detached );
		} );
		assertStored( scope );
	}

	private static void assertStored(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.createNativeQuery( "select sql_id, filter_id, computed_id, note from bitmap_owner where id=1", Object[].class )
					.getSingleResult() ).containsExactly( 2L, 2L, 2L, "changed" );
			assertThat( session.createNativeQuery( "select target_id, link_note from bitmap_link where owner_id=1", Object[].class )
					.getSingleResult() ).containsExactly( 2L, null );
		} );
	}

	@Entity(name = "BitmapOwner")
	@Table(name = "bitmap_owner")
	static class Owner {
		@Id Long id;
		@Version int version;
		String name;
		@Embedded Details details;
		@Embedded Computed computed;
		@ManyToOne
		@JoinTable(name = "bitmap_link", joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id"))
		RestrictedToOneTest.SqlTarget joined;
		@Column(name = "link_note", table = "bitmap_link") String linkNote;
	}

	@Embeddable
	static class Details {
		String note;
		@Embedded Links links;
	}

	@Embeddable
	static class Computed {
		@Column(name = "computed_note") String note;
		@CurrentTimestamp LocalDateTime modified;
		@ManyToOne @JoinColumn(name = "computed_id") RestrictedToOneTest.SqlTarget sql;
	}

	@Embeddable
	static class Links {
		@ManyToOne(optional = false) @JoinColumn(name = "sql_id", nullable = false)
		RestrictedToOneTest.SqlTarget sql;
		@ManyToOne(optional = false) @JoinColumn(name = "filter_id", nullable = false)
		RestrictedToOneTest.FilterTarget filter;
	}
}

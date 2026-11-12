package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Changelog;
import org.hibernate.audit.ChangesetListener;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Period use validates an application-owned changelog identity without replacing it.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class PeriodChangelogPrecisionTests {
	@ParameterizedTest
	@ValueSource(strings = { "graph", "legacy" })
	void representableIdentityAndRejectionBeforePeriodDml(String queue) {
		final var settings = ChangesetPeriodPrecisionTests.settings( new ChangesetPeriodPrecisionTests.Fixed() );
		settings.remove( "hibernate.temporal.changeset_id_supplier" );
		settings.put( "hibernate.flush.queue.type", queue );
		try ( var scope = ContextScope.open( false, false, settings, List.of(),
				ChangesetPeriodPrecisionTests.First.class, Event.class ) ) {
			Listener.value = ChangesetPeriodPrecisionTests.RAW;
			scope.measure();
			assertThatThrownBy( () -> scope.tx( session -> {
				session.persist( new ChangesetPeriodPrecisionTests.First() );
				session.flush();
			} ) ).hasMessageContaining( "not representable" );
			assertThat( Listener.last.id ).isEqualTo( ChangesetPeriodPrecisionTests.RAW );
			final var history = scope.metadata.getEntityBinding( ChangesetPeriodPrecisionTests.First.class.getName() )
					.getAuxiliaryTable().getName().toLowerCase();
			assertThat( scope.sql ).noneMatch( sql -> sql.toLowerCase().startsWith( "insert into " + history ) );
			for ( var collection : scope.metadata.getCollectionBindings() ) {
				final var collectionHistory = collection.getAuxiliaryTable().getName().toLowerCase();
				assertThat( scope.sql ).noneMatch( sql -> sql.toLowerCase().startsWith( "insert into " + collectionHistory ) );
			}
			Listener.value = ChangesetPeriodPrecisionTests.FIRST;
			scope.tx( session -> {
				session.persist( new ChangesetPeriodPrecisionTests.First() );
				session.flush();
				assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() ).isEqualTo( Listener.value );
			} );
			assertThat( Listener.last.id ).isEqualTo( ChangesetPeriodPrecisionTests.FIRST );
			ChangesetPeriodPrecisionTests.assertStored( scope, ChangesetPeriodPrecisionTests.FIRST );
		}
	}

	public static class Listener implements ChangesetListener {
		static Instant value;
		static Event last;
		@Override public void newChangeset(Object entity) { last = (Event) entity; last.id = value; }
	}

	@Entity(name = "PrecisionChangelog") @Changelog(listener = Listener.class)
	static class Event {
		@Id @Changelog.ChangesetId @Column(secondPrecision = 9) Instant id;
		@Changelog.Timestamp Instant timestamp = Instant.now();
	}
}

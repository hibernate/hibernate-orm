package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hibernate.annotations.Audited;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.temporal.internal.CurrentTimestampChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.RequiresDialect;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Database timestamps are cached per transaction even when separate statements
/// would produce different values. Offline EDB selection is distinct from live
/// lifecycle evidence; H2 exercises the same supplier using an unstable test dialect.
///
/// @author Steve Ebersole
class TemporalServerCoordinationTests {
	@Test
	void constructorsKeepEdbSeparateFromPostgresql() {
		var info = mock( DialectResolutionInfo.class );
		when( info.makeCopyOrDefault( any( DatabaseVersion.class ) ) ).thenReturn( DatabaseVersion.make( 17 ) );
		for ( var dialect : List.of( new PostgresPlusDialect(), new PostgresPlusDialect( DatabaseVersion.make( 17 ) ),
				new PostgresPlusDialect( info ) ) ) {
			assertThat( dialect.getCurrentTemporalSupport().isCurrentTimestampStable() ).isFalse();
			assertThat( dialect.currentTimestamp() ).isEqualTo( "current_timestamp" );
		}
		assertThat( new PostgreSQLDialect().getCurrentTemporalSupport().isCurrentTimestampStable() ).isTrue();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void offlineBootstrapAndCustomSupplier(boolean preservation) {
		var settings = new HashMap<String, Object>( Map.of(
				"hibernate.dialect", PostgresPlusDialect.class,
				"jakarta.persistence.database-product-version", "17.0",
				"hibernate.type.temporal_preservation", preservation,
				"hibernate.temporal.table_strategy", "HISTORY_TABLE",
				"hibernate.temporal.use_server_transaction_timestamps", true ) );
		try ( var scope = ContextScope.boot( false, false, settings, TemporalPeriodFixture.Period.class ) ) {
			assertThat( scope.provider.attempts ).isZero();
			assertThat( scope.factory.getChangesetCoordinator().getIdentifierSupplier() )
					.isInstanceOf( CurrentTimestampChangesetIdentifierSupplier.class );
			assertThat( scope.factory.getChangesetCoordinator().useServerTimestamp( scope.factory.getJdbcServices().getDialect() ) ).isFalse();
		}
		var supplier = new TemporalPeriodFixture.Fixed();
		settings.put( "hibernate.temporal.changeset_id_supplier", supplier );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( false, false, settings, TemporalPeriodFixture.Period.class ) ) {}
		} ).hasMessageContaining( "mutually exclusive" );
		settings.remove( "hibernate.temporal.use_server_transaction_timestamps" );
		try ( var scope = ContextScope.boot( false, false, settings, TemporalPeriodFixture.Period.class ) ) {
			assertThat( scope.factory.getChangesetCoordinator().getIdentifierSupplier() ).isSameAs( supplier );
			assertThat( scope.provider.attempts ).isZero();
		}
	}

	static Stream<Arguments> modes() {
		return Stream.of( true, false ).flatMap( preservation -> Stream.of( true, false )
				.flatMap( metadata -> Stream.of( "graph", "legacy" )
						.map( queue -> Arguments.of( preservation, metadata, queue ) ) ) );
	}

	static Map<String, Object> overrides(String queue) {
		return ContextEnvironment.is( H2Dialect.class )
				? Map.of( "hibernate.flush.queue.type", queue, "hibernate.dialect", UnstableH2Dialect.class )
				: Map.of( "hibernate.flush.queue.type", queue );
	}

	@ParameterizedTest
	@MethodSource("modes")
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgresPlusDialect.class)
	void historyAcrossFlushCommitAndRollback(boolean preservation, boolean metadata, String queue) throws Exception {
		try ( var f = new TemporalPeriodFixture( metadata, "HISTORY_TABLE", true,
				TemporalPeriodFixture.Period.class, preservation, true, overrides( queue ) ) ) {
			assertThat( f.scope.factory.getChangesetCoordinator().getIdentifierSupplier() )
					.isInstanceOf( CurrentTimestampChangesetIdentifierSupplier.class );
			try ( var session = f.scope.factory.withOptions().connection( f.writer ).open() ) {
				var implementor = (SharedSessionContractImplementor) session;
				var transaction = session.beginTransaction();
				session.persist( f.row( 10 ) ); session.flush();
				Instant first = (Instant) implementor.getCurrentChangesetIdentifier();
				session.persist( f.row( 11 ) ); session.flush();
				assertThat( implementor.getCurrentChangesetIdentifier() ).isSameAs( first );
				transaction.commit();
				for ( int id : new int[] {10, 11} ) {
					assertThat( f.intervals( false, id ).get( 0 ).start() ).isEqualTo( first );
					assertThat( f.intervals( true, id ).get( 0 ).start() ).isEqualTo( first );
				}
				f.advancePast( first );
				transaction = session.beginTransaction();
				session.persist( f.row( 12 ) ); session.flush();
				Instant rolledBack = (Instant) implementor.getCurrentChangesetIdentifier();
				assertThat( rolledBack ).isAfter( first );
				transaction.rollback(); session.clear();
				f.advancePast( rolledBack );
				transaction = session.beginTransaction();
				session.persist( f.row( 13 ) ); session.flush();
				Instant last = (Instant) implementor.getCurrentChangesetIdentifier();
				assertThat( last ).isAfter( rolledBack );
				transaction.commit();
				assertThat( f.intervals( false, 12 ) ).isEmpty();
				assertThat( f.intervals( true, 12 ) ).isEmpty();
				assertThat( f.intervals( false, 13 ).get( 0 ).start() ).isEqualTo( last );
			}
			f.lifecycle( 1, TemporalPeriodFixture.SEASONAL );
		}
	}

	@ParameterizedTest
	@MethodSource("modes")
	@RequiresDialect(H2Dialect.class)
	@RequiresDialect(OracleDialect.class)
	@RequiresDialect(PostgresPlusDialect.class)
	void auditUsesTheSharedCoordinator(boolean preservation, boolean metadata, String queue) {
		var settings = new HashMap<String, Object>( overrides( queue ) );
		settings.put( "hibernate.type.temporal_preservation", preservation );
		settings.put( "hibernate.temporal.use_server_transaction_timestamps", true );
		try ( var scope = ContextScope.open( metadata, false, settings, List.of(), AuditRow.class ) ) {
			assertThat( scope.factory.getChangesetCoordinator().getIdentifierSupplier() )
					.isInstanceOf( CurrentTimestampChangesetIdentifierSupplier.class );
			try ( var session = scope.factory.openSession() ) {
				for ( int phase = 0; phase < 3; phase++ ) {
					var transaction = session.beginTransaction();
					var first = new AuditRow(); first.id = phase * 2;
					session.persist( first ); session.flush();
					var identifier = ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier();
					var second = new AuditRow(); second.id = phase * 2 + 1;
					session.persist( second ); session.flush();
					assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() ).isSameAs( identifier );
					if ( phase == 1 ) transaction.rollback(); else transaction.commit();
					session.clear();
					if ( phase != 1 ) {
						try ( var history = scope.factory.withOptions().atChangeset( identifier ).open() ) {
							assertThat( history.find( AuditRow.class, first.id ).tags ).containsExactly( "tag" );
							assertThat( history.find( AuditRow.class, second.id ).tags ).containsExactly( "tag" );
						}
					}
				}
				session.beginTransaction();
				assertThat( session.find( AuditRow.class, 2 ) ).isNull();
				assertThat( session.find( AuditRow.class, 3 ) ).isNull();
				assertThat( session.createNativeQuery( "select count(*) from CoordinatedAuditRow_AUD where id in (2,3)", Long.class ).getSingleResult() ).isZero();
				session.getTransaction().commit();
			}
		}
	}

	public static class UnstableH2Dialect extends H2Dialect {
		@Override public boolean isCurrentTimestampStable() { return false; }
	}

	@Entity(name = "CoordinatedAuditRow")
	@Audited
	static class AuditRow {
		@Id int id;
		@ElementCollection List<String> tags = new ArrayList<>( List.of( "tag" ) );
	}
}

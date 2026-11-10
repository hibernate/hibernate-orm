package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCollectionHistoryRepairTests.Fixture;
import org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCollectionHistoryRepairTests.Owner;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCollectionHistoryRepairTests.A;
import static org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCollectionHistoryRepairTests.B;
import static org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCollectionHistoryRepairTests.C;
import static org.hibernate.orm.test.mapping.javatime.redesign.context.TemporalCollectionHistoryRepairTests.D;

/// Reused legacy executors must retain every current/history mutation when batch keys alternate.
/// Raw interval and current-state checks expose lost tails before a later row-count failure.
///
/// @author Steve Ebersole
@JiraKey("HHH-21102")
class TemporalLegacyHistoryBatchTests {
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	@RequiresDialect(H2Dialect.class)
	void listOrderingAndClear(boolean preserving) throws Exception {
		TemporalCollectionHistoryRepairTests.assertListOrderingAndClear( "legacy", preserving );
	}

	@ParameterizedTest
	@CsvSource({"true,0,2", "true,1,2", "true,5,2", "false,0,2", "false,1,2", "false,5,2",
			"true,0,3", "true,1,3", "true,5,3", "false,0,3", "false,1,3", "false,5,3"})
	@RequiresDialect(H2Dialect.class)
	void exactHistoryAcrossBatchSizes(boolean preserving, int batch, int count) throws Exception {
		lifecycle( preserving, batch, count );
	}

	@Test
	@RequiresDialect(PostgreSQLDialect.class)
	@RequiresDialect(OracleDialect.class)
	void providerLegacyHistoryControl() throws Exception {
		lifecycle( true, 5, 3 );
	}

	static void lifecycle(boolean preserving, int batch, int count) throws Exception {
		try ( var f = new Fixture( "legacy", preserving, batch ) ) {
			var initial = seed( f, count );
			var swapped = new ArrayList<>( initial ); Collections.swap( swapped, 0, count - 1 );
			f.change( B, row -> Collections.swap( row.items, 0, count - 1 ) );
			current( f, swapped );
			var expected = new ArrayList<HistoryRow>();
			for ( int i = 0; i < count; i++ ) {
				boolean moved = i == 0 || i == count - 1;
				expected.add( new HistoryRow( A, moved ? B : null, initial.get( i ), i ) );
				if ( moved ) expected.add( new HistoryRow( B, null, swapped.get( i ), i ) );
			}
			assertThat( raw( f ) ).containsExactlyInAnyOrderElementsOf( expected );
			f.at( B.minusNanos( 1000 ), row -> assertThat( row.items ).containsExactlyElementsOf( initial ) );
			f.at( B, row -> assertThat( row.items ).containsExactlyElementsOf( swapped ) );

			var removed = new ArrayList<>( swapped ); removed.remove( 0 );
			f.change( C, row -> row.items.remove( 0 ) ); current( f, removed );
			// All remaining entries shift after removing position zero, so every open interval ends.
			closeOpen( expected, C );
			for ( int i = 0; i < removed.size(); i++ ) expected.add( new HistoryRow( C, null, removed.get( i ), i ) );
			assertThat( raw( f ) ).containsExactlyInAnyOrderElementsOf( expected );
			f.at( C.minusNanos( 1000 ), row -> assertThat( row.items ).containsExactlyElementsOf( swapped ) );
			f.at( C, row -> assertThat( row.items ).containsExactlyElementsOf( removed ) );
			f.change( D, row -> row.items.clear() ); current( f, List.of() ); closeOpen( expected, D );
			assertThat( raw( f ) ).containsExactlyInAnyOrderElementsOf( expected );
			f.at( D.minusNanos( 1000 ), row -> assertThat( row.items ).containsExactlyElementsOf( removed ) );
			f.at( D, row -> assertThat( row.items ).isEmpty() );
		}
	}

	@ParameterizedTest
	@CsvSource({"true,0", "true,1", "true,5", "false,0", "false,1", "false,5"})
	@RequiresDialect(H2Dialect.class)
	void failedHistoryInsertRollsBackAndDoesNotComplete(boolean preserving, int batch) throws Exception {
		try ( var f = new Fixture( "legacy", preserving, batch ) ) {
			var initial = seed( f, 3 );
			try ( var connection = f.scope.provider.getConnection(); var statement = connection.createStatement() ) {
				statement.execute( "alter table " + historyTable( f ) + " add constraint lb_reject_history check (entry_value <> 'fail')" );
				connection.commit();
			}
			var completed = new AtomicInteger();
			f.scope.factory.getEventEngine().getListenerRegistry().appendListeners( EventType.POST_COLLECTION_UPDATE,
					(PostCollectionUpdateEventListener) event -> completed.incrementAndGet() );
			f.scope.measure();
			assertThatThrownBy( () -> f.change( B, row -> {
				row.items.set( 0, "fail" ); row.items.set( 1, "changed" );
			} ) ).isInstanceOf( org.hibernate.exception.ConstraintViolationException.class );
			assertThat( completed ).hasValue( 0 );
			assertThat( f.scope.factory.getStatistics().getCollectionUpdateCount() ).isZero();
			current( f, initial );
			assertThat( raw( f ) ).containsExactlyInAnyOrder(
					new HistoryRow( A, null, "one", 0 ), new HistoryRow( A, null, "two", 1 ), new HistoryRow( A, null, "three", 2 ) );
			f.at( B, row -> assertThat( row.items ).containsExactlyElementsOf( initial ) );
		}
	}

	static List<String> seed(Fixture f, int count) {
		var values = List.of( "one", "two", "three" ).subList( 0, count );
		f.scope.tx( session -> {
			var row = new Owner(); row.id = 1; row.items.addAll( values ); session.persist( row );
		} );
		return values;
	}

	static void current(Fixture f, List<String> expected) {
		f.scope.tx( session -> assertThat( session.find( Owner.class, 1 ).items ).containsExactlyElementsOf( expected ) );
	}

	static void closeOpen(List<HistoryRow> rows, Instant end) {
		rows.replaceAll( row -> row.end == null ? new HistoryRow( row.start, end, row.value, row.position ) : row );
	}

	static String historyTable(Fixture f) {
		return f.scope.metadata.getCollectionBinding( Owner.class.getName() + ".items" ).getAuxiliaryTable().getName();
	}

	static List<HistoryRow> raw(Fixture f) throws Exception {
		boolean zoned = f.scope.factory.getJdbcServices().getDialect() instanceof PostgreSQLDialect;
		var result = new ArrayList<HistoryRow>();
		try ( var connection = f.scope.provider.getConnection(); var statement = connection.createStatement();
				var rows = statement.executeQuery( "select valid_from,valid_to,entry_value,item_position from " + historyTable( f ) + " where owner_id=1" ) ) {
			while ( rows.next() ) {
				Instant start, end;
				if ( zoned ) {
					start = rows.getObject( 1, OffsetDateTime.class ).toInstant();
					var value = rows.getObject( 2, OffsetDateTime.class ); end = value == null ? null : value.toInstant();
				}
				else {
					start = rows.getObject( 1, LocalDateTime.class ).toInstant( ZoneOffset.UTC );
					var value = rows.getObject( 2, LocalDateTime.class ); end = value == null ? null : value.toInstant( ZoneOffset.UTC );
				}
				result.add( new HistoryRow( start, end, rows.getString( 3 ), rows.getInt( 4 ) ) );
			} connection.commit();
		}
		return result;
	}

	record HistoryRow(Instant start, Instant end, String value, int position) {}
}

package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/// History mutations must follow the logical collection change, including graph-queue
/// operations that do not insert a current row. Legacy execution remains a differential control.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalCollectionHistoryRepairTests {
	static final Instant A = Instant.parse( "2024-01-15T12:00:00.123456Z" );
	static final Instant B = A.plusSeconds( 10 );
	static final Instant C = B.plusSeconds( 10 );
	static final Instant D = C.plusSeconds( 10 );

	@ParameterizedTest
	@CsvSource({"graph,true", "graph,false", "legacy,true", "legacy,false"})
	void valuesAndRowRemoval(String queue, boolean preserving) throws Exception {
		try ( var f = new Fixture( queue, preserving, 5 ) ) {
			f.insert();
			f.change( B, row -> {
				row.labels.put( "key", "changed" );
				row.names.remove( "one" );
				row.items.set( 0, "changed" );
			} );
			f.at( A, row -> {
				assertThat( row.labels ).containsEntry( "key", "one" );
				assertThat( row.names ).containsExactlyInAnyOrder( "one", "two" );
				assertThat( row.items ).containsExactly( "one", "two" );
			} );
			f.at( B, row -> {
				assertThat( row.labels ).containsEntry( "key", "changed" );
				assertThat( row.names ).containsExactly( "two" );
				assertThat( row.items ).containsExactly( "changed", "two" );
			} );
			assertThat( f.raw( "names", 1 ) ).containsExactlyInAnyOrder(
					new Interval( A, B, "one" ), new Interval( A, null, "two" ) );
			assertThat( f.raw( "labels", 1 ) ).containsExactlyInAnyOrder(
					new Interval( A, B, "one" ), new Interval( B, null, "changed" ) );
			assertThat( f.raw( "names", 2 ) ).containsExactlyInAnyOrder(
					new Interval( A, null, "one" ), new Interval( A, null, "two" ) );
		}
	}

	@ParameterizedTest
	@CsvSource({"graph,true", "graph,false"})
	void listOrderingAndClear(String queue, boolean preserving) throws Exception {
		assertListOrderingAndClear( queue, preserving );
	}

	static void assertListOrderingAndClear(String queue, boolean preserving) throws Exception {
		try ( var f = new Fixture( queue, preserving, 5 ) ) {
			f.insert();
			f.change( B, row -> Collections.swap( row.items, 0, 1 ) );
			f.change( C, row -> row.items.remove( 0 ) );
			f.change( D, row -> row.items.clear() );
			f.at( B.minusNanos( 1000 ), row -> assertThat( row.items ).containsExactly( "one", "two" ) );
			f.at( B, row -> assertThat( row.items ).containsExactly( "two", "one" ) );
			f.at( C, row -> assertThat( row.items ).containsExactly( "one" ) );
			f.at( D, row -> assertThat( row.items ).isEmpty() );
			assertThat( f.raw( "items", 1 ) ).allSatisfy( interval -> {
				assertThat( interval.start ).isIn( A, B, C );
				assertThat( interval.end ).isIn( B, C, D );
			} );
		}
	}

	@ParameterizedTest
	@CsvSource({"graph,true", "graph,false", "legacy,true", "legacy,false"})
	void bagRecreationAndClear(String queue, boolean preserving) throws Exception {
		try ( var f = new Fixture( queue, preserving, 0 ) ) {
			f.insert();
			f.change( B, row -> row.bag.remove( "one" ) );
			f.change( C, row -> row.bag.clear() );
			f.at( A, row -> assertThat( row.bag ).containsExactlyInAnyOrder( "one", "two" ) );
			f.at( B, row -> assertThat( row.bag ).containsExactly( "two" ) );
			f.at( C, row -> assertThat( row.bag ).isEmpty() );
			assertThat( f.raw( "bag", 1 ) ).allSatisfy( interval -> assertThat( interval.end ).isIn( B, C ) );
		}
	}

	@ParameterizedTest
	@CsvSource({"graph,true", "graph,false", "legacy,true", "legacy,false"})
	void removalAndRollback(String queue, boolean preserving) throws Exception {
		try ( var f = new Fixture( queue, preserving, 5 ) ) {
			f.insert();
			f.supplier.value = B;
			try ( var session = f.scope.factory.openSession() ) {
				var tx = session.beginTransaction();
				session.find( Owner.class, 1 ).labels.put( "key", "rolled back" );
				session.flush();
				tx.rollback();
			}
			assertThat( f.raw( "labels", 1 ) ).containsExactly( new Interval( A, null, "one" ) );
			for ( int id : new int[] {1, 2, 3} ) {
				f.supplier.value = id == 1 ? C : id == 2 ? D : D.plusSeconds( 10 );
				if ( id == 3 ) f.scope.measure();
				f.scope.tx( session -> {
					var owner = session.find( Owner.class, id );
					if ( id == 1 || id == 3 ) {
						owner.items.size(); owner.names.size(); owner.labels.size(); owner.bag.size();
					}
					session.remove( owner );
				} );
				for ( var role : List.of( "items", "names", "labels", "bag" ) ) {
					if ( id == 3 ) assertThat( f.raw( role, id ) ).isEmpty();
					else assertThat( f.raw( role, id ) ).allSatisfy( interval -> assertThat( interval.end ).isEqualTo( id == 1 ? C : D ) );
				}
				if ( id == 3 ) assertThat( f.scope.sql ).noneMatch( sql -> sql.toLowerCase().matches( "(?s).*update chr_(items|names|labels|bag)_history.*" ) );
			}
		}
	}

	@ParameterizedTest
	@CsvSource({"true,0", "true,5", "false,0", "false,5"})
	void completionWaitsForHistoryAndFailureDoesNotComplete(boolean preserving, int batch) throws Exception {
		try ( var f = new Fixture( "graph", preserving, batch ) ) {
			f.insert();
			var completed = new java.util.concurrent.atomic.AtomicInteger();
			String history = f.scope.metadata.getCollectionBinding( Owner.class.getName() + ".items" ).getAuxiliaryTable().getName();
			f.scope.factory.getEventEngine().getListenerRegistry().appendListeners(
					org.hibernate.event.spi.EventType.POST_COLLECTION_UPDATE,
					(org.hibernate.event.spi.PostCollectionUpdateEventListener) event -> {
						if ( event.getCollection().getRole().endsWith( ".items" ) ) {
							event.getSession().doWork( connection -> {
								try ( var st = connection.createStatement(); var rs = st.executeQuery(
										"select count(*) from " + history + " where owner_id=1 and entry_value='changed' and valid_to is null" ) ) {
									rs.next(); assertThat( rs.getInt( 1 ) ).isEqualTo( 1 );
								}
							} );
							completed.incrementAndGet();
						}
					} );
			f.scope.measure();
			f.change( B, row -> row.items.set( 0, "changed" ) );
			assertThat( completed ).hasValue( 1 );
			assertThat( f.scope.factory.getStatistics().getCollectionUpdateCount() ).isEqualTo( 1 );
			try ( var connection = f.scope.provider.getConnection(); var st = connection.createStatement() ) {
				st.execute( "alter table " + history + " add constraint chr_reject_history check (entry_value <> 'fail')" );
				connection.commit();
			}
			org.assertj.core.api.Assertions.assertThatThrownBy( () -> f.change( C, row -> row.items.set( 0, "fail" ) ) )
					.isInstanceOf( RuntimeException.class );
			assertThat( completed ).hasValue( 1 );
			assertThat( f.scope.factory.getStatistics().getCollectionUpdateCount() ).isEqualTo( 1 );
			f.scope.tx( session -> assertThat( session.find( Owner.class, 1 ).items ).containsExactly( "changed", "two" ) );
			assertThat( f.raw( "items", 1 ) ).containsExactlyInAnyOrder(
					new Interval( A, B, "one" ), new Interval( A, null, "two" ), new Interval( B, null, "changed" ) );
		}
	}

	static class Fixed implements org.hibernate.temporal.spi.ChangesetIdentifierSupplier<Instant> {
		Instant value = A;

		@Override
		public Instant generateIdentifier(org.hibernate.SharedSessionContract session) {
			return value;
		}
	}

	record Interval(Instant start, Instant end, String value) {}

	static class Fixture implements AutoCloseable {
		final Fixed supplier = new Fixed();
		final ContextScope scope;
		Fixture(String queue, boolean preserving, int batch) {
			supplier.value = A;
			var settings = new HashMap<String, Object>();
			settings.put( "hibernate.type.temporal_preservation", preserving );
			settings.put( "hibernate.temporal.table_strategy", "HISTORY_TABLE" );
			settings.put( "hibernate.test.temporal.reuse_connection", true );
			settings.put( "hibernate.cache.use_second_level_cache", false );
			settings.put( "hibernate.temporal.changeset_id_supplier", supplier );
			settings.put( "hibernate.flush.queue.type", queue );
			settings.put( "hibernate.jdbc.batch_size", batch );
			scope = ContextScope.open( false, false, settings, List.of(), Owner.class );
		}
		void insert() {
			scope.tx( session -> {
				for ( int id : new int[] {1, 2} ) {
					var row = new Owner(); row.id = id;
					row.items.addAll( List.of( "one", "two" ) );
					row.names.addAll( row.items ); row.bag.addAll( row.items );
					row.labels.put( "key", "one" ); session.persist( row );
				}
				var empty = new Owner(); empty.id = 3; session.persist( empty );
			} );
		}
		void change(Instant instant, Consumer<Owner> change) {
			supplier.value = instant;
			scope.tx( session -> change.accept( session.find( Owner.class, 1 ) ) );
		}
		void at(Instant point, Consumer<Owner> check) {
			try ( Session session = scope.factory.withOptions().asOf( point ).open() ) {
				var tx = session.beginTransaction();
				check.accept( session.find( Owner.class, 1 ) ); tx.commit();
			}
		}
		List<Interval> raw(String role, int id) throws Exception {
			String table = scope.metadata.getCollectionBinding( Owner.class.getName() + "." + role ).getAuxiliaryTable().getName();
			var result = new ArrayList<Interval>();
			try ( var c = scope.provider.getConnection(); var st = c.prepareStatement(
					"select valid_from,valid_to,entry_value from " + table + " where owner_id=?" ) ) {
				st.setInt( 1, id );
				try ( var rs = st.executeQuery() ) {
					while ( rs.next() ) {
						var start = rs.getObject( 1, LocalDateTime.class );
						var end = rs.getObject( 2, LocalDateTime.class );
						result.add( new Interval( start.toInstant( ZoneOffset.UTC ), end == null ? null : end.toInstant( ZoneOffset.UTC ), rs.getString( 3 ) ) );
					}
				} c.commit();
			} return result;
		}
		@Override public void close() { scope.close(); }
	}

	@Entity(name = "HistoryRepairOwner") @Table(name = "chr_owner")
	@Temporal(rowStart = "valid_from", rowEnd = "valid_to", secondPrecision = 6)
	static class Owner {
		@Id int id;
		@ElementCollection @CollectionTable(name = "chr_items", joinColumns = @JoinColumn(name = "owner_id"))
		@OrderColumn(name = "item_position") @Column(name = "entry_value", nullable = false)
		@Temporal(rowStart = "valid_from", rowEnd = "valid_to", secondPrecision = 6)
		List<String> items = new ArrayList<>();
		@ElementCollection @CollectionTable(name = "chr_names", joinColumns = @JoinColumn(name = "owner_id"))
		@Column(name = "entry_value", nullable = false)
		@Temporal(rowStart = "valid_from", rowEnd = "valid_to", secondPrecision = 6)
		Set<String> names = new HashSet<>();
		@ElementCollection @CollectionTable(name = "chr_labels", joinColumns = @JoinColumn(name = "owner_id"))
		@MapKeyColumn(name = "entry_key") @Column(name = "entry_value", nullable = false)
		@Temporal(rowStart = "valid_from", rowEnd = "valid_to", secondPrecision = 6)
		Map<String, String> labels = new HashMap<>();
		@ElementCollection @CollectionTable(name = "chr_bag", joinColumns = @JoinColumn(name = "owner_id"))
		@Column(name = "entry_value", nullable = false)
		@Temporal(rowStart = "valid_from", rowEnd = "valid_to", secondPrecision = 6)
		List<String> bag = new ArrayList<>();
	}
}

package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import org.hibernate.SharedSessionContract;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.annotations.Temporal;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;
import org.hibernate.dialect.temporaltype.spi.DelegatingCurrentTemporalSupport;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.temporal.spi.ChangesetIdentifierSupplier;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// The published event timestamp and every participating endpoint share a physical
/// grid, including temporal collections whose owner has no entity history.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class ChangesetPeriodPrecisionTests {
	static final Instant RAW = Instant.parse( "2026-01-01T12:00:00.123456789Z" );
	static final Instant FIRST = Instant.parse( "2026-01-01T12:00:00.123457Z" );
	static final Instant SECOND = FIRST.plusSeconds( 1 );

	static Map<String, Object> settings(Fixed supplier) {
		return new HashMap<>( Map.of(
				"hibernate.type.temporal_preservation", true,
				"hibernate.temporal.table_strategy", "HISTORY_TABLE",
				"hibernate.temporal.changeset_id_supplier", supplier ) );
	}

	@ParameterizedTest
	@CsvSource({ "graph,0,true", "graph,5,false", "legacy,0,false", "legacy,5,true" })
	void sharedEventAndFineGrainedHistory(String queue, int batch, boolean metadata) {
		final var supplier = new Fixed();
		final var settings = settings( supplier );
		settings.put( "hibernate.flush.queue.type", queue );
		settings.put( "hibernate.jdbc.batch_size", batch );
		try ( var scope = ContextScope.open( metadata, false, settings, List.of(), First.class, Second.class, Owner.class ) ) {
			for ( var binding : scope.metadata.getEntityBindings() ) {
				if ( binding.getAuxiliaryColumn( "rowStart" ) != null ) {
					assertThat( binding.getAuxiliaryColumn( "rowStart" ).getTemporalPrecision() ).isEqualTo( 6 );
					assertThat( binding.getAuxiliaryColumn( "rowEnd" ).getSqlType( scope.metadata ) ).isEqualTo( "timestamp(6)" );
				}
			}
			for ( var binding : scope.metadata.getCollectionBindings() ) {
				assertThat( binding.getAuxiliaryColumn( "rowStart" ).getSqlType( scope.metadata ) ).isEqualTo( "timestamp(6)" );
			}
			scope.tx( session -> {
				session.persist( new First() );
				session.flush();
				assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() ).isEqualTo( FIRST );
				supplier.value = RAW.plusSeconds( 1 );
				session.persist( new Second() );
				session.persist( new Owner() );
				session.flush();
				assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() ).isEqualTo( FIRST );
			} );
			assertThat( supplier.calls ).isOne();
			assertStored( scope, FIRST );
			scope.tx( session -> {
				final var first = session.find( First.class, 1 );
				first.payload = "second";
				first.tags.add( "second" );
				session.find( Second.class, 1 ).tags.add( "second" );
				session.find( Owner.class, 1 ).tags.add( "second" );
			} );
			assertThat( supplier.calls ).isEqualTo( 2 );
			for ( Instant point : List.of( SECOND.minusNanos( 1 ), SECOND, SECOND.plusNanos( 1 ) ) ) {
				try ( var session = scope.factory.withOptions().asOf( point ).open() ) {
					session.inTransaction( transaction -> {
						final boolean old = point.isBefore( SECOND );
						final var first = session.find( First.class, 1 );
						assertThat( first.payload ).isEqualTo( old ? "first" : "second" );
						assertThat( first.tags ).hasSize( old ? 1 : 2 );
						assertThat( session.find( Second.class, 1 ).tags ).hasSize( old ? 1 : 2 );
						assertThat( session.find( Owner.class, 1 ).tags ).hasSize( old ? 1 : 2 );
					} );
				}
			}
		}
	}

	static void assertStored(ContextScope scope, Instant expected) {
		final var tables = new ArrayList<String>();
		for ( var entity : scope.metadata.getEntityBindings() ) {
			if ( entity.getAuxiliaryColumn( "rowStart" ) != null ) {
				tables.add( entity.getAuxiliaryTable().getName() );
			}
		}
		for ( var collection : scope.metadata.getCollectionBindings() ) {
			tables.add( collection.getAuxiliaryTable().getName() );
		}
		scope.tx( session -> session.doWork( connection -> {
			for ( var table : tables ) {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery( "select valid_from from " + table ) ) {
					assertThat( rows.next() ).isTrue();
					do {
						assertThat( rows.getObject( 1, LocalDateTime.class ).toInstant( ZoneOffset.UTC ) ).isEqualTo( expected );
					} while ( rows.next() );
				}
			}
		} ) );
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void declarationAndLegacyControls(boolean metadata) {
		for ( var entity : List.of( Millis.class, Mixed.class, Zero.class, CollectionConflict.class ) ) {
			assertThatThrownBy( () -> {
				try ( var ignored = ContextScope.boot( metadata, false, settings( new Fixed() ), entity ) ) {}
			} ).hasMessageContaining( "period precision conflict" ).hasMessageContaining( "Dialect-selected 6" );
			final var legacy = settings( new Fixed() );
			legacy.put( "hibernate.type.temporal_preservation", false );
			try ( var scope = ContextScope.boot( metadata, false, legacy, entity ) ) {
				assertThat( scope.factory.getTemporalPeriodPrecision() ).isNull();
				assertThat( scope.metadata.getEntityBinding( entity.getName() ).getAuxiliaryColumn( "rowStart" ).getTemporalPrecision() )
						.isEqualTo( entity == Zero.class ? 0 : entity == CollectionConflict.class ? 6 : 3 );
			}
		}
		try ( var scope = ContextScope.boot( metadata, false, settings( new Fixed() ), Matching.class ) ) {
			assertThat( scope.factory.getTemporalPeriodPrecision() ).isNotNull();
		}
	}

	@Test
	void rollbackResetsAndStatelessUsesTheSamePreparation() {
		final var supplier = new Fixed();
		try ( var scope = ContextScope.open( false, false, settings( supplier ), List.of(), First.class ) ) {
			try ( var session = scope.factory.openSession() ) {
				var transaction = session.beginTransaction();
				session.persist( new First() );
				session.flush();
				transaction.rollback();
				supplier.value = RAW.plusSeconds( 1 );
				transaction = session.beginTransaction();
				session.persist( new First() );
				assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() ).isEqualTo( SECOND );
				transaction.commit();
			}
			assertThat( supplier.calls ).isEqualTo( 2 );
			try ( var session = scope.factory.openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() ).isEqualTo( SECOND );
				assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() ).isEqualTo( SECOND );
				transaction.commit();
			}
			assertThat( supplier.calls ).isEqualTo( 3 );
			assertStored( scope, SECOND );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void inlineAndFetchedDatabaseEvents(boolean fetched) {
		final var options = settings( new Fixed() );
		options.remove( "hibernate.temporal.changeset_id_supplier" );
		options.put( "hibernate.temporal.use_server_transaction_timestamps", true );
		if ( fetched ) {
			options.put( "hibernate.dialect", FetchedH2Dialect.class );
		}
		try ( var scope = ContextScope.open( false, false, options, List.of(), First.class, Second.class, Owner.class ) ) {
			scope.tx( session -> {
				session.persist( new First() );
				session.flush();
				session.persist( new Second() );
				session.persist( new Owner() );
				session.flush();
			} );
			final var table = scope.metadata.getEntityBinding( First.class.getName() ).getAuxiliaryTable().getName();
			final var instant = scope.factory.fromTransaction( session -> session.doReturningWork( connection -> {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery( "select valid_from from " + table ) ) {
					assertThat( rows.next() ).isTrue();
					return rows.getObject( 1, LocalDateTime.class ).toInstant( ZoneOffset.UTC );
				}
			} ) );
			assertThat( scope.factory.getTemporalPeriodPrecision().prepare( instant, false ) ).isEqualTo( instant );
			assertStored( scope, instant );
			assertThat( scope.factory.getChangesetCoordinator().useServerTimestamp( scope.factory.getJdbcServices().getDialect() ) )
					.isEqualTo( !fetched );
		}
	}

	/// Exercises the existing fetched producer path without claiming another provider's behavior.
	public static class FetchedH2Dialect extends H2Dialect {
		@Override
		public CurrentTemporalSupport getCurrentTemporalSupport() {
			return new DelegatingCurrentTemporalSupport( super.getCurrentTemporalSupport() ) {
				@Override public boolean isCurrentTimestampStable() { return false; }
			};
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void sameBucketKeepsExistingDatabaseIntervalConstraint(boolean preserving) {
		final var supplier = new Fixed();
		final var options = settings( supplier );
		options.put( "hibernate.type.temporal_preservation", preserving );
		try ( var scope = ContextScope.open( false, false, options, List.of(), First.class ) ) {
			scope.tx( session -> session.persist( new First() ) );
			supplier.value = RAW.plusNanos( 1 );
			assertThatThrownBy( () -> scope.tx( session -> session.find( First.class, 1 ).payload = "collision" ) )
					.isInstanceOf( ConstraintViolationException.class );
			scope.tx( session -> assertThat( session.find( First.class, 1 ).payload ).isEqualTo( "first" ) );
		}
	}

	@Test
	void collectionOnlyFactoryResolvesItsOwnContract() {
		final var supplier = new Fixed();
		try ( var scope = ContextScope.open( false, false, settings( supplier ), List.of(), Owner.class ) ) {
			assertThat( scope.factory.getTemporalPeriodPrecision() ).isNotNull();
			scope.tx( session -> session.persist( new Owner() ) );
			assertStored( scope, FIRST );
			assertThat( supplier.calls ).isOne();
		}
	}

	static class Fixed implements ChangesetIdentifierSupplier<Instant> {
		Instant value = RAW;
		int calls;
		@Override public Instant generateIdentifier(SharedSessionContract session) { calls++; return value; }
	}

	@Entity(name = "PrecisionFirst") @Temporal(rowStart = "valid_from", rowEnd = "valid_to")
	static class First {
		@Id int id = 1;
		String payload = "first";
		@ElementCollection @OrderColumn @Temporal(rowStart = "valid_from", rowEnd = "valid_to")
		List<String> tags = new ArrayList<>( List.of( "first" ) );
	}
	@Entity(name = "PrecisionSecond") @Temporal(rowStart = "valid_from", rowEnd = "valid_to")
	static class Second {
		@Id int id = 1;
		@ElementCollection @OrderColumn @Temporal(rowStart = "valid_from", rowEnd = "valid_to")
		List<String> tags = new ArrayList<>( List.of( "first" ) );
	}
	@Entity(name = "PrecisionOwner")
	static class Owner {
		@Id int id = 1;
		@ElementCollection @OrderColumn @Temporal(rowStart = "valid_from", rowEnd = "valid_to")
		List<String> tags = new ArrayList<>( List.of( "first" ) );
	}
	@Entity @Temporal(secondPrecision = 3) static class Millis { @Id int id; }
	@Entity @Temporal(secondPrecision = 0) static class Zero { @Id int id; }
	@Entity @Temporal(secondPrecision = 6) static class Matching { @Id int id; }
	@Entity @Temporal static class CollectionConflict {
		@Id int id;
		@ElementCollection @Temporal(secondPrecision = 3) List<String> tags;
	}
	@Entity @Temporal(secondPrecision = 3) static class Mixed {
		@Id int id;
		@ElementCollection @Temporal(secondPrecision = 6) List<String> tags;
	}
}

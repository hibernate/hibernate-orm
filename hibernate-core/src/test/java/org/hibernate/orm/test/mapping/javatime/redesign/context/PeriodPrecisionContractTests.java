package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.TemporalValueException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.temporal.internal.H2TemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.BasicValue;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Independent JDBC fixed points and factory-local ownership of the resolved contract.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class PeriodPrecisionContractTests {
	@Test
	void jdbcRoundingAndPreparedValuesAgree() {
		try ( var scope = ContextScope.open( false, false,
				ChangesetPeriodPrecisionTests.settings( new ChangesetPeriodPrecisionTests.Fixed() ),
				List.of(), ChangesetPeriodPrecisionTests.Matching.class ) ) {
			final var contract = scope.factory.getTemporalPeriodPrecision();
			final var column = scope.metadata.getEntityBinding( ChangesetPeriodPrecisionTests.Matching.class.getName() )
					.getAuxiliaryColumn( "rowStart" );
			final var jdbc = ((BasicValue) column.getValue()).resolve().getJdbcType();
			final var binder = jdbc.getBinder( InstantJavaType.INSTANCE );
			final var extractor = jdbc.getExtractor( InstantJavaType.INSTANCE );
			scope.tx( session -> session.doWork( connection -> {
				for ( String input : List.of(
						"2026-01-01T12:00:00.123456499Z", "2026-01-01T12:00:00.123456500Z",
						"2026-01-01T12:00:00.123456501Z", "1969-12-31T23:59:59.999999500Z",
						"2026-12-31T23:59:59.999999500Z" ) ) {
					final var raw = Instant.parse( input );
					final var canonical = (Instant) contract.prepare( raw, false );
					assertThat( contract.prepare( canonical, false ) ).isEqualTo( canonical );
					try ( var statement = connection.prepareStatement( "select cast(? as timestamp(6)), cast(? as timestamp(6))" ) ) {
						final var options = (SharedSessionContractImplementor) session;
						binder.bind( statement, raw, 1, options );
						binder.bind( statement, canonical, 2, options );
						try ( var rows = statement.executeQuery() ) {
							assertThat( rows.next() ).isTrue();
							assertThat( extractor.extract( rows, 1, options ) ).as( "raw JDBC %s", input ).isEqualTo( canonical );
							assertThat( extractor.extract( rows, 2, options ) ).as( "prepared JDBC %s", input ).isEqualTo( canonical );
						}
					}
				}
			} ) );
		}
	}

	@Test
	void sameBucketDoesNotInventNewTokensAndApplicationIdentityIsNotRekeyed() {
		try ( var scope = ContextScope.boot( false, false,
				ChangesetPeriodPrecisionTests.settings( new ChangesetPeriodPrecisionTests.Fixed() ),
				ChangesetPeriodPrecisionTests.Matching.class ) ) {
			final var precision = scope.factory.getTemporalPeriodPrecision();
			final var raw = ChangesetPeriodPrecisionTests.RAW;
			final var aligned = ChangesetPeriodPrecisionTests.FIRST;
			assertThat( precision.prepare( raw, false ) ).isEqualTo( precision.prepare( raw.plusNanos( 1 ), false ) );
			assertThat( precision.prepare( aligned, true ) ).isEqualTo( aligned );
			assertThatThrownBy( () -> precision.prepare( raw, true ) )
					.isInstanceOf( TemporalValueException.class ).hasMessageContaining( "will not rekey" );
			assertThat( precision.prepare( 42L, true ) ).isEqualTo( 42L );
		}
	}

	@Test
	void factoriesSharingARegistryDoNotShareResolvedPeriodState() {
		try ( var scope = ContextScope.boot( false, false,
				ChangesetPeriodPrecisionTests.settings( new ChangesetPeriodPrecisionTests.Fixed() ),
				ChangesetPeriodPrecisionTests.Matching.class ) ) {
			final var contract = scope.factory.getTemporalPeriodPrecision();
			final var metadata = new MetadataSources( scope.registry ).addAnnotatedClass( Ordinary.class ).buildMetadata();
			try ( var other = (SessionFactoryImplementor) metadata.buildSessionFactory() ) {
				assertThat( other.getChangesetCoordinator() ).isSameAs( scope.factory.getChangesetCoordinator() );
				assertThat( other.getTemporalPeriodPrecision() ).isNull();
				assertThat( scope.factory.getTemporalPeriodPrecision() ).isSameAs( contract );
				assertThat( contract.prepare( ChangesetPeriodPrecisionTests.RAW, false ) ).isEqualTo( ChangesetPeriodPrecisionTests.FIRST );
			}
		}
	}

	@Test
	void statelessInsertUsesPublishedCanonicalBoundary() {
		final var supplier = new ChangesetPeriodPrecisionTests.Fixed();
		try ( var scope = ContextScope.open( false, false, ChangesetPeriodPrecisionTests.settings( supplier ),
				List.of(), ChangesetPeriodPrecisionTests.Matching.class ) ) {
			try ( var session = scope.factory.openStatelessSession() ) {
				final var transaction = session.beginTransaction();
				session.insert( new ChangesetPeriodPrecisionTests.Matching() );
				assertThat( ((SharedSessionContractImplementor) session).getCurrentChangesetIdentifier() )
						.isEqualTo( ChangesetPeriodPrecisionTests.FIRST );
				transaction.commit();
			}
			assertThat( supplier.calls ).isOne();
			final var model = scope.metadata.getEntityBinding( ChangesetPeriodPrecisionTests.Matching.class.getName() );
			scope.tx( session -> session.doWork( connection -> {
				try ( var statement = connection.createStatement(); var rows = statement.executeQuery(
						"select " + model.getAuxiliaryColumn( "rowStart" ).getName() + " from " + model.getAuxiliaryTable().getName() ) ) {
					assertThat( rows.next() ).isTrue();
					assertThat( rows.getObject( 1, LocalDateTime.class ).toInstant( ZoneOffset.UTC ) )
							.isEqualTo( ChangesetPeriodPrecisionTests.FIRST );
				}
			} ) );
		}
	}

	@Test
	void changingOnlyThePrecisionIntegerDoesNotCertifyAProfile() {
		final var settings = ChangesetPeriodPrecisionTests.settings( new ChangesetPeriodPrecisionTests.Fixed() );
		settings.put( "hibernate.dialect", IntegerOnlyDialect.class );
		assertThatThrownBy( () -> {
			try ( var ignored = ContextScope.boot( false, false, settings, ChangesetPeriodPrecisionTests.First.class ) ) {}
		} ).hasMessageContaining( "explicit storage/producer precision contribution" );
	}

	public static class IntegerOnlyDialect extends H2Dialect {
		@Override public TemporalTableSupport getTemporalTableSupport() {
			return new H2TemporalTableSupport( this ) {
				@Override public int getTemporalColumnPrecision() { return 3; }
			};
		}
	}

	@Test
	void providerContractsResolveWithoutJdbcMetadata() {
		for ( var dialect : List.of( OracleDialect.class, PostgreSQLDialect.class, PostgresPlusDialect.class ) ) {
			final var settings = ChangesetPeriodPrecisionTests.settings( new ChangesetPeriodPrecisionTests.Fixed() );
			settings.put( "hibernate.dialect", dialect );
			settings.put( "jakarta.persistence.database-product-version", dialect == OracleDialect.class ? "23.26.1" : "17.0" );
			try ( var scope = ContextScope.boot( false, false, settings, ChangesetPeriodPrecisionTests.First.class ) ) {
				assertThat( scope.provider.attempts ).isZero();
				assertThat( scope.factory.getTemporalPeriodPrecision() ).isNotNull();
				assertThat( scope.metadata.getEntityBinding( ChangesetPeriodPrecisionTests.First.class.getName() )
						.getAuxiliaryColumn( "rowStart" ).getTemporalPrecision() ).isEqualTo( dialect == OracleDialect.class ? 9 : 6 );
			}
		}
	}

	@Entity static class Ordinary { @Id int id; }
}

package org.hibernate.orm.test.dialect;

import java.sql.SQLException;
import java.util.stream.Stream;

import org.hibernate.community.dialect.SybaseASELegacyDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies the retained SQLException contracts and maintained supply points.
///
/// @author Steve Ebersole
public class SQLExceptionSupportTest {
	@Test
	void rootDefaultsPreserveAbsenceAndTransactionState() {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {};
		final SQLException exception = new SQLException( "unrecognized", "ZZ999", 99 );

		assertThat( dialect.buildSQLExceptionConversionDelegate() ).isNull();
		assertThat( dialect.getViolatedConstraintNameExtractor().extractConstraintName( exception ) ).isNull();
		assertThat( dialect.causesRollback( exception ) ).isFalse();
	}

	@Test
	void maintainedDelegateMayConvertOrDeclineAndExtractorMayNameOrDecline() {
		final H2Dialect dialect = new H2Dialect();
		final SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		final SQLException violation = new SQLException(
				"Unique index or primary key violation: \"UK_FIXTURE ON PUBLIC.T(C)\"",
				"23505",
				23505
		);

		assertThat( delegate.convert( violation, "constraint", "insert" ) )
				.isInstanceOf( ConstraintViolationException.class );
		assertThat( dialect.getViolatedConstraintNameExtractor().extractConstraintName( violation ) )
				.isEqualTo( "UK_FIXTURE" );
		assertThat( delegate.convert( new SQLException( "unknown", "ZZ999", 99 ), "unknown", "select" ) )
				.isNull();
		assertThat( dialect.getViolatedConstraintNameExtractor()
				.extractConstraintName( new SQLException( "unknown", "ZZ999", 99 ) ) ).isNull();
	}

	@Test
	void postgreSqlRecognizesRollbackCausingExceptions() {
		assertThat( new PostgreSQLDialect().causesRollback( new SQLException() ) ).isTrue();
	}

	static Stream<Dialect> sybaseAseDialects() {
		return Stream.of( new SybaseASEDialect(), new SybaseASELegacyDialect() );
	}

	@ParameterizedTest
	@MethodSource("sybaseAseDialects")
	void sybaseAseRecognizesRollbackOnLockTimeout(Dialect dialect) {
		final var timeout = new SQLException( "Aborting the transaction", "S1000", 12205 );
		assertThat( dialect.causesRollback( timeout ) ).isTrue();
		final var wrapper = new SQLException( "Statement failed" );
		wrapper.setNextException( timeout );
		assertThat( dialect.causesRollback( wrapper ) ).isTrue();
		// An explicit LOCK TABLE timeout reports 12207 and leaves the transaction intact.
		assertThat( dialect.causesRollback( new SQLException( "Table lock unavailable", "S1000", 12207 ) ) )
				.isFalse();
	}
}

package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.sql.Timestamp;
import java.util.List;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Broken source intervals must be diagnosed before a migration can alter them.
/// These owned fixtures deliberately model historical defects, not an automatic repair.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@RequiresDialect(OracleDialect.class)
@RequiresDialect(PostgreSQLDialect.class)
class TemporalMalformedHistoryMigrationTests {
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void malformedSourceIsUnchanged(boolean collection) throws Exception {
		var migration = new TemporalPeriodMigrationFixture( "M01", false, false );
		try {
			var evidence = migration.write();
			List<TemporalPeriodFixture.Interval> entityBefore;
			List<TemporalPeriodFixture.Interval> collectionBefore;
			try ( var source = migration.fixture( false, false ) ) {
				String table = collection ? source.collectionTable : source.entityTable;
				String column = collection ? "valid_from" : "valid_to";
				String predicate = collection ? "tag_value='two'" : "payload='v1'";
				try ( var statement = source.reader.prepareStatement( "update " + table + " set " + column + "=? where " + predicate ) ) {
					statement.setTimestamp( 1, Timestamp.from( TemporalPeriodFixture.SEASONAL[1].minusSeconds( 1 ) ) );
					assertThat( statement.executeUpdate() ).isEqualTo( 1 );
				}
				source.reader.commit();
				entityBefore = TemporalPeriodMigrationFixture.sourceIntervals( source, false );
				collectionBefore = TemporalPeriodMigrationFixture.sourceIntervals( source, true );
			}
			assertThatThrownBy( () -> migration.read( evidence ) ).isInstanceOf( AssertionError.class )
					.hasMessageContaining( collection ? "source collection interval" : "source entity continuity" );
			try ( var source = migration.fixture( false, false ) ) {
				assertThat( TemporalPeriodMigrationFixture.sourceIntervals( source, false ) ).isEqualTo( entityBefore );
				assertThat( TemporalPeriodMigrationFixture.sourceIntervals( source, true ) ).isEqualTo( collectionBefore );
			}
		}
		finally { migration.cleanup(); }
	}
}

package org.hibernate.orm.test.mapping.javatime.redesign.context;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.jdbc.LocalDateTimeJdbcType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Offline SQL-generation controls do not substitute for Oracle 19/21 live evidence.
///
/// @author Steve Ebersole
class ContextOracleVersionTest {
	@ParameterizedTest
	@ValueSource(ints = {19, 21, 23})
	void databaseVersionSelectsCompatibleClockAndDropSyntax(int major) {
		assertEquals( major + ".0", ContextEnvironment.checkedVersion( null, major, 0 ) );
		var dialect = new OracleDialect( DatabaseVersion.make( major ) );
		assertEquals( major < 23 ? "select systimestamp from dual" : "select systimestamp",
				dialect.getCurrentTemporalSupport().getCurrentTimestampSelection().command() );
		assertEquals( "select localtimestamp from dual", dialect.getCurrentTemporalSupport()
				.getCurrentTimestampSelection( LocalDateTimeJavaType.INSTANCE, LocalDateTimeJdbcType.INSTANCE ).command() );
		assertEquals( major < 23 ? ExistenceCheckPlacement.NONE : ExistenceCheckPlacement.BEFORE_NAME,
				dialect.getIfExistsSupport().dropTablePlacement() );
	}
}

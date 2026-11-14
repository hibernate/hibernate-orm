package org.hibernate.orm.test.type.temporal.preservation;

import java.sql.Types;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.type.internal.PostgreSQLOffsetTimeJdbcType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/// Driver metadata identifies timetz as TIME. Resolve its preserving descriptor
/// on the new path while retaining legacy UTC-time metadata resolution.
///
/// @author Steve Ebersole
class PostgreSQLTemporalMetadataTests {
	@Test
	void resolvesMetadataAccordingToContributedHandling() {
		final var registry = new TypeConfiguration().getJdbcTypeRegistry();
		final var dialect = new PostgreSQLDialect();
		assertThat( dialect.resolveSqlTypeDescriptor( "timetz", Types.TIME, 0, 6, registry ).getDefaultSqlTypeCode() )
				.isEqualTo( SqlTypes.TIME_UTC );
		registry.addDescriptor( PostgreSQLOffsetTimeJdbcType.INSTANCE );
		assertThat( dialect.resolveSqlTypeDescriptor( "timetz", Types.TIME, 0, 6, registry ).getDefaultSqlTypeCode() )
				.isEqualTo( Types.TIME_WITH_TIMEZONE );
		assertThat( dialect.resolveSqlTypeDescriptor( "time", Types.TIME, 0, 6, registry ).getDefaultSqlTypeCode() )
				.isEqualTo( Types.TIME );
	}
}

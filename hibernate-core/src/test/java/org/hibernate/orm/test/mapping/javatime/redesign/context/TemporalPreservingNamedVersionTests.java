package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/// Named HBM version types retain the version role under both preservation modes.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalPreservingNamedVersionTests {
	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void namedVersionAcrossMetadataModes(boolean metadata) throws InterruptedException {
		for ( boolean enabled : List.of( true, false ) ) {
			for ( String format : List.of( "hbm", "orm" ) ) {
			try ( var scope = ContextScope.open( metadata, !enabled,
					Map.of( MappingSettings.TEMPORAL_PRESERVATION, enabled ),
					List.of( "org/hibernate/orm/test/mapping/javatime/redesign/context/preserving-version." + format + ".xml" ) ) ) {
				final var row = new Named();
				scope.tx( session -> session.persist( row ) );
				scope.tx( session -> assertThat( session.find( Named.class, 1 ).stamp ).isEqualTo( row.stamp ) );
				Thread.sleep( 15 );
				scope.tx( session -> session.find( Named.class, 1 ).payload = "changed" );
				scope.tx( session -> assertThat( session.find( Named.class, 1 ).stamp ).isNotEqualTo( row.stamp ) );
			}
		}
	}

	}

	public static class Named {
		int id = 1;
		Instant stamp;
		String payload = "initial";
	}
}

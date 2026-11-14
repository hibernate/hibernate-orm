package org.hibernate.orm.test.mapping.basic;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Unassessed or offset-losing native timestamps cannot satisfy the enabled example.
///
/// @author Steve Ebersole
@SkipForDialect(dialectClass = H2Dialect.class, reason = "Accepted INLINE mapping is tested separately")
@SkipForDialect(dialectClass = OracleDialect.class, reason = "Accepted INLINE mapping is tested separately")
class OffsetDateTimeRejectedMappingTests {
	@Test
	void rejectsUnsupportedInlineExample() {
		try ( var registry = new StandardServiceRegistryBuilder()
				.applySetting( MappingSettings.TEMPORAL_PRESERVATION, true )
				.applySetting( "hibernate.hbm2ddl.auto", "none" ).build() ) {
			assertThatThrownBy( () -> {
				try ( var ignored = new MetadataSources( registry )
						.addAnnotatedClass( OffsetDateTimeMappingTests.EntityWithOffsetDateTime.class )
						.buildMetadata().buildSessionFactory() ) {}
			} ).isInstanceOf( MappingException.class )
					.hasMessageContaining( "preservation" ).hasMessageContaining( "offsetDateTime" );
		}
	}
}

package org.hibernate.orm.test.envers.integration.reventity;

import java.sql.SQLException;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureCheck;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.internal.TemporalMappingValidator;

import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.Level.SUPPORTED;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.DATE;
import static org.hibernate.dialect.temporaltype.spi.TemporalPreservation.TemporalPart.TIME;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Resolves the revision fixture's implicit LocalDateTime pair without boot JDBC.
/// Unsupported pairs have separate negative bootstrap coverage.
///
/// @author Steve Ebersole
public class RevisionLocalDateTimeEligibility implements DialectFeatureCheck {
	@Override
	public boolean apply(Dialect dialect) {
		final var guard = mock( ConnectionProvider.class );
		try {
			when( guard.getConnection() ).thenThrow( new AssertionError( "No eligibility JDBC" ) );
			try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
					.addService( ConnectionProvider.class, guard )
					.applySetting( AvailableSettings.DIALECT, dialect )
					.applySetting( AvailableSettings.ALLOW_METADATA_ON_BOOT, false )
					.applySetting( AvailableSettings.TEMPORAL_PRESERVATION, true ).build() ) {
				final var metadata = (MetadataImplementor) new MetadataSources( registry ).buildMetadata();
				final var java = LocalDateTimeJavaType.INSTANCE;
				final var jdbc = java.getRecommendedJdbcType( metadata.getTypeConfiguration().getCurrentBaseSqlTypeIndicators() );
				final var report = TemporalMappingValidator.assess( dialect, java, jdbc, "revision timestamp eligibility" );
				return report.preserves( DATE ) == SUPPORTED && report.preserves( TIME ) == SUPPORTED;
			}
			finally {
				verify( guard, never() ).getConnection();
			}
		}
		catch (SQLException e) {
			throw new AssertionError( e );
		}
	}
}

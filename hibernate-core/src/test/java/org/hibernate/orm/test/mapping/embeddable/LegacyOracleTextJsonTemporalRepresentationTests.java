package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.DialectSpecificSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/// Independent factory retains the same representation assertions under its selected settings.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = {
		@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"),
		@Setting(name = DialectSpecificSettings.ORACLE_OSON_DISABLED, value = "true")
})
class LegacyOracleTextJsonTemporalRepresentationTests extends OracleJsonTemporalRepresentationTests {
	@Override
	boolean preservationEnabled() {
		return false;
	}
	@Override
	boolean osonDisabled() {
		return true;
	}
}

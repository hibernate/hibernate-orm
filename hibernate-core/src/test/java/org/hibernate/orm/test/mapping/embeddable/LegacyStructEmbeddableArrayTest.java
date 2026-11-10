package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SettingProvider;

/// Retains the native aggregate array lifecycle with legacy temporal descriptors.
///
/// @author Steve Ebersole
@ServiceRegistry(
		settings = {
				@Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"),
				@Setting(name = AvailableSettings.COLUMN_ORDERING_STRATEGY, value = "legacy")
		},
		settingProviders = @SettingProvider(
				settingName = AvailableSettings.PREFERRED_ARRAY_JDBC_TYPE,
				provider = OracleNestedTableSettingProvider.class
		)
)
class LegacyStructEmbeddableArrayTest extends StructEmbeddableArrayTest {
}

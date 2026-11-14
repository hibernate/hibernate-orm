package org.hibernate.orm.test.idgen.mixed;

import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.generator.internal.CurrentTimestampGeneration;
import org.hibernate.orm.test.annotations.MutableClockSettingProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;

import static org.assertj.core.api.Assertions.assertThat;

/// Enabled H2 counterpart of the compatibility fixture; inherits its unchanged
/// generation, immutability and mutation assertions and original model.
///
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(
		settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "true"),
		settingProviders = @SettingProvider(
				settingName = CurrentTimestampGeneration.CLOCK_SETTING_NAME,
				provider = MutableClockSettingProvider.class
		)
)
class EnabledMixedTypeEmbeddableGeneratorsTest2 extends MixedTypeEmbeddableGeneratorsTest2 {

	@BeforeEach
	void preservationIsEnabled(SessionFactoryScope scope) {
		assertThat( scope.getSessionFactory().getSessionFactoryOptions().isTemporalPreservationEnabled() ).isTrue();
	}
}

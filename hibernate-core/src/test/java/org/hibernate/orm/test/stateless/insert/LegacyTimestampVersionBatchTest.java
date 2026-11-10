package org.hibernate.orm.test.stateless.insert;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/// Retains the legacy timestamp producer's batching contract alongside the default enabled path.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
class LegacyTimestampVersionBatchTest extends TimestampVersionBatchTest {
}

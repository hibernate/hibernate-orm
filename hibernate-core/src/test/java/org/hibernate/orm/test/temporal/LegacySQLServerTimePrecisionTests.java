package org.hibernate.orm.test.temporal;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/// Compatibility mode retains explicit conversion coverage and the same TIME DDL contract.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
class LegacySQLServerTimePrecisionTests extends SQLServerTimePrecisionTests {
	@Override
	boolean preservationEnabled() { return false; }
}

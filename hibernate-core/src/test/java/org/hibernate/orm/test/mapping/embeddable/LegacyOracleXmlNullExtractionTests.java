package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/// Independent legacy factory executes the same nested XML null assertions.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
class LegacyOracleXmlNullExtractionTests extends OracleXmlNullExtractionTests {
	@Override
	boolean preservationEnabled() { return false; }
}

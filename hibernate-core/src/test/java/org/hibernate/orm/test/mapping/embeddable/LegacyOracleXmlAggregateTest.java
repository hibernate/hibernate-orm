package org.hibernate.orm.test.mapping.embeddable;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;

/// Retains the existing XML lifecycle assertions with legacy temporal handling.
///
/// @author Steve Ebersole
@RequiresDialect(OracleDialect.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
class LegacyOracleXmlAggregateTest extends XmlEmbeddableTest {
}

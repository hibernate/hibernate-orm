package org.hibernate.orm.test.version;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/// Runs the original regression body in legacy mode without inheriting enabled eligibility.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = LocalDateTimeVersionTest.TheEntity.class)
@SessionFactory
class LegacyLocalDateTimeVersionTest {
	@Test
	void dropTestData(SessionFactoryScope scope) {
		new LocalDateTimeVersionTest().dropTestData( scope );
	}

	@Test
	void testInstantUsageAsVersion(SessionFactoryScope scope) {
		new LocalDateTimeVersionTest().testInstantUsageAsVersion( scope );
	}
}

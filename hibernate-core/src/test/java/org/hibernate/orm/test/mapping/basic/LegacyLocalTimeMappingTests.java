package org.hibernate.orm.test.mapping.basic;

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
@DomainModel(annotatedClasses = LocalTimeMappingTests.EntityWithLocalTime.class)
@SessionFactory
class LegacyLocalTimeMappingTests {
	@Test
	void verifyMappings(SessionFactoryScope scope) {
		new LocalTimeMappingTests().verifyMappings( scope );
	}
}

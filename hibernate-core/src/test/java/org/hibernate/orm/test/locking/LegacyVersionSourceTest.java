package org.hibernate.orm.test.locking;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/// Runs the original regression body in legacy mode without inheriting enabled eligibility.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = VersionSourceTest.Person.class)
@SessionFactory
class LegacyVersionSourceTest {
	@Test
	void test(SessionFactoryScope scope) {
		new VersionSourceTest().test( scope );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}
}

package org.hibernate.orm.test.locking;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.Test;

/// Runs the original regression body in legacy mode without inheriting enabled eligibility.
///
/// @author Steve Ebersole
@Jpa(annotatedClasses = CurrentTimestampVersionTest.Timestamped.class,
		integrationSettings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
class LegacyCurrentTimestampVersionTest {
	@Test
	void test(EntityManagerFactoryScope scope) {
		new CurrentTimestampVersionTest().test( scope );
	}

	@AfterEach
	void tearDown(EntityManagerFactoryScope scope) {
		scope.dropData();
	}
}

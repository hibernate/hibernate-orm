package org.hibernate.orm.test.stateless.insert;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/// Runs the original regression body in legacy mode without inheriting enabled eligibility.
///
/// @author Steve Ebersole
@JiraKey("HHH-21093")
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = TimestampVersionBatchTest.TimestampedEntry.class)
@SessionFactory
class LegacyTimestampVersionBatchTest {
	@BeforeEach
	void checkDialect(SessionFactoryScope scope) {
		new TimestampVersionBatchTest().checkDialect( scope );
	}

	@Test
	void fetchingVersionTimestampsPreservesTheBatch(SessionFactoryScope scope) {
		new TimestampVersionBatchTest().fetchingVersionTimestampsPreservesTheBatch( scope );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}
}

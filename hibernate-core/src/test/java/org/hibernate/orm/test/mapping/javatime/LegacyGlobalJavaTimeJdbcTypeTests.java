package org.hibernate.orm.test.mapping.javatime;


import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.Setting;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;



/// Independent compatibility factory reuses the original type-specific assertions.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = GlobalJavaTimeJdbcTypeTests.EntityWithInstant.class)
@SessionFactory
class LegacyGlobalJavaTimeJdbcTypeTests {
	@Test
	void lifecycle(SessionFactoryScope scope) {
		new GlobalJavaTimeJdbcTypeTests().testInstant( scope );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}

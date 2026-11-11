package org.hibernate.orm.test.mapping.javatime;


import org.hibernate.cfg.MappingSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.testing.orm.junit.Setting;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;



/// Independent compatibility factory reuses the original type-specific assertions.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@DomainModel(annotatedClasses = GlobalJavaTimeLocalTimeJdbcTypeTests.EntityWithLocalTime.class)
@SessionFactory
class LegacyGlobalJavaTimeLocalTimeJdbcTypeTests {
	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "Oracle drivers truncate fractional seconds from the LocalTime")
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA time type does not support fractional seconds")
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase drivers truncate fractional seconds from the LocalTime")
	void lifecycle(SessionFactoryScope scope) {
		new GlobalJavaTimeLocalTimeJdbcTypeTests().testLocalTime( scope );
	}

	@Test
	void mappings(DomainModelScope scope, SessionFactoryScope factory) {
		new GlobalJavaTimeLocalTimeJdbcTypeTests().testMappings( scope, factory );
	}

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}

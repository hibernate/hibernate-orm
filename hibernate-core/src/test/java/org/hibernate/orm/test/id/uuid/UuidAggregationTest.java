package org.hibernate.orm.test.id.uuid;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = EntityOfBasics.class)
@SessionFactory
@SkipForDialect( dialectClass = SpannerPostgreSQLDialect.class, reason = "Aggregate functions doesn't work with UUID")
// Retain the legacy temporal semantics of this regression fixture.
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
public class UuidAggregationTest {
	@Test
	@JiraKey(value = "HHH-15495")
	public void testMaxUuid(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createSelectionQuery( "select max(theUuid) from EntityOfBasics", UUID.class ).getSingleResult();
		} );
	}
}

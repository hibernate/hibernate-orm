package org.hibernate.orm.test.hql;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Retains the original HHH-20302 zoned aggregate checks under legacy interpretation.
/// Enabled H2 mappings reject loss of regional zone identity.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = LegacyZonedDateTimeMinMaxFunctionTest.Foo.class)
@ServiceRegistry(settings = @Setting(name = MappingSettings.TEMPORAL_PRESERVATION, value = "false"))
@SessionFactory
@RequiresDialect(H2Dialect.class)
@JiraKey("HHH-20302")
class LegacyZonedDateTimeMinMaxFunctionTest {
	@Test
	void maxMinFunctionsPreserveLegacyZonedDateTimeAggregation(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			var foo1 = new Foo();
			var earlier = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
			foo1.zdtField = earlier;
			session.persist(foo1);

			var foo2 = new Foo();
			var later = ZonedDateTime.of(2000, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));
			foo2.zdtField = later;
			session.persist(foo2);

			ZonedDateTime maxValue = session.createSelectionQuery(
					"select max(zdtField) from Foo", ZonedDateTime.class
			).getSingleResult();

			assertTrue(later.isEqual(maxValue),
					"MAX() on ZonedDateTime should return the latest value with legacy temporal interpretation");

			ZonedDateTime minValue = session.createSelectionQuery(
					"select min(zdtField) from Foo", ZonedDateTime.class
			).getSingleResult();

			assertTrue(earlier.isEqual(minValue),
					"MIN() on ZonedDateTime should return the earliest value with legacy temporal interpretation");
		});
	}

	@Entity(name = "Foo")
	static class Foo {
		@Id @GeneratedValue Long id;
		ZonedDateTime zdtField;
	}
}

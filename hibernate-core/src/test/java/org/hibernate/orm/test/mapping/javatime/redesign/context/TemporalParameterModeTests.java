package org.hibernate.orm.test.mapping.javatime.redesign.context;

import java.time.OffsetDateTime;
import java.util.Map;
import jakarta.persistence.*;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

/// The receiving-mapping guard belongs to the owning factory's preservation mode.
/// @author Steve Ebersole
@RequiresDialect(H2Dialect.class)
class TemporalParameterModeTests {
	@ParameterizedTest @ValueSource(booleans = {true, false})
	void parameterReuseFollowsFactoryMode(boolean metadata) {
		try (var legacy = ContextScope.open(metadata, Map.of(MappingSettings.TEMPORAL_PRESERVATION, false), Event.class);
			var enabled = ContextScope.boot(metadata, true, Map.of(MappingSettings.TEMPORAL_PRESERVATION, true), Event.class)) {
			enabled.provider.blocked = false;
			final var value = OffsetDateTime.parse("2026-07-01T12:00:00+02:00");
			legacy.tx(s -> {var row = new Event(); row.id=1; row.first=value; row.second=value; s.persist(row);});
			for (var scope : new ContextScope[]{legacy, enabled}) {
				scope.tx(s -> {
					assertThat(s.createSelectionQuery("from ParameterModeEvent e where e.first=:p or e.first=:p", Event.class)
						.setParameter("p", value).getSingleResult().id).isEqualTo(1);
					assertThat(s.createSelectionQuery("from ParameterModeEvent e where e.first=:p or e.second=:q", Event.class)
						.setParameter("p",value).setParameter("q",value).getSingleResult().id).isEqualTo(1);
				});
			}
			legacy.tx(s -> {
				assertThat(s.createSelectionQuery("from ParameterModeEvent e where e.first=:p or e.second=:p", Event.class)
					.setParameter("p",value).getSingleResult().id).isEqualTo(1);
				assertThat(s.createSelectionQuery("from ParameterModeEvent e where e.first=?1 or e.second=?1", Event.class)
					.setParameter(1,value).getSingleResult().id).isEqualTo(1);
			});
			enabled.measure();
			assertThatThrownBy(() -> enabled.tx(s -> s.createSelectionQuery(
					"from ParameterModeEvent e where e.first=:p or e.second=:p", Event.class)
					.setParameter("p",value).getResultList()))
				.hasMessageContaining("different temporal bindings").hasMessageContaining("first").hasMessageContaining("second");
			enabled.noSelect();
		}
	}

	@ParameterizedTest @ValueSource(booleans = {true, false})
	void deprecatedAliasAlsoDisablesGuard(boolean metadata) {
		try (var scope=ContextScope.open(metadata, false, Map.of(), java.util.List.of(), Event.class)) {
			assertThat(scope.factory.getSessionFactoryOptions().isTemporalPreservationEnabled()).isFalse();
			scope.tx(s -> assertThat(s.createSelectionQuery("from ParameterModeEvent e where e.first=:p or e.second=:p",Event.class)
				.setParameter("p",OffsetDateTime.parse("2026-07-01T12:00:00+02:00")).getResultList()).isEmpty());
		}
	}

	@Entity(name="ParameterModeEvent")
	static class Event {
		@Id int id;
		@Column(name="first_stamp") @TimeZoneStorage(TimeZoneStorageType.COLUMN) OffsetDateTime first;
		@Column(name="second_stamp") @TimeZoneStorage(TimeZoneStorageType.COLUMN) OffsetDateTime second;
	}
}

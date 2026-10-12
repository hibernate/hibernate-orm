package org.hibernate.orm.test.mapping.userguide.converters;

import java.time.Period;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.hibernate.orm.test.mapping.converter.PeriodStringConverter;
import org.hibernate.type.NumericBooleanConverter;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Verifies automatic, explicit, disabled, and map-part converter selection.
///
/// @author Steve Ebersole
@DomainModel(
		annotatedClasses = ConverterSelectionTests.Settings.class,
		annotatedPackageNames = "org.hibernate.orm.test.mapping.userguide.converters"
)
@SessionFactory
public class ConverterSelectionTests {
	@Test
	void selectionAndRoundTrip(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				Settings settings = new Settings();
				settings.id = 1L;
				settings.automatic = true;
				settings.explicit = true;
				settings.disabled = true;
				settings.entries.put( Period.ofDays( 2 ), true );
				session.persist( settings );
			} );
			scope.inTransaction( session -> {
				Object[] row = session.createNativeQuery(
						"select automatic_flag, explicit_flag, disabled_flag from guide_settings", Object[].class
				).getSingleResult();
				assertThat( row[0].toString() ).isEqualTo( "Y" );
				assertThat( ((Number) row[1]).intValue() ).isEqualTo( 1 );
				assertThat( row[2] ).isNotNull();
				assertThat( row[2].toString() ).isNotEqualTo( "Y" );
				Settings settings = session.find( Settings.class, 1L );
				assertThat( settings.automatic ).isTrue();
				assertThat( settings.explicit ).isTrue();
				assertThat( settings.disabled ).isTrue();
				assertThat( settings.entries ).containsEntry( Period.ofDays( 2 ), true );
				Object[] mapRow = session.createNativeQuery(
						"select duration_key, enabled_value from guide_settings_entries", Object[].class
				).getSingleResult();
				assertThat( mapRow[0].toString() ).isEqualTo( "P2D" );
				assertThat( ((Number) mapRow[1]).intValue() ).isEqualTo( 1 );
			} );
		}
		finally {
			scope.getSessionFactory().getSchemaManager().truncate();
		}
	}

	@Entity(name = "GuideSettings")
	@Table(name = "guide_settings")
	public static class Settings {
		@Id
		Long id;
		//tag::guide-converter-selection[]
		@Column(name = "automatic_flag")
		Boolean automatic;

		@Convert(converter = NumericBooleanConverter.class)
		@Column(name = "explicit_flag")
		Boolean explicit;

		@Convert(disableConversion = true)
		@Column(name = "disabled_flag")
		Boolean disabled;
		//end::guide-converter-selection[]
		//tag::guide-converter-map[]
		@ElementCollection
		@CollectionTable(name = "guide_settings_entries")
		@MapKeyColumn(name = "duration_key")
		@Column(name = "enabled_value")
		@Convert(attributeName = "key", converter = PeriodStringConverter.class)
		@Convert(attributeName = "value", converter = NumericBooleanConverter.class)
		Map<Period, Boolean> entries = new HashMap<>();
		//end::guide-converter-map[]
	}
}

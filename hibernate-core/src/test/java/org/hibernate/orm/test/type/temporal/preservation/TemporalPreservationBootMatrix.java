package org.hibernate.orm.test.type.temporal.preservation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.annotations.TimeZoneAdjustment;
import org.hibernate.annotations.TimeZoneRepresentation;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.type.spi.TemporalPreservation.Level;
import org.hibernate.dialect.type.spi.TemporalPreservation.Part;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.TemporalPreservationSettings;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.util.ServiceRegistryUtil;

/// Boots one independent factory per Java Time class against the database
/// and JDBC driver of the current test profile, and records what was selected
/// and whether the mapping was accepted.
///
/// Nothing here is answered from a Dialect in isolation: the registry, the
/// driver and the connection are those of the profile the tests run with.
///
/// @author Steve Ebersole
final class TemporalPreservationBootMatrix {
	static final Map<Class<?>, Class<?>> ENTITIES = Map.of(
			LocalDate.class, WithLocalDate.class,
			LocalTime.class, WithLocalTime.class,
			LocalDateTime.class, WithLocalDateTime.class,
			Instant.class, WithInstant.class,
			OffsetTime.class, WithOffsetTime.class,
			OffsetDateTime.class, WithOffsetDateTime.class,
			ZonedDateTime.class, WithZonedDateTime.class
	);

	static final List<Class<?>> JAVA_TIME_CLASSES = List.of(
			LocalDate.class,
			LocalTime.class,
			LocalDateTime.class,
			Instant.class,
			OffsetTime.class,
			OffsetDateTime.class,
			ZonedDateTime.class
	);

	private TemporalPreservationBootMatrix() {
	}

	/// One cell of the matrix.
	///
	/// @param dialect the simple name of the Dialect class
	/// @param javaClass the simple name of the Java Time class, followed in
	///        parentheses by `INLINE` or `COLUMN` when that representation is
	///        declared, by `NORMALIZED` when booted with that policy, or by
	///        `ADJUSTED` when its zone is declared with `@TimeZoneAdjustment`
	/// @param jdbcType the simple name of the selected JDBC type class
	/// @param columnType the column type in generated DDL
	/// @param preservedParts the required parts reported as supported
	/// @param instantBasis how an instant is carried, or empty
	/// @param minimum the least preserved value, or empty
	/// @param maximum the greatest preserved value, or empty
	/// @param outcome `QUALIFIED` when the factory was built, `REJECTED` when
	///        the mapping was rejected, `DISABLED` when temporal preservation
	///        was not enabled
	record Cell(
			String dialect,
			String javaClass,
			String jdbcType,
			String columnType,
			String preservedParts,
			String instantBasis,
			String minimum,
			String maximum,
			String outcome) {
		String toCsv() {
			return Stream.of(
					dialect,
					javaClass,
					jdbcType,
					columnType,
					preservedParts,
					instantBasis,
					minimum,
					maximum,
					outcome
			).map( field -> field.contains( "," ) ? '"' + field + '"' : field ).collect( Collectors.joining( "," ) );
		}
	}

	static final String CSV_HEADER =
			"Dialect,Java class,JDBC type,Column type,Preserved parts,Instant basis,Minimum,Maximum,Outcome";

	/// Boot each Java Time class with the given settings applied on top of
	/// the current test profile, with nothing declared on the attribute.
	static List<Cell> boot(Map<String, Object> settings) {
		final List<Cell> cells = new ArrayList<>();
		for ( var javaClass : JAVA_TIME_CLASSES ) {
			cells.add( boot( ENTITIES.get( javaClass ), javaClass, "", settings, false ) );
		}
		return cells;
	}

	/// As [#boot(Map)], followed by `OffsetTime`, `OffsetDateTime` and `ZonedDateTime` with
	/// each explicit representation, with the `NORMALIZED` policy, and with a
	/// zone declared by `@TimeZoneAdjustment`.
	static List<Cell> bootWithPolicies(Map<String, Object> settings) {
		final List<Cell> cells = boot( settings );
		final Map<String, Object> normalized = new HashMap<>( settings );
		normalized.put( MappingSettings.TEMPORAL_PRESERVATION_POLICY, "normalized" );
		cells.add( boot( WithOffsetTimeInline.class, OffsetTime.class, "INLINE", settings, false ) );
		cells.add( boot( WithOffsetDateTimeInline.class, OffsetDateTime.class, "INLINE", settings, false ) );
		cells.add( boot( WithZonedDateTimeInline.class, ZonedDateTime.class, "INLINE", settings, false ) );
		cells.add( boot( WithOffsetTimeColumn.class, OffsetTime.class, "COLUMN", settings, false ) );
		cells.add( boot( WithOffsetDateTimeColumn.class, OffsetDateTime.class, "COLUMN", settings, false ) );
		cells.add( boot( WithZonedDateTimeColumn.class, ZonedDateTime.class, "COLUMN", settings, false ) );
		cells.add( boot( WithOffsetTime.class, OffsetTime.class, "NORMALIZED", normalized, true ) );
		cells.add( boot( WithOffsetDateTime.class, OffsetDateTime.class, "NORMALIZED", normalized, true ) );
		cells.add( boot( WithZonedDateTime.class, ZonedDateTime.class, "NORMALIZED", normalized, true ) );
		// with a declared zone, the column need only hold the instant
		cells.add( boot( WithOffsetTimeAdjusted.class, OffsetTime.class, "ADJUSTED", settings, true ) );
		cells.add( boot( WithOffsetDateTimeAdjusted.class, OffsetDateTime.class, "ADJUSTED", settings, true ) );
		cells.add( boot( WithZonedDateTimeAdjusted.class, ZonedDateTime.class, "ADJUSTED", settings, true ) );
		return cells;
	}

	/// Boot one entity, whose attribute `theValue` is of the given class.
	///
	/// @param mark what distinguishes the mapping from the default one, or empty
	/// @param normalized whether only the instant is required of the column:
	///        the settings select the `NORMALIZED` policy, or the zone is declared
	static Cell boot(
			Class<?> entityClass,
			Class<?> javaClass,
			String mark,
			Map<String, Object> settings,
			boolean normalized) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		settings.forEach( registryBuilder::applySetting );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = (MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClass( entityClass )
					.buildMetadata();
			final var dialect = metadata.getDatabase().getDialect();
			final boolean enabled = TemporalPreservationSettings.resolve(
					registry.requireService( ConfigurationService.class ).getSettings()
			).preservationEnabled();
			final var value = metadata.getEntityBinding( entityClass.getName() ).getProperty( "theValue" ).getValue();

			// one column, or the instant in one column and the offset or zone in a second
			final BasicValue primary;
			final BasicValue companion;
			if ( value instanceof Component component ) {
				primary = (BasicValue) component.getProperty(
						component.hasProperty( "instant" ) ? "instant" : "utcTime"
				).getValue();
				companion = (BasicValue) component.getProperty( component.getPropertySpan() == 2
						&& component.hasProperty( "zoneId" ) ? "zoneId" : "zoneOffset" ).getValue();
			}
			else {
				primary = (BasicValue) value;
				companion = null;
			}
			final JavaType<?> javaType = primary.resolve().getRelationalJavaType();
			final JdbcType jdbcType = primary.resolve().getJdbcType();
			final String jdbcTypes = jdbcType.getClass().getSimpleName()
				+ ( companion == null ? "" : " + " + companion.resolve().getJdbcType().getClass().getSimpleName() );
			final String columnTypes = ( (Column) primary.getColumn() ).getSqlType( metadata )
				+ ( companion == null ? "" : " + " + ( (Column) companion.getColumn() ).getSqlType( metadata ) );
			final String label = javaClass.getSimpleName() + ( mark.isEmpty() ? "" : " (" + mark + ")" );

			final boolean built;
			try ( var sessionFactory = metadata.buildSessionFactory() ) {
				built = true;
			}
			catch (MappingException e) {
				if ( !enabled ) {
					throw e;
				}
				return new Cell(
						dialect.getClass().getSimpleName(), label, jdbcTypes, columnTypes, "", "", "", "", "REJECTED"
				);
			}
			if ( !enabled ) {
				return new Cell(
						dialect.getClass().getSimpleName(), label, jdbcTypes, columnTypes, "", "", "", "", "DISABLED"
				);
			}

			final var preservation = dialect.getTemporalPreservation( javaType, jdbcType );
			final var parts = new StringJoiner( "+" );
			if ( companion != null ) {
				// the second column supplies what the instant, or the time at UTC, lacks
				parts.add( javaClass == OffsetTime.class ? "TIME" : "INSTANT" );
				if ( !normalized ) {
					parts.add( "OFFSET" );
					if ( javaClass == ZonedDateTime.class ) {
						parts.add( "ZONE_ID" );
					}
				}
			}
			else {
				final var required = normalized ? Part.requiredOfNormalized( javaClass ) : Part.requiredBy( javaClass );
				for ( var part : Part.values() ) {
					if ( required.contains( part ) && preservation.level( part ) == Level.SUPPORTED ) {
						parts.add( part.name() );
					}
				}
			}
			return new Cell(
					dialect.getClass().getSimpleName(),
					label,
					jdbcTypes,
					columnTypes,
					parts.toString(),
					preservation.instantBasis() == null ? "" : preservation.instantBasis().name(),
					preservation.minimum() == null ? "" : preservation.minimum().toString(),
					preservation.maximum() == null ? "" : preservation.maximum().toString(),
					"QUALIFIED"
			);
		}
	}

	@Entity(name = "WithOffsetTimeInline")
	@Table(name = "tp_offset_time_inline")
	static class WithOffsetTimeInline {
		@Id
		Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		OffsetTime theValue;
	}

	@Entity(name = "WithOffsetTimeColumn")
	@Table(name = "tp_offset_time_column")
	static class WithOffsetTimeColumn {
		@Id
		Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		OffsetTime theValue;
	}

	@Entity(name = "WithOffsetTimeAdjusted")
	@Table(name = "tp_offset_time_adjusted")
	static class WithOffsetTimeAdjusted {
		@Id
		Integer id;
		@TimeZoneAdjustment("Z")
		OffsetTime theValue;
	}

	@Entity(name = "WithOffsetDateTimeInline")
	@Table(name = "tp_offset_date_time_inline")
	static class WithOffsetDateTimeInline {
		@Id
		Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		OffsetDateTime theValue;
	}

	@Entity(name = "WithZonedDateTimeInline")
	@Table(name = "tp_zoned_date_time_inline")
	static class WithZonedDateTimeInline {
		@Id
		Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.INLINE)
		ZonedDateTime theValue;
	}

	@Entity(name = "WithOffsetDateTimeColumn")
	@Table(name = "tp_offset_date_time_column")
	static class WithOffsetDateTimeColumn {
		@Id
		Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		OffsetDateTime theValue;
	}

	@Entity(name = "WithZonedDateTimeColumn")
	@Table(name = "tp_zoned_date_time_column")
	static class WithZonedDateTimeColumn {
		@Id
		Integer id;
		@TimeZoneRepresentation(TimeZoneRepresentation.Type.COLUMN)
		ZonedDateTime theValue;
	}

	@Entity(name = "WithOffsetDateTimeAdjusted")
	@Table(name = "tp_offset_date_time_adjusted")
	static class WithOffsetDateTimeAdjusted {
		@Id
		Integer id;
		@TimeZoneAdjustment("UTC")
		OffsetDateTime theValue;
	}

	@Entity(name = "WithZonedDateTimeAdjusted")
	@Table(name = "tp_zoned_date_time_adjusted")
	static class WithZonedDateTimeAdjusted {
		@Id
		Integer id;
		@TimeZoneAdjustment("UTC")
		ZonedDateTime theValue;
	}

	@Entity(name = "WithLocalDate")
	@Table(name = "tp_local_date")
	static class WithLocalDate {
		@Id
		Integer id;
		LocalDate theValue;
	}

	@Entity(name = "WithLocalTime")
	@Table(name = "tp_local_time")
	static class WithLocalTime {
		@Id
		Integer id;
		LocalTime theValue;
	}

	@Entity(name = "WithLocalDateTime")
	@Table(name = "tp_local_date_time")
	static class WithLocalDateTime {
		@Id
		Integer id;
		LocalDateTime theValue;
	}

	@Entity(name = "WithInstant")
	@Table(name = "tp_instant")
	static class WithInstant {
		@Id
		Integer id;
		Instant theValue;
	}

	@Entity(name = "WithOffsetTime")
	@Table(name = "tp_offset_time")
	static class WithOffsetTime {
		@Id
		Integer id;
		OffsetTime theValue;
	}

	@Entity(name = "WithOffsetDateTime")
	@Table(name = "tp_offset_date_time")
	static class WithOffsetDateTime {
		@Id
		Integer id;
		OffsetDateTime theValue;
	}

	@Entity(name = "WithZonedDateTime")
	@Table(name = "tp_zoned_date_time")
	static class WithZonedDateTime {
		@Id
		Integer id;
		ZonedDateTime theValue;
	}
}

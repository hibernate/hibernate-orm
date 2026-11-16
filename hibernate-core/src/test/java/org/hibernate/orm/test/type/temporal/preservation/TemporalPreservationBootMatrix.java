package org.hibernate.orm.test.type.temporal.preservation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.type.spi.TemporalPreservation;
import org.hibernate.dialect.type.spi.TemporalPreservation.Level;
import org.hibernate.dialect.type.spi.TemporalPreservation.Part;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Column;
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
	/// @param javaClass the simple name of the Java Time class
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
			return String.join(
					",",
					dialect,
					javaClass,
					jdbcType,
					columnType,
					preservedParts,
					instantBasis,
					minimum,
					maximum,
					outcome
			);
		}
	}

	static final String CSV_HEADER =
			"Dialect,Java class,JDBC type,Column type,Preserved parts,Instant basis,Minimum,Maximum,Outcome";

	/// Boot each Java Time class with the given settings applied on top of
	/// the current test profile.
	static List<Cell> boot(Map<String, Object> settings) {
		final List<Cell> cells = new ArrayList<>();
		for ( var javaClass : JAVA_TIME_CLASSES ) {
			cells.add( boot( javaClass, settings ) );
		}
		return cells;
	}

	static Cell boot(Class<?> javaClass, Map<String, Object> settings) {
		final var registryBuilder = ServiceRegistryUtil.serviceRegistryBuilder();
		settings.forEach( registryBuilder::applySetting );
		try ( var registry = registryBuilder.build() ) {
			final var metadata = (MetadataImplementor) new MetadataSources( registry )
					.addAnnotatedClass( ENTITIES.get( javaClass ) )
					.buildMetadata();
			final var dialect = metadata.getDatabase().getDialect();
			final var value = (BasicValue) metadata.getEntityBinding( ENTITIES.get( javaClass ).getName() )
					.getProperty( "theValue" )
					.getValue();
			final var resolution = value.resolve();
			final JavaType<?> javaType = resolution.getRelationalJavaType();
			final JdbcType jdbcType = resolution.getJdbcType();
			final String columnType = ( (Column) value.getColumn() ).getSqlType( metadata );
			final boolean enabled = TemporalPreservationSettings.resolve(
					registry.requireService( ConfigurationService.class ).getSettings()
			).preservationEnabled();

			final String outcome;
			try ( var sessionFactory = metadata.buildSessionFactory() ) {
				outcome = enabled ? "QUALIFIED" : "DISABLED";
			}
			catch (MappingException e) {
				if ( !enabled ) {
					throw e;
				}
				return cell( dialect.getClass(), javaClass, jdbcType, columnType, null, "REJECTED" );
			}
			return cell(
					dialect.getClass(),
					javaClass,
					jdbcType,
					columnType,
					enabled ? dialect.getTemporalPreservation( javaType, jdbcType ) : null,
					outcome
			);
		}
	}

	private static Cell cell(
			Class<?> dialectClass,
			Class<?> javaClass,
			JdbcType jdbcType,
			String columnType,
			TemporalPreservation preservation,
			String outcome) {
		final var parts = new StringJoiner( "+" );
		if ( preservation != null ) {
			final var required = Part.requiredBy( javaClass );
			for ( var part : Part.values() ) {
				if ( required.contains( part ) && preservation.level( part ) == Level.SUPPORTED ) {
					parts.add( part.name() );
				}
			}
		}
		return new Cell(
				dialectClass.getSimpleName(),
				javaClass.getSimpleName(),
				jdbcType.getClass().getSimpleName(),
				columnType,
				parts.toString(),
				preservation == null || preservation.instantBasis() == null
						? ""
						: preservation.instantBasis().name(),
				preservation == null || preservation.minimum() == null ? "" : preservation.minimum().toString(),
				preservation == null || preservation.maximum() == null ? "" : preservation.maximum().toString(),
				outcome
		);
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

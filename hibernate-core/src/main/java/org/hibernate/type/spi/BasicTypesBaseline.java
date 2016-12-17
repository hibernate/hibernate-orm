/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.hibernate.boot.model.type.spi.BasicTypeProducerRegistry;
import org.hibernate.type.AdaptedImmutableType;
import org.hibernate.type.BinaryType;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.DateType;
import org.hibernate.type.DbTimestampType;
import org.hibernate.type.SerializableType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.TimeType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.spi.basic.RegistryKey;

/**
 * Baseline set of {@link BasicType} implementations
 *
 * @author Steve Ebersole
 */
public class BasicTypesBaseline {
	public static void prime(TypeConfiguration typeConfiguration, BasicTypeProducerRegistry basicTypeProducerRegistry) {
		// clean up the calls to register by not having to pass TypeConfiguration and BasicTypeProducerRegistry
		final BasicTypesBaseline primer = new BasicTypesBaseline( typeConfiguration, basicTypeProducerRegistry );

		// boolean data
		primer.register( StandardBasicTypes.BOOLEAN, "boolean", boolean.class.getName(), Boolean.class.getName() );
		primer.register( StandardBasicTypes.NUMERIC_BOOLEAN, "numeric_boolean" );
		primer.register( StandardBasicTypes.TRUE_FALSE, "true_false" );
		primer.register( StandardBasicTypes.YES_NO, "yes_no" );

		// byte/binary data
		primer.register( StandardBasicTypes.BYTE, "byte", byte.class.getName(), Byte.class.getName() );
		primer.register( StandardBasicTypes.BINARY, "binary", "byte[]", byte[].class.getName() );
		primer.register( StandardBasicTypes.WRAPPER_BINARY, "wrapper-binary", "Byte[]", Byte[].class.getName() );
		primer.register( StandardBasicTypes.IMAGE, "image" );
		primer.register( StandardBasicTypes.BLOB, "blob", Blob.class.getName() );
		primer.register( StandardBasicTypes.MATERIALIZED_BLOB, "materialized_blob" );

		// Numeric data
		primer.register( StandardBasicTypes.SHORT, "short", short.class.getName(), Short.class.getName() );
		primer.register( StandardBasicTypes.INTEGER, "integer", int.class.getName(), Integer.class.getName() );
		primer.register( StandardBasicTypes.LONG, "long", long.class.getName(), Long.class.getName() );
		primer.register( StandardBasicTypes.FLOAT, "float", float.class.getName(), Float.class.getName() );
		primer.register( StandardBasicTypes.DOUBLE, "double", double.class.getName(), Double.class.getName() );
		primer.register( StandardBasicTypes.BIG_INTEGER, "big_integer", BigInteger.class.getName() );
		primer.register( StandardBasicTypes.BIG_DECIMAL, "big_decimal", BigDecimal.class.getName() );

		// String data
		primer.register( StandardBasicTypes.CHARACTER, "character", char.class.getName(), Character.class.getName() );
		primer.register( StandardBasicTypes.STRING, "string", String.class.getName() );
		primer.register( StandardBasicTypes.CHAR_ARRAY, "characters", "char[]", char[].class.getName() );
		primer.register( StandardBasicTypes.CHARACTER_ARRAY, "wrapper-characters", Character[].class.getName(), "Character[]" );
		primer.register( StandardBasicTypes.TEXT, "text" );
		primer.register( StandardBasicTypes.NTEXT, "ntext" );
		primer.register( StandardBasicTypes.CLOB, "clob", Clob.class.getName() );
		primer.register( StandardBasicTypes.NCLOB, "nclob", NClob.class.getName() );
		primer.register( StandardBasicTypes.MATERIALIZED_CLOB, "materialized_clob" );
		primer.register( StandardBasicTypes.MATERIALIZED_NCLOB, "materialized_nclob" );

		// date / time data
		primer.register( StandardBasicTypes.DATE, "date", java.sql.Date.class.getName() );
		primer.register( StandardBasicTypes.TIME, "time", java.sql.Time.class.getName() );
		primer.register( StandardBasicTypes.TIMESTAMP, "timestamp", java.sql.Timestamp.class.getName(), java.util.Date.class.getName() );
		primer.register( StandardBasicTypes.CALENDAR, "calendar", Calendar.class.getName(), GregorianCalendar.class.getName() );
		primer.register( StandardBasicTypes.CALENDAR_DATE, "calendar_date" );
		primer.register( StandardBasicTypes.DURATION, Duration.class.getSimpleName(), Duration.class.getName() );
		primer.register( StandardBasicTypes.LOCAL_DATE_TIME, LocalDateTime.class.getSimpleName(), LocalDateTime.class.getName() );
		primer.register( StandardBasicTypes.LOCAL_DATE, LocalDate.class.getSimpleName(), LocalDate.class.getName() );
		primer.register( StandardBasicTypes.LOCAL_TIME, LocalTime.class.getSimpleName(), LocalTime.class.getName() );
		primer.register( StandardBasicTypes.OFFSET_DATE_TIME, OffsetDateTime.class.getSimpleName(), OffsetDateTime.class.getName() );
		primer.register( StandardBasicTypes.OFFSET_TIME, OffsetTime.class.getSimpleName(), OffsetTime.class.getName() );
		primer.register( StandardBasicTypes.ZONED_DATE_TIME, ZonedDateTime.class.getSimpleName(), ZonedDateTime.class.getName() );
		primer.register( DbTimestampType.INSTANCE, "dbtimestamp" );

		// UUID data
		primer.register( StandardBasicTypes.UUID_BINARY, "uuid-binary", UUID.class.getName() );
		primer.register( StandardBasicTypes.UUID_CHAR, "uuid-char" );

		// Misc data
		primer.register( StandardBasicTypes.CLASS, "class", Class.class.getName() );
		primer.register( StandardBasicTypes.CURRENCY, "currency", Currency.class.getSimpleName(), Currency.class.getName() );
		primer.register( StandardBasicTypes.LOCALE, "locale", Locale.class.getName() );
		primer.register( StandardBasicTypes.SERIALIZABLE, "serializable", Serializable.class.getName() );
		primer.register( StandardBasicTypes.TIMEZONE, "timezone", TimeZone.class.getName() );
		primer.register( StandardBasicTypes.URL, "url", java.net.URL.class.getName() );

		// immutable variants of certain types
		primer.register( new AdaptedImmutableType( DateType.INSTANCE ), "imm_date" );
		primer.register( new AdaptedImmutableType( TimeType.INSTANCE ), "imm_time" );
		primer.register( new AdaptedImmutableType( TimestampType.INSTANCE ), "imm_timestamp" );
		primer.register( new AdaptedImmutableType( DbTimestampType.INSTANCE ), "imm_dbtimestamp" );
		primer.register( new AdaptedImmutableType( CalendarType.INSTANCE ), "imm_calendar" );
		primer.register( new AdaptedImmutableType( CalendarDateType.INSTANCE ), "imm_calendar_date" );
		primer.register( new AdaptedImmutableType( BinaryType.INSTANCE ), "imm_binary" );
		primer.register( new AdaptedImmutableType( SerializableType.INSTANCE ), "imm_serializable" );
	}

	private final TypeConfiguration typeConfiguration;
	private final BasicTypeProducerRegistry basicTypeProducerRegistry;

	private BasicTypesBaseline(
			TypeConfiguration typeConfiguration,
			BasicTypeProducerRegistry basicTypeProducerRegistry) {
		this.typeConfiguration = typeConfiguration;
		this.basicTypeProducerRegistry = basicTypeProducerRegistry;
	}

	private void register(BasicType basicType, String... registrationKeys) {
		typeConfiguration.getBasicTypeRegistry().register(
				basicType,
				RegistryKey.from( basicType )
		);

		basicTypeProducerRegistry.register(
				basicType,
				BasicTypeProducerRegistry.DuplicationStrategy.OVERWRITE,
				basicType.getClass().getName()
		);

		basicTypeProducerRegistry.register(
				basicType,
				BasicTypeProducerRegistry.DuplicationStrategy.OVERWRITE,
				registrationKeys
		);
	}
}

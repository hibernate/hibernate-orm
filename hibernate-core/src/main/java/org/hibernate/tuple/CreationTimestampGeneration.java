/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Value generation implementation for {@link CreationTimestamp}.
 *
 * @author Gunnar Morling
 */
public class CreationTimestampGeneration implements AnnotationValueGeneration<CreationTimestamp> {

	private ValueGenerator<?> generator;

	@Override
	public void initialize(CreationTimestamp annotation, Class<?> propertyType) {
		if ( java.sql.Date.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentSqlDateGenerator();
		}
		else if ( Time.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentSqlTimeGenerator();
		}
		else if ( Timestamp.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentSqlTimestampGenerator();
		}
		else if ( Date.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentDateGenerator();
		}
		else if ( Calendar.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentCalendarGenerator();
		}
		else if ( Instant.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentInstantGenerator();
		}
		else if ( LocalDate.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentLocalDateGenerator();
		}
		else if ( LocalDateTime.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentLocalDateTimeGenerator();
		}
		else if ( LocalTime.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentLocalTimeGenerator();
		}
		else if ( MonthDay.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentMonthDayGenerator();
		}
		else if ( OffsetDateTime.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentOffsetDateTimeGenerator();
		}
		else if ( OffsetTime.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentOffsetTimeGenerator();
		}
		else if ( Year.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentYearGenerator();
		}
		else if ( YearMonth.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentYearMonthGenerator();
		}
		else if ( ZonedDateTime.class.isAssignableFrom( propertyType ) ) {
			generator = new TimestampGenerators.CurrentZonedDateTimeGenerator();
		}
		else {
			throw new HibernateException( "Unsupported property type for generator annotation @CreationTimestamp" );
		}
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.INSERT;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return generator;
	}

	@Override
	public boolean referenceColumnInSql() {
		return false;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return null;
	}
}

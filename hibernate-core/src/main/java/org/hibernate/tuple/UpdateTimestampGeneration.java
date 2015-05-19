/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

import org.hibernate.HibernateException;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Value generation implementation for {@link UpdateTimestamp}.
 *
 * @author Gunnar Morling
 */
public class UpdateTimestampGeneration implements AnnotationValueGeneration<UpdateTimestamp> {

	private ValueGenerator<?> generator;

	@Override
	public void initialize(UpdateTimestamp annotation, Class<?> propertyType) {
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
		else {
			throw new HibernateException( "Unsupported property type for generator annotation @UpdateTimestamp" );
		}
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.ALWAYS;
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

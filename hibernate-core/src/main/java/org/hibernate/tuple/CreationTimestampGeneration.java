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
import org.hibernate.Session;
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
			generator = new CreationTimestampGeneratorForSqlDate();
		}
		else if ( Time.class.isAssignableFrom( propertyType ) ) {
			generator = new CreationTimestampGeneratorForSqlTime();
		}
		else if ( Timestamp.class.isAssignableFrom( propertyType ) ) {
			generator = new CreationTimestampGeneratorForSqlTimestamp();
		}
		else if ( Date.class.isAssignableFrom( propertyType ) ) {
			generator = new CreationTimestampGeneratorForDate();
		}
		else if ( Calendar.class.isAssignableFrom( propertyType ) ) {
			generator = new CreationTimestampGeneratorForCalendar();
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

	private static class CreationTimestampGeneratorForDate implements ValueGenerator<Date> {

		@Override
		public Date generateValue(Session session, Object owner) {
			return new Date();
		}
	}

	private static class CreationTimestampGeneratorForCalendar implements ValueGenerator<Calendar> {

		@Override
		public Calendar generateValue(Session session, Object owner) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime( new Date() );
			return calendar;
		}
	}

	private static class CreationTimestampGeneratorForSqlDate implements ValueGenerator<java.sql.Date> {

		@Override
		public java.sql.Date generateValue(Session session, Object owner) {
			return new java.sql.Date( new Date().getTime() );
		}
	}

	private static class CreationTimestampGeneratorForSqlTime implements ValueGenerator<Time> {

		@Override
		public Time generateValue(Session session, Object owner) {
			return new Time( new Date().getTime() );
		}
	}

	private static class CreationTimestampGeneratorForSqlTimestamp implements ValueGenerator<Timestamp> {

		@Override
		public Timestamp generateValue(Session session, Object owner) {
			return new Timestamp( new Date().getTime() );
		}
	}
}

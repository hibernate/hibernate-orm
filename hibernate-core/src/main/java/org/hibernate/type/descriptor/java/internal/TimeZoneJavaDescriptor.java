/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import java.util.Comparator;
import java.util.TimeZone;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Descriptor for {@link TimeZone} handling.
 *
 * @author Steve Ebersole
 */
public class TimeZoneJavaDescriptor extends AbstractBasicJavaDescriptor<TimeZone> {
	public static final TimeZoneJavaDescriptor INSTANCE = new TimeZoneJavaDescriptor();

	public static class TimeZoneComparator implements Comparator<TimeZone> {
		public static final TimeZoneComparator INSTANCE = new TimeZoneComparator();

		public int compare(TimeZone o1, TimeZone o2) {
			return o1.getID().compareTo( o2.getID() );
		}
	}

	public TimeZoneJavaDescriptor() {
		super( TimeZone.class );
	}

	public String toString(TimeZone value) {
		return value.getID();
	}

	public TimeZone fromString(String string) {
		return TimeZone.getTimeZone( string );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		return StringJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	@Override
	public Comparator<TimeZone> getComparator() {
		return TimeZoneComparator.INSTANCE;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(TimeZone value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> TimeZone wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}

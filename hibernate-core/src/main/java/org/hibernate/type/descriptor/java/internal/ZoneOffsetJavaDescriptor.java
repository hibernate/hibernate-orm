/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.SqlTypeDescriptorIndicators;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

import java.time.ZoneOffset;
import java.util.Comparator;

/**
 * Descriptor for {@link ZoneOffset} handling.
 *
 * @author Gavin King
 */
public class ZoneOffsetJavaDescriptor extends AbstractBasicJavaDescriptor<ZoneOffset> {
	public static final ZoneOffsetJavaDescriptor INSTANCE = new ZoneOffsetJavaDescriptor();

	public static class ZoneOffsetComparator implements Comparator<ZoneOffset> {
		public static final ZoneOffsetComparator INSTANCE = new ZoneOffsetComparator();

		public int compare(ZoneOffset o1, ZoneOffset o2) {
			return o1.getId().compareTo( o2.getId() );
		}
	}

	public ZoneOffsetJavaDescriptor() {
		super( ZoneOffset.class );
	}

	//TODO: set the default column length to 6

	public String toString(ZoneOffset value) {
		return value.getId();
	}

	public ZoneOffset fromString(String string) {
		return ZoneOffset.of( string );
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(SqlTypeDescriptorIndicators context) {
		return StringJavaDescriptor.INSTANCE.getJdbcRecommendedSqlType( context );
	}

	@Override
	public Comparator<ZoneOffset> getComparator() {
		return ZoneOffsetComparator.INSTANCE;
	}

	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(ZoneOffset value, Class<X> type, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	public <X> ZoneOffset wrap(X value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isInstance( value ) ) {
			return fromString( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}

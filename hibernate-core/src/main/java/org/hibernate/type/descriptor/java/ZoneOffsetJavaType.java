/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import java.time.ZoneOffset;
import java.util.Comparator;

/**
 * Descriptor for {@link ZoneOffset} handling.
 *
 * @author Gavin King
 */
public class ZoneOffsetJavaType extends AbstractClassJavaType<ZoneOffset> {
	public static final ZoneOffsetJavaType INSTANCE = new ZoneOffsetJavaType();

	public static class ZoneOffsetComparator implements Comparator<ZoneOffset> {
		public static final ZoneOffsetComparator INSTANCE = new ZoneOffsetComparator();

		public int compare(ZoneOffset o1, ZoneOffset o2) {
			return o1.getId().compareTo( o2.getId() );
		}
	}

	public ZoneOffsetJavaType() {
		super( ZoneOffset.class, ImmutableMutabilityPlan.instance(), ZoneOffsetComparator.INSTANCE );
	}

	public String toString(ZoneOffset value) {
		return value.getId();
	}

	public ZoneOffset fromString(CharSequence string) {
		return ZoneOffset.of( string.toString() );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return StringJavaType.INSTANCE.getRecommendedJdbcType( context );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <X> X unwrap(ZoneOffset value, Class<X> type, WrapperOptions wrapperOptions) {
		if ( value == null ) {
			return null;
		}
		if ( ZoneOffset.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) Integer.valueOf( value.getTotalSeconds() );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZoneOffset wrap(X value, WrapperOptions wrapperOptions) {
		if ( value == null ) {
			return null;
		}
		if ( value instanceof ZoneOffset ) {
			return (ZoneOffset) value;
		}
		if ( value instanceof CharSequence ) {
			return fromString( (CharSequence) value );
		}
		if ( value instanceof Integer ) {
			return ZoneOffset.ofTotalSeconds( (Integer) value );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 6;
	}
}

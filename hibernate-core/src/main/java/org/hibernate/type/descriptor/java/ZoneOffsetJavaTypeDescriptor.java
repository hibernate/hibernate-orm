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
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

import java.time.ZoneOffset;
import java.util.Comparator;

/**
 * Descriptor for {@link ZoneOffset} handling.
 *
 * @author Gavin King
 */
public class ZoneOffsetJavaTypeDescriptor extends AbstractClassJavaTypeDescriptor<ZoneOffset> {
	public static final ZoneOffsetJavaTypeDescriptor INSTANCE = new ZoneOffsetJavaTypeDescriptor();

	public static class ZoneOffsetComparator implements Comparator<ZoneOffset> {
		public static final ZoneOffsetComparator INSTANCE = new ZoneOffsetComparator();

		public int compare(ZoneOffset o1, ZoneOffset o2) {
			return o1.getId().compareTo( o2.getId() );
		}
	}

	public ZoneOffsetJavaTypeDescriptor() {
		super( ZoneOffset.class, ImmutableMutabilityPlan.INSTANCE, ZoneOffsetComparator.INSTANCE );
	}

	public String toString(ZoneOffset value) {
		return value.getId();
	}

	public ZoneOffset fromString(CharSequence string) {
		return ZoneOffset.of( string.toString() );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeDescriptorIndicators context) {
		return StringJavaTypeDescriptor.INSTANCE.getRecommendedJdbcType( context );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public <X> X unwrap(ZoneOffset value, Class<X> type, WrapperOptions wrapperOptions) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) toString( value );
		}
		throw unknownUnwrap( type );
	}

	@Override
	public <X> ZoneOffset wrap(X value, WrapperOptions wrapperOptions) {
		if ( value == null ) {
			return null;
		}
		if ( CharSequence.class.isInstance( value ) ) {
			return fromString( (CharSequence) value );
		}
		throw unknownWrap( value.getClass() );
	}

	@Override
	public long getDefaultSqlLength(Dialect dialect, JdbcType jdbcType) {
		return 6;
	}
}

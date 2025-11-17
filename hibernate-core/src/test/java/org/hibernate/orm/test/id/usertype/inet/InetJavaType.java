/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.inet;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * @author Vlad Mihalcea
 */
public class InetJavaType extends AbstractClassJavaType<Inet> {

	public static final InetJavaType INSTANCE = new InetJavaType();

	public InetJavaType() {
		super( Inet.class );
	}

	public String toString(Inet value) {
		return value.toString();
	}

	public Inet fromString(CharSequence string) {
		return new Inet( string.toString() );
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		return indicators.getJdbcType( SqlTypes.INET );
	}

	@Override
	public boolean areEqual(Inet one, Inet another) {
		return one == another || ( one != null && another != null && one.equals( another ) );
	}

	@Override
	public int extractHashCode(Inet value) {
		return value.hashCode();
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Inet value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Inet.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	public <X> Inet wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Inet.class.isInstance( value ) ) {
			return (Inet) value;
		}
		if ( String.class.isInstance( value ) ) {
			return new Inet( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}

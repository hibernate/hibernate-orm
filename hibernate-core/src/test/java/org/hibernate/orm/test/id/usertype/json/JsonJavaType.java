/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.usertype.json;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;

/**
 * @author Vlad Mihalcea
 */
public class JsonJavaType extends AbstractClassJavaType<Json> {

	public static final JsonJavaType INSTANCE = new JsonJavaType();

	public JsonJavaType() {
		super( Json.class );
	}

	public String toString(Json value) {
		return value.toString();
	}

	public Json fromString(CharSequence string) {
		return new Json( string.toString() );
	}

	@Override
	public boolean areEqual(Json one, Json another) {
		return one == another || ( one != null && another != null && one.equals( another ) );
	}

	@Override
	public int extractHashCode(Json value) {
		return value.hashCode();
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(Json value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Json.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value.toString();
		}
		throw unknownUnwrap( type );
	}

	public <X> Json wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( Json.class.isInstance( value ) ) {
			return (Json) value;
		}
		if ( String.class.isInstance( value ) ) {
			return new Json( (String) value );
		}
		throw unknownWrap( value.getClass() );
	}
}

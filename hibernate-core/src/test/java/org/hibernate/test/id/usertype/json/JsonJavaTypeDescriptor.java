/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.usertype.json;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;

/**
 * @author Vlad Mihalcea
 */
public class JsonJavaTypeDescriptor extends AbstractTypeDescriptor<Json> {

	public static final JsonJavaTypeDescriptor INSTANCE = new JsonJavaTypeDescriptor();

	public JsonJavaTypeDescriptor() {
		super( Json.class );
	}

	public String toString(Json value) {
		return value.toString();
	}

	public Json fromString(String string) {
		return new Json( string );
	}

	@Override
	public boolean areEqual(Json one, Json another) {
		return one == another || ( one != null && another != null && one.equals( another ) );
	}

	@Override
	public int extractHashCode(Json value) {
		return value.hashCode();
	}

	@SuppressWarnings({ "unchecked" })
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

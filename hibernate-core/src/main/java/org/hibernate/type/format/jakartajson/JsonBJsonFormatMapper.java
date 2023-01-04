/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.format.jakartajson;

import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;

/**
 * @author Christian Beikov
 */
public final class JsonBJsonFormatMapper implements FormatMapper {

	public static final String SHORT_NAME = "jsonb";

	private final Jsonb jsonb;

	public JsonBJsonFormatMapper() {
		this( JsonbBuilder.create() );
	}

	public JsonBJsonFormatMapper(Jsonb jsonb) {
		this.jsonb = jsonb;
	}

	@Override
	public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (T) charSequence.toString();
		}
		try {
			return jsonb.fromJson( charSequence.toString(), javaType.getJavaType() );
		}
		catch (JsonbException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + javaType, e );
		}
	}

	@Override
	public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (String) value;
		}
		try {
			return jsonb.toJson( value, javaType.getJavaType() );
		}
		catch (JsonbException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType, e );
		}
	}
}

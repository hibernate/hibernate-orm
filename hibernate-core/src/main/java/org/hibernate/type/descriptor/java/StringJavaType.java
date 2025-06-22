/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Reader;
import java.io.StringReader;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Types;

import org.hibernate.engine.jdbc.CharacterStream;
import org.hibernate.engine.jdbc.internal.CharacterStreamImpl;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Descriptor for {@link String} handling.
 *
 * @author Steve Ebersole
 */
public class StringJavaType extends AbstractClassJavaType<String> {
	public static final StringJavaType INSTANCE = new StringJavaType();

	public StringJavaType() {
		super( String.class );
	}

	@Override
	public boolean useObjectEqualsHashCode() {
		return true;
	}

	public String toString(String value) {
		return value;
	}

	public String fromString(CharSequence string) {
		return string.toString();
	}

	@Override
	public boolean isInstance(Object value) {
		return value instanceof String;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators stdIndicators) {
		final TypeConfiguration typeConfiguration = stdIndicators.getTypeConfiguration();
		final JdbcTypeRegistry stdRegistry = typeConfiguration.getJdbcTypeRegistry();

		if ( stdIndicators.isLob() ) {
			return stdIndicators.isNationalized()
					? stdRegistry.getDescriptor( Types.NCLOB )
					: stdRegistry.getDescriptor( Types.CLOB );
		}
		else if ( stdIndicators.isNationalized() ) {
			return stdRegistry.getDescriptor( Types.NVARCHAR );
		}

		return super.getRecommendedJdbcType( stdIndicators );
	}

	@SuppressWarnings("unchecked")
	public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if ( String.class.isAssignableFrom( type ) ) {
			return (X) value;
		}
		if ( byte[].class.isAssignableFrom( type ) ) {
			return (X) value.getBytes( UTF_8 );
		}
		if ( Reader.class.isAssignableFrom( type ) ) {
			return (X) new StringReader( value );
		}
		if ( CharacterStream.class.isAssignableFrom( type ) ) {
			return (X) new CharacterStreamImpl( value );
		}
		// Since NClob extends Clob, we need to check if type is an NClob
		// before checking if type is a Clob. That will ensure that
		// the correct type is returned.
		if ( NClob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createNClob( value );
		}
		if ( Clob.class.isAssignableFrom( type ) ) {
			return (X) options.getLobCreator().createClob( value );
		}
		if ( Integer.class.isAssignableFrom( type ) ) {
			return (X) (Integer) Integer.parseInt( value );
		}
		if ( Long.class.isAssignableFrom( type ) ) {
			return (X) (Long) Long.parseLong( value );
		}

		throw unknownUnwrap( type );
	}

	public <X> String wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		if (value instanceof String string) {
			return string;
		}
		if (value instanceof char[] chars) {
			return new String( chars );
		}
		if (value instanceof byte[] bytes) {
			return new String( bytes, UTF_8 );
		}
		if (value instanceof Reader reader) {
			return DataHelper.extractString( reader );
		}
		if (value instanceof Clob clob) {
			return DataHelper.extractString( clob );
		}
		if (value instanceof Integer) {
			return value.toString();
		}
		if (value instanceof Long) {
			return value.toString();
		}

		throw unknownWrap( value.getClass() );
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		return switch ( javaType.getTypeName() ) {
			case
				"char",
				"char[]",
				"java.lang.Character",
				"java.lang.Character[]" -> true;
			default -> false;
		};
	}

	@Override
	public <X> String coerce(X value, CoercionContext coercionContext) {
		return wrap( value, null );
	}
}

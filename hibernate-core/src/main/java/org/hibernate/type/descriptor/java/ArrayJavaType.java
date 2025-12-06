/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Collection;

import org.hibernate.HibernateException;
import org.hibernate.SharedSessionContract;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.ArrayBackedBinaryStream;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.reflect.Array.newInstance;
import static org.hibernate.internal.util.ReflectHelper.arrayClass;

/**
 * Descriptor for {@code T[]} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
@AllowReflection
public class ArrayJavaType<T> extends AbstractArrayJavaType<T[], T> {

	public ArrayJavaType(BasicType<T> baseDescriptor) {
		this( baseDescriptor.getJavaTypeDescriptor() );
	}

	public ArrayJavaType(JavaType<T> baseDescriptor) {
		super( arrayClass( baseDescriptor.getJavaTypeClass() ),
				baseDescriptor,
				new ArrayMutabilityPlan<>( baseDescriptor ) );
	}

	@Override
	public BasicType<?> resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<T> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators stdIndicators) {
		if ( stdIndicators.isLob() ) {
			final Class<?> javaTypeClass = getJavaTypeClass();
			if ( javaTypeClass == Byte[].class ) {
				return typeConfiguration.getBasicTypeRegistry().resolve(
						ByteArrayJavaType.INSTANCE,
						ByteArrayJavaType.INSTANCE.getRecommendedJdbcType( stdIndicators )
				);
			}
			if ( javaTypeClass == Character[].class ) {
				return typeConfiguration.getBasicTypeRegistry().resolve(
						CharacterArrayJavaType.INSTANCE,
						CharacterArrayJavaType.INSTANCE.getRecommendedJdbcType( stdIndicators )
				);
			}
		}
		final var elementTypeJavaType = elementType.getJavaTypeDescriptor();
		final var elementJavaTypeClass = elementTypeJavaType.getJavaTypeClass();
		if ( elementType instanceof BasicPluralType<?, ?>
				|| elementJavaTypeClass != null
					&& elementJavaTypeClass.isArray()
					&& elementJavaTypeClass != byte[].class ) {
			// No support for nested arrays, except for byte[][]
			return null;
		}
		final ArrayJavaType<T> arrayJavaType;
		if ( getElementJavaType() == elementTypeJavaType ) {
			arrayJavaType = this;
		}
		else {
			arrayJavaType = new ArrayJavaType<>( elementTypeJavaType );
			// Register the array type as that will be resolved in the next step
			typeConfiguration.getJavaTypeRegistry().addDescriptor( arrayJavaType );
		}
		final var valueConverter = elementType.getValueConverter();
		return valueConverter == null
				? resolveType( typeConfiguration, arrayJavaType, elementType, columnTypeInformation, stdIndicators )
				: createTypeUsingConverter( typeConfiguration, elementType, columnTypeInformation, stdIndicators, valueConverter );
	}

	@Override
	public String extractLoggableRepresentation(T[] value) {
		if ( value == null ) {
			return "null";
		}
		int iMax = value.length - 1;
		if ( iMax == -1 ) {
			return "[]";
		}
		final var sb = new StringBuilder();
		sb.append( '[' );
		for ( int i = 0; ; i++ ) {
			sb.append( getElementJavaType().extractLoggableRepresentation( value[i] ) );
			if ( i == iMax ) {
				return sb.append( ']' ).toString();
			}
			sb.append( ", " );
		}
	}

	@Override
	public boolean areEqual(T[] one, T[] another) {
		if ( one == null && another == null ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}
		if ( one.length != another.length ) {
			return false;
		}
		int l = one.length;
		for ( int i = 0; i < l; i++ ) {
			if ( !getElementJavaType().areEqual( one[i], another[i] )) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int extractHashCode(T[] value) {
		if ( value == null ) {
			return 0;
		}
		int result = 1;
		for ( T element : value ) {
			result = 31 * result + ( element == null ? 0 : getElementJavaType().extractHashCode( element ) );
		}
		return result;
	}

	@Override
	public String toString(T[] value) {
		if ( value == null ) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		sb.append( '{' );
		String glue = "";
		for ( T v : value ) {
			sb.append( glue );
			if ( v == null ) {
				sb.append( "null" );
				glue = ",";
				continue;
			}
			sb.append( '"' );
			final String valstr = getElementJavaType().toString( v );
			// using replaceAll is a shorter, but much slower way to do this
			for (int i = 0, len = valstr.length(); i < len; i ++ ) {
				char c = valstr.charAt( i );
				// Surrogate pairs. This is how they're done.
				if (c == '\\' || c == '"') {
					sb.append( '\\' );
				}
				sb.append( c );
			}
			sb.append( '"' );
			glue = ",";
		}
		sb.append( '}' );
		return sb.toString();
	}

	@Override
	public T[] fromString(CharSequence charSequence) {
		if ( charSequence == null ) {
			return null;
		}
		final var lst = new java.util.ArrayList<String>();
		StringBuilder sb = null;
		char lastChar = charSequence.charAt( charSequence.length() - 1 );
		char firstChar = charSequence.charAt( 0 );
		if ( firstChar != '{' || lastChar != '}' ) {
			throw new IllegalArgumentException( "Cannot parse given string into array of strings. First and last character must be { and }" );
		}
		int len = charSequence.length();
		boolean inquote = false;
		for ( int i = 1; i < len; i ++ ) {
			char c = charSequence.charAt( i );
			if ( c == '"' ) {
				if (inquote) {
					lst.add( sb.toString() );
				}
				else {
					sb = new StringBuilder();
				}
				inquote = !inquote;
				continue;
			}
			else if ( !inquote ) {
				if ( Character.isWhitespace( c ) ) {
					continue;
				}
				else if ( c == ',' ) {
					// treat no-value between commas to mean null
					if ( sb == null ) {
						lst.add( null );
					}
					else {
						sb = null;
					}
					continue;
				}
				else {
					// i + 4, because there has to be a comma or closing brace after null
					if ( i + 4 < len
							&& charSequence.charAt( i ) == 'n'
							&& charSequence.charAt( i + 1 ) == 'u'
							&& charSequence.charAt( i + 2 ) == 'l'
							&& charSequence.charAt( i + 3 ) == 'l') {
						lst.add( null );
						i += 4;
						continue;
					}
					if (i + 1 == len) {
						break;
					}
					throw new IllegalArgumentException( "Cannot parse given string into array of strings."
														+ " Outside of quote, but neither whitespace, comma, array end, nor null found." );
				}
			}
			else if ( c == '\\' && i + 2 < len && (charSequence.charAt( i + 1 ) == '\\' || charSequence.charAt( i + 1 ) == '"')) {
				c = charSequence.charAt( ++i );
			}
			// If there is ever a null-pointer here, the if-else logic before is incomplete
			sb.append( c );
		}
		//noinspection unchecked
		final var result = (T[]) newInstance( getElementJavaType().getJavaTypeClass(), lst.size() );
		for ( int i = 0; i < result.length; i ++ ) {
			if ( lst.get( i ) != null ) {
				result[i] = getElementJavaType().fromString( lst.get( i ) );
			}
		}
		return result;
	}

	@Override
	public <X> X unwrap(T[] value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			return type.cast( value );
		}
		else if ( type == byte[].class ) {
			return type.cast( toBytes( value ) );
		}
		else if ( type == BinaryStream.class ) {
			return type.cast( new ArrayBackedBinaryStream( toBytes( value ) ) );
		}
		else if ( type.isArray() ) {
		final var preferredJavaTypeClass = type.getComponentType();
			final var unwrapped = (Object[]) newInstance( preferredJavaTypeClass, value.length );
			for ( int i = 0; i < value.length; i++ ) {
				unwrapped[i] = getElementJavaType().unwrap( value[i], preferredJavaTypeClass, options );
			}
			return type.cast( unwrapped );
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> T[] wrap(X value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( value instanceof java.sql.Array array ) {
			try {
				//noinspection unchecked
				value = (X) array.getArray();
			}
			catch ( SQLException ex ) {
				// This basically shouldn't happen unless you've lost connection to the database.
				throw new HibernateException( ex );
			}
		}

		final var elementJavaType = getElementJavaType();
		if ( value instanceof Object[] raw ) {
			final var componentClass = elementJavaType.getJavaTypeClass();
			//noinspection unchecked
			final var wrapped = (T[]) newInstance( componentClass, raw.length );
			if ( componentClass.isAssignableFrom( value.getClass().getComponentType() ) ) {
				for (int i = 0; i < raw.length; i++) {
					//noinspection unchecked
					wrapped[i] = (T) raw[i];
				}
			}
			else {
				for ( int i = 0; i < raw.length; i++ ) {
					wrapped[i] = elementJavaType.wrap( raw[i], options );
				}
			}
			return wrapped;
		}
		else if ( value instanceof byte[] bytes ) {
			return fromBytes( bytes );
		}
		else if ( value instanceof BinaryStream binaryStream ) {
			// When the value is a BinaryStream, this is a deserialization request
			return fromBytes( binaryStream.getBytes() );
		}
		else if ( elementJavaType.isInstance( value ) ) {
			// Support binding a single element as parameter value
			//noinspection unchecked
			final var wrapped = (T[]) newInstance( elementJavaType.getJavaTypeClass(), 1 );
			//noinspection unchecked
			wrapped[0] = (T) value;
			return wrapped;
		}
		else if ( value instanceof Collection<?> collection ) {
			//noinspection unchecked
			final var wrapped = (T[]) newInstance( elementJavaType.getJavaTypeClass(), collection.size() );
			int i = 0;
			for ( Object e : collection ) {
				wrapped[i++] = elementJavaType.wrap( e, options );
			}
			return wrapped;
		}

		throw unknownWrap( value.getClass() );
	}

	private static <T> byte[] toBytes(T[] array) {
		if ( array.getClass().getComponentType().isEnum() ) {
			final byte[] bytes = new byte[array.length];
			for (int i = 0; i < array.length; i++ ) {
				final T value = array[i];
				// encode null enum value as -1
				bytes[i] = value == null ? -1 : (byte) ((Enum<?>) value).ordinal();
			}
			return bytes;

		}
		else {
			// byte[] can only be requested if the value should be serialized
			return SerializationHelper.serialize( array );
		}
	}

	private T[] fromBytes(byte[] bytes) {
		final var elementClass = getElementJavaType().getJavaTypeClass();
		if ( elementClass.isEnum() ) {
			final T[] enumConstants = elementClass.getEnumConstants();
			final var array = (Object[]) newInstance( elementClass, bytes.length );
			for (int i = 0; i < bytes.length; i++ ) {
				// null enum value was encoded as -1
				array[i] = bytes[i] == -1 ? null : enumConstants[bytes[i]];
			}
			//noinspection unchecked
			return (T[]) array;

		}
		else {
			// When the value is a byte[], this is a deserialization request
			//noinspection unchecked
			return (T[]) SerializationHelper.deserialize(bytes);
		}
	}

	@AllowReflection
	private static class ArrayMutabilityPlan<T> implements MutabilityPlan<T[]> {

		private final Class<T> componentClass;
		private final MutabilityPlan<T> componentPlan;

		public ArrayMutabilityPlan(JavaType<T> baseDescriptor) {
			this.componentClass = baseDescriptor.getJavaTypeClass();
			this.componentPlan = baseDescriptor.getMutabilityPlan();
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public T[] deepCopy(T[] value) {
			if ( value == null ) {
				return null;
			}
			//noinspection unchecked
			final T[] copy = (T[]) newInstance( componentClass, value.length );
			for ( int i = 0; i < value.length; i ++ ) {
				copy[ i ] = componentPlan.deepCopy( value[ i ] );
			}
			return copy;
		}

		@Override
		public Serializable disassemble(T[] value, SharedSessionContract session) {
			return deepCopy( value );
		}

		@Override
		public T[] assemble(Serializable cached, SharedSessionContract session) {
			//noinspection unchecked
			return deepCopy( (T[]) cached );
		}

	}
}

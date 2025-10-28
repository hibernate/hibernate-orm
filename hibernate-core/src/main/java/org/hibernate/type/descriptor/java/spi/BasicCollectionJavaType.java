/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.MappingException;
import org.hibernate.SharedSessionContract;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.BinaryStream;
import org.hibernate.engine.jdbc.internal.ArrayBackedBinaryStream;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicCollectionType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.ConvertedBasicCollectionType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.internal.CollectionConverter;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.AbstractJavaType;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.spi.TypeConfiguration;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static java.lang.reflect.Array.newInstance;
import static java.lang.reflect.Array.set;


/**
 * Descriptor for handling persistent collections.
 *
 * @author Christian Beikov
 */
@Incubating
@AllowReflection // Needed for arbitrary array wrapping/unwrapping
public class BasicCollectionJavaType<C extends Collection<E>, E>
		extends AbstractJavaType<C>
		implements BasicPluralJavaType<E> {

	private final CollectionSemantics<C, E> semantics;
	private final JavaType<E> componentJavaType;

	public BasicCollectionJavaType(ParameterizedType type, JavaType<E> componentJavaType, CollectionSemantics<C, E> semantics) {
		super( type, new CollectionMutabilityPlan<>( componentJavaType, semantics ) );
		this.semantics = semantics;
		this.componentJavaType = componentJavaType;
	}

	@Override
	public JavaType<E> getElementJavaType() {
		return componentJavaType;
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		if ( componentJavaType instanceof UnknownBasicJavaType ) {
			throw new MappingException("Basic collection has element type '"
					+ componentJavaType.getTypeName()
					+ "' which is not a known basic type"
					+ " (attribute is not annotated '@ElementCollection', '@OneToMany', or '@ManyToMany')");
		}
		// Always determine the recommended type to make sure this is a valid basic java type
		// (even though we only use this inside the if block, we want it to throw here if something wrong)
		final var recommendedComponentJdbcType = componentJavaType.getRecommendedJdbcType( indicators );
		final var typeConfiguration = indicators.getTypeConfiguration();
		return typeConfiguration.getJdbcTypeRegistry()
				.resolveTypeConstructorDescriptor(
						indicators.getPreferredSqlTypeCodeForArray( recommendedComponentJdbcType.getDefaultSqlTypeCode() ),
						typeConfiguration.getBasicTypeRegistry().resolve( componentJavaType, recommendedComponentJdbcType ),
						ColumnTypeInformation.EMPTY
				);
	}

	public CollectionSemantics<C, E> getSemantics() {
		return semantics;
	}

	@Override
	public boolean isWider(JavaType<?> javaType) {
		// Support binding single element value
		return this == javaType || componentJavaType == javaType;
	}

	@Override
	public BasicType<?> resolveType(
			TypeConfiguration typeConfiguration,
			Dialect dialect,
			BasicType<E> elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeIndicators stdIndicators) {
		final var elementJavaType = elementType.getJavaTypeDescriptor();
		final var elementJavaTypeClass = elementJavaType.getJavaTypeClass();
		if ( elementType instanceof BasicPluralType<?, ?>
				|| elementJavaTypeClass != null && elementJavaTypeClass.isArray() ) {
			return null;
		}
		final var collectionJavaType = collectionJavaType( typeConfiguration, elementJavaType );
		final int elementSqlTypeCode = elementType.getJdbcType().getDefaultSqlTypeCode();
		final int arrayTypeCode = stdIndicators.getPreferredSqlTypeCodeForArray( elementSqlTypeCode );
		final var arrayJdbcType =
				typeConfiguration.getJdbcTypeRegistry()
						.resolveTypeConstructorDescriptor( arrayTypeCode, elementType, columnTypeInformation );
		final var valueConverter = elementType.getValueConverter();
		if ( valueConverter == null ) {
			return typeConfiguration.getBasicTypeRegistry()
					.resolve( collectionJavaType, arrayJdbcType,
							() -> new BasicCollectionType<>( elementType, arrayJdbcType, collectionJavaType ) );
		}
		else {
			return convertedCollectionType(
					typeConfiguration,
					elementType,
					arrayJdbcType,
					collectionJavaType,
					valueConverter
			);
		}
	}

	private static <C extends Collection<E>, E, R> ConvertedBasicCollectionType<C, E> convertedCollectionType(
			TypeConfiguration typeConfiguration,
			BasicType<E> elementType,
			JdbcType arrayJdbcType,
			BasicCollectionJavaType<C, E> collectionJavaType,
			BasicValueConverter<E, R> elementValueConverter) {
		final var elementRelationalJavaType = elementValueConverter.getRelationalJavaType();
		final var relationalJavaType =
				typeConfiguration.getJavaTypeRegistry()
						.resolveArrayDescriptor( elementRelationalJavaType.getJavaTypeClass() );
		return new ConvertedBasicCollectionType<>(
				elementType,
				arrayJdbcType,
				collectionJavaType,
				new CollectionConverter<>( elementValueConverter, collectionJavaType, relationalJavaType )
		);
	}

	private BasicCollectionJavaType<C, E> collectionJavaType(
			TypeConfiguration typeConfiguration,
			JavaType<E> elementJavaType) {
		if ( componentJavaType == elementJavaType ) {
			return this;
		}
		else {
			final var parameterizedType = (ParameterizedType) getJavaType();
			final var collectionJavaType =
					new BasicCollectionJavaType<>( parameterizedType, elementJavaType, semantics );
			// Register the collection type as that will be resolved in the next step
			typeConfiguration.getJavaTypeRegistry().addDescriptor( collectionJavaType );
			return collectionJavaType;
		}
	}

	@Override
	public String extractLoggableRepresentation(C value) {
		if ( value == null ) {
			return "null";
		}
		final var iterator = value.iterator();
		if ( !iterator.hasNext() ) {
			return "[]";
		}
		final var string = new StringBuilder();
		string.append( '[' );
		do {
			final E element = iterator.next();
			string.append( componentJavaType.extractLoggableRepresentation( element ) );
			if ( !iterator.hasNext() ) {
				return string.append( ']' ).toString();
			}
			string.append( ", " );
		} while ( true );
	}

	@Override
	public boolean areEqual(C one, C another) {
		if ( one == null && another == null ) {
			return true;
		}
		if ( one == null || another == null ) {
			return false;
		}
		if ( one.size() != another.size() ) {
			return false;
		}
		switch ( semantics.getCollectionClassification() ) {
			case ARRAY:
			case LIST:
			case ORDERED_SET:
			case SORTED_SET:
				final var iterator1 = one.iterator();
				final var iterator2 = another.iterator();
				while ( iterator1.hasNext() ) {
					if ( !componentJavaType.areEqual( iterator1.next(), iterator2.next() ) ) {
						return false;
					}
				}
			default: {
				OUTER: for ( E e1 : one ) {
					for ( E e2 : another ) {
						if ( componentJavaType.areEqual( e1, e2 ) ) {
							continue OUTER;
						}
					}
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public int extractHashCode(C value) {
		int result = 0;
		if ( value != null && !value.isEmpty() ) {
			for ( E element : value ) {
				if ( element != null ) {
					result += componentJavaType.extractHashCode( element );
				}
			}
		}
		return result;
	}

	@Override
	public String toString(C value) {
		if ( value == null ) {
			return null;
		}
		final var string = new StringBuilder();
		string.append( '{' );
		String glue = "";
		for ( E v : value ) {
			string.append( glue );
			if ( v == null ) {
				string.append( "null" );
				glue = ",";
				continue;
			}
			string.append( '"' );
			String valstr = componentJavaType.toString( v );
			// using replaceAll is a shorter, but much slower way to do this
			for (int i = 0, len = valstr.length(); i < len; i ++ ) {
				char c = valstr.charAt( i );
				// Surrogate pairs. This is how they're done.
				if (c == '\\' || c == '"') {
					string.append( '\\' );
				}
				string.append( c );
			}
			string.append( '"' );
			glue = ",";
		}
		string.append( '}' );
		return string.toString();
	}

	@Override
	public C fromString(CharSequence charSequence) {
		if ( charSequence == null ) {
			return null;
		}
		java.util.ArrayList<String> list = new java.util.ArrayList<>();
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
					list.add( sb.toString() );
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
						list.add( null );
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
						list.add( null );
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
			else if ( c == '\\' && i + 2 < len && (charSequence.charAt( i + 1 ) == '\\'
					|| charSequence.charAt( i + 1 ) == '"') ) {
				c = charSequence.charAt( ++i );
			}
			// If there is ever a null-pointer here, the if-else logic before is incomplete
			sb.append( c );
		}
		final C result = semantics.instantiateRaw( list.size(), null );
		for ( int i = 0; i < list.size(); i ++ ) {
			if ( list.get( i ) != null ) {
				result.add( componentJavaType.fromString( list.get( i ) ) );
			}
		}
		return result;
	}

	@Override
	public <X> X unwrap(C value, Class<X> type, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}

		if ( type.isInstance( value ) ) {
			//noinspection unchecked
			return (X) value;
		}
		else if ( type == byte[].class ) {
			// byte[] can only be requested if the value should be serialized
			return (X) SerializationHelper.serialize( asArrayList( value ) );
		}
		else if ( type == BinaryStream.class ) {
			// BinaryStream can only be requested if the value should be serialized
			//noinspection unchecked
			return (X) new ArrayBackedBinaryStream( SerializationHelper.serialize( asArrayList( value ) ) );
		}
		else if ( type == Object[].class ) {
			//noinspection unchecked
			return (X) value.toArray();
		}
		else if ( Object[].class.isAssignableFrom( type ) ) {
			final var preferredJavaTypeClass = type.getComponentType();
			final Object[] unwrapped = (Object[]) newInstance( preferredJavaTypeClass, value.size() );
			int i = 0;
			for ( E element : value ) {
				unwrapped[i] = componentJavaType.unwrap( element, preferredJavaTypeClass, options );
				i++;
			}
			//noinspection unchecked
			return (X) unwrapped;
		}
		else if ( type.isArray() ) {
			final var preferredJavaTypeClass = type.getComponentType();
			//noinspection unchecked
			final X unwrapped = (X) newInstance( preferredJavaTypeClass, value.size() );
			int i = 0;
			for ( E element : value ) {
				set( unwrapped, i, componentJavaType.unwrap( element, preferredJavaTypeClass, options ) );
				i++;
			}
			return unwrapped;
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> C wrap(X value, WrapperOptions options) {
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

		if ( value instanceof Object[] raw ) {
			final C wrapped = semantics.instantiateRaw( raw.length, null );
			if ( componentJavaType.getJavaTypeClass()
					.isAssignableFrom( value.getClass().getComponentType() ) ) {
				for ( Object o : raw ) {
					//noinspection unchecked
					wrapped.add( (E) o );
				}
			}
			else {
				for ( Object o : raw ) {
					wrapped.add( componentJavaType.wrap( o, options ) );
				}
			}
			return wrapped;
		}
		else if ( value instanceof byte[] bytes ) {
			// When the value is a byte[], this is a deserialization request
			//noinspection unchecked
			return fromCollection( (ArrayList<E>) SerializationHelper.deserialize( bytes ), options );
		}
		else if ( value instanceof BinaryStream stream ) {
			// When the value is a BinaryStream, this is a deserialization request
			//noinspection unchecked
			return fromCollection( (ArrayList<E>) SerializationHelper.deserialize( stream.getBytes() ), options );
		}
		else if ( value instanceof Collection<?> ) {
			//noinspection unchecked
			return fromCollection( (Collection<E>) value, options );
		}
		else if ( value.getClass().isArray() ) {
			final int length = getLength( value );
			final C wrapped = semantics.instantiateRaw( length, null );
			for ( int i = 0; i < length; i++ ) {
				wrapped.add( componentJavaType.wrap( get( value, i ), options ) );
			}
			return wrapped;
		}
		else if ( getElementJavaType().isInstance( value ) ) {
			// Support binding a single element as parameter value
			final C wrapped = semantics.instantiateRaw( 1, null );
			//noinspection unchecked
			wrapped.add( (E) value );
			return wrapped;
		}

		throw unknownWrap( value.getClass() );
	}

	private ArrayList<E> asArrayList(C value) {
		if ( value instanceof ArrayList ) {
			//noinspection unchecked
			return (ArrayList<E>) value;
		}
		return new ArrayList<>( value );
	}

	private C fromCollection(Collection<E> value, WrapperOptions options) {
		final C collection;
		switch ( semantics.getCollectionClassification() ) {
			case SET:
				// Keep consistent with CollectionMutabilityPlan::deepCopy
				//noinspection unchecked
				collection = (C) new LinkedHashSet<>( value.size() );
				break;
			case LIST:
			case BAG:
				if ( value instanceof ArrayList<E> arrayList ) {
					arrayList.replaceAll( e -> componentJavaType.wrap( e, options ) );
					//noinspection unchecked
					return (C) value;
				}
			default:
				collection = semantics.instantiateRaw( value.size(), null );
				break;
		}
		for ( E e : value ) {
			collection.add( componentJavaType.wrap( e, options ) );
		}
		return collection;
	}

	private static class CollectionMutabilityPlan<C extends Collection<E>, E> implements MutabilityPlan<C> {

		private final CollectionSemantics<C, E> semantics;
		private final MutabilityPlan<E> componentPlan;

		public CollectionMutabilityPlan(JavaType<E> baseDescriptor, CollectionSemantics<C, E> semantics) {
			this.semantics = semantics;
			this.componentPlan = baseDescriptor.getMutabilityPlan();
		}

		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public C deepCopy(C value) {
			if ( value == null ) {
				return null;
			}
			final C copy;
			if ( semantics.getCollectionClassification() == CollectionClassification.SET ) {
				// Retain the original order as we currently only map to the JDBC array type which is order sensitive
				//noinspection unchecked
				copy = (C) CollectionHelper.linkedSetOfSize( value.size() );
			}
			else {
				copy = semantics.instantiateRaw( value.size(), null );
			}

			for ( E element : value ) {
				copy.add( componentPlan.deepCopy( element ) );
			}
			return copy;
		}

		@Override
		public Serializable disassemble(C value, SharedSessionContract session) {
			return (Serializable) deepCopy( value );
		}

		@Override
		public C assemble(Serializable cached, SharedSessionContract session) {
			//noinspection unchecked
			return deepCopy( (C) cached );
		}

	}
}

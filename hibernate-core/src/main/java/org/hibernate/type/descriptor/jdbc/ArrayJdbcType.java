/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterArray;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import static java.lang.reflect.Array.get;
import static java.lang.reflect.Array.getLength;
import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
@AllowReflection // See https://hibernate.atlassian.net/browse/HHH-16809
public class ArrayJdbcType implements JdbcType {

	private final JdbcType elementJdbcType;

	public ArrayJdbcType(JdbcType elementJdbcType) {
		this.elementJdbcType = elementJdbcType;
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.ARRAY;
	}

	public JdbcType getElementJdbcType() {
		return elementJdbcType;
	}

	@Override
	public JavaType<?> getRecommendedJavaType(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		final var elementJavaType =
				elementJdbcType.getRecommendedJavaType( precision, scale, typeConfiguration );
		final var javaType =
				typeConfiguration.getJavaTypeRegistry()
						.resolveDescriptor( elementJavaType.getJavaTypeClass().arrayType() );
		if ( javaType instanceof BasicPluralType<?, ?> ) {
			return javaType;
		}
		else {
			final var parameterizedType =
					new ParameterizedTypeImpl( javaType.getJavaTypeClass(),
							new Type[0], null );
			return javaType.createJavaType( parameterizedType, typeConfiguration );
		}
	}

	protected static JavaType<?> elementJavaType(JavaType<?> javaTypeDescriptor) {
		if ( javaTypeDescriptor instanceof ByteArrayJavaType
			|| javaTypeDescriptor instanceof PrimitiveByteArrayJavaType ) {
			// Special handling needed for Byte[] and byte[],
			// because that would conflict with the VARBINARY mapping
			return ByteJavaType.INSTANCE;
		}
		else if ( javaTypeDescriptor instanceof BasicPluralJavaType<?> basicPluralJavaType ) {
			return basicPluralJavaType.getElementJavaType();
		}
		else {
			throw new IllegalArgumentException("not a BasicPluralJavaType");
		}
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		return new JdbcLiteralFormatterArray<>( javaTypeDescriptor,
				elementJdbcType.getJdbcLiteralFormatter( elementJavaType( javaTypeDescriptor ) ) );
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Object[].class;
	}

	protected String getElementTypeName(JavaType<?> javaType, SharedSessionContractImplementor session) {
		// TODO: ideally, we would have the actual size or the actual type/column accessible
		//       this is something that we would need for supporting composite types anyway
		if ( elementJdbcType instanceof StructuredJdbcType structJdbcType ) {
			return structJdbcType.getStructTypeName();
		}
		final var elementJavaType = elementJavaType( javaType );
		final Size size =
				session.getJdbcServices().getDialect().getSizeStrategy()
						.resolveSize( elementJdbcType, elementJavaType, null, null, null );
		final var ddlTypeRegistry = session.getTypeConfiguration().getDdlTypeRegistry();
		final String typeName =
				ddlTypeRegistry.getDescriptor( elementJdbcType.getDdlTypeCode() )
						.getTypeName( size, new BasicTypeImpl<>( elementJavaType, elementJdbcType), ddlTypeRegistry );

		final int cutIndexBegin = typeName.indexOf( '(' );
		if ( cutIndexBegin > 0 ) {
			final int cutIndexEnd = typeName.lastIndexOf( ')' );
			assert cutIndexEnd > cutIndexBegin;
			// getTypeName for this case required length, etc, parameters.
			// Cut them out and use database defaults.
			// e.g. "timestamp($p) with timezone" becomes "timestamp with timezone"
			return typeName.substring( 0, cutIndexBegin ) + typeName.substring( cutIndexEnd + 1 );
		}
		else {
			return typeName;
		}
	}

	protected <T,E> Object[] convertToArray(
			BasicBinder<T> binder,
			ValueBinder<E> elementBinder,
			BasicPluralJavaType<E> pluralJavaType,
			T value,
			WrapperOptions options)
					throws SQLException {
		final var elementJdbcType = this.getElementJdbcType();
		final var javaType = binder.getJavaType();
		if ( elementJdbcType instanceof AggregateJdbcType ) {
			final var domainObjects = javaType.unwrap( value, Object[].class, options );
			final var objects = new Object[domainObjects.length];
			for ( int i = 0; i < domainObjects.length; i++ ) {
				if ( domainObjects[i] != null ) {
					final E element =
							pluralJavaType.getElementJavaType()
									.cast( domainObjects[i] );
					objects[i] = elementBinder.getBindValue( element, options );
				}
			}
			return objects;
		}
		else {
			final var arrayClass =
					(Class<? extends Object[]>)
							elementJdbcJavaTypeClass( options, elementJdbcType )
									.arrayType();
			return javaType.unwrap( value, arrayClass, options );
		}
	}

	private static Class<?> elementJdbcJavaTypeClass(WrapperOptions options, JdbcType elementJdbcType) {
		final var typeConfiguration = options.getTypeConfiguration();
		final var preferredJavaTypeClass = elementJdbcType.getPreferredJavaTypeClass( options );
		return preferredJavaTypeClass != null
				? preferredJavaTypeClass
				: typeConfiguration.getJdbcTypeRegistry()
						.getDescriptor( elementJdbcType.getDefaultSqlTypeCode() )
						.getRecommendedJavaType( null, null, typeConfiguration )
						.getJavaTypeClass();
	}

	protected <X> X getArray(BasicExtractor<X> extractor, java.sql.Array array, WrapperOptions options)
			throws SQLException {
		final var javaType = extractor.getJavaType();
		return array != null
			&& getElementJdbcType() instanceof AggregateJdbcType aggregateJdbcType
			&& aggregateJdbcType.getEmbeddableMappingType() != null
				? javaType.wrap( toJavaArray( array, options, aggregateJdbcType ), options )
				: javaType.wrap( array, options );
	}

	private static Object @NonNull [] toJavaArray(
			java.sql.Array array,
			WrapperOptions options,
			AggregateJdbcType aggregateJdbcType)
			throws SQLException {
		final var embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
		final Object rawArray = array.getArray();
		final var domainObjects = new Object[ getLength( rawArray ) ];
		for ( int i = 0; i < domainObjects.length; i++ ) {
			final Object rawJdbcValue = get( rawArray, i );
			if ( rawJdbcValue == null ) {
				domainObjects[i] = null;
			}
			else {
				final var aggregateRawValues = aggregateJdbcType.extractJdbcValues( rawJdbcValue, options );
				domainObjects[i] = instantiate( embeddableMappingType,
						StructHelper.getAttributeValues( embeddableMappingType, aggregateRawValues, options ) );
			}
		}
		return domainObjects;
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		return new Binder<>( javaTypeDescriptor,
				(BasicPluralJavaType<?>) javaTypeDescriptor );
	}

	private class Binder<X,E> extends BasicBinder<X> {
		private final BasicPluralJavaType<E> pluralJavaType;

		private Binder(JavaType<X> javaType, BasicPluralJavaType<E> pluralJavaType) {
			super( javaType, ArrayJdbcType.this );
			this.pluralJavaType = pluralJavaType;
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
				throws SQLException {
			st.setArray( index, getArray( value, options ) );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
				throws SQLException {
			final var array = getArray( value, options );
			try {
				st.setObject( name, array, Types.ARRAY );
			}
			catch (SQLException ex) {
				throw new HibernateException(
						"JDBC driver does not support named parameters for setArray. Use positional.", ex );
			}
		}

		@Override
		public Object[] getBindValue(X value, WrapperOptions options) throws SQLException {
			final var elementBinder = getElementJdbcType().getBinder( pluralJavaType.getElementJavaType() );
			return convertToArray( this, elementBinder, pluralJavaType, value, options );
		}

		private java.sql.Array getArray(X value, WrapperOptions options) throws SQLException {
			final var session = options.getSession();
			return session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
					.createArrayOf( getElementTypeName( getJavaType(), session ),
							getBindValue( value, options ) );
		}
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaTypeDescriptor) {
		return new BasicExtractor<>( javaTypeDescriptor, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getArray( this, rs.getArray( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getArray( this, statement.getArray( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getArray( this, statement.getArray( name ), options );
			}
		};
	}

	@Override
	public String getFriendlyName() {
		return "ARRAY";
	}

	@Override
	public String toString() {
		return "ArrayTypeDescriptor(" + getElementJdbcType() + ")";
	}

	/**
	 * Check equality.
	 * Needed so that {@code ArrayJdbcType} in collections correctly
	 * match each other.
	 * @return true if the two array types share the same element type
	 */
	@Override
	public boolean equals(Object that) {
		return that instanceof ArrayJdbcType arrayJdbcType
			&& this.getClass() == that.getClass()
			&& getElementJdbcType().equals( arrayJdbcType.getElementJdbcType() );
	}

	@Override
	public int hashCode() {
		return getJdbcTypeCode() + 31 * getElementJdbcType().hashCode();
	}
}

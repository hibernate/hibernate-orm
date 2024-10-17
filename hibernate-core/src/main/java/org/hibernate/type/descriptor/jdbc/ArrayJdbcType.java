/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.dialect.StructAttributeValues;
import org.hibernate.dialect.StructHelper;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.ByteArrayJavaType;
import org.hibernate.type.descriptor.java.ByteJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcLiteralFormatterArray;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.dialect.StructHelper.instantiate;

/**
 * Descriptor for {@link Types#ARRAY ARRAY} handling.
 *
 * @author Christian Beikov
 * @author Jordan Gigov
 */
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
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		final JavaType<Object> elementJavaType = elementJdbcType.getJdbcRecommendedJavaTypeMapping(
				precision,
				scale,
				typeConfiguration
		);
		final JavaType<Object> javaType = typeConfiguration.getJavaTypeRegistry().resolveDescriptor(
				Array.newInstance( elementJavaType.getJavaTypeClass(), 0 ).getClass()
		);
		if ( javaType instanceof BasicPluralType<?, ?> ) {
			//noinspection unchecked
			return (JavaType<T>) javaType;
		}
		//noinspection unchecked
		return (JavaType<T>) javaType.createJavaType(
				new ParameterizedTypeImpl( javaType.getJavaTypeClass(), new Type[0], null ),
				typeConfiguration
		);
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaTypeDescriptor) {
		final JavaType<T> elementJavaType;
		if ( javaTypeDescriptor instanceof ByteArrayJavaType ) {
			// Special handling needed for Byte[], because that would conflict with the VARBINARY mapping
			//noinspection unchecked
			elementJavaType = (JavaType<T>) ByteJavaType.INSTANCE;
		}
		else if (javaTypeDescriptor instanceof BasicPluralJavaType) {
			//noinspection unchecked
			elementJavaType = ((BasicPluralJavaType<T>) javaTypeDescriptor).getElementJavaType();
		}
		else {
			throw new IllegalArgumentException("not a BasicPluralJavaType");
		}
		return new JdbcLiteralFormatterArray<>(
				javaTypeDescriptor,
				elementJdbcType.getJdbcLiteralFormatter( elementJavaType )
		);
	}

	@Override
	public Class<?> getPreferredJavaTypeClass(WrapperOptions options) {
		return Object[].class;
	}

	protected String getElementTypeName(JavaType<?> javaType, SharedSessionContractImplementor session) {
		// TODO: ideally, we would have the actual size or the actual type/column accessible
		//       this is something that we would need for supporting composite types anyway
		if ( elementJdbcType instanceof StructJdbcType ) {
			return ( (StructJdbcType) elementJdbcType ).getStructTypeName();
		}
		final JavaType<?> elementJavaType;
		if ( javaType instanceof ByteArrayJavaType ) {
			// Special handling needed for Byte[], because that would conflict with the VARBINARY mapping
			elementJavaType = ByteJavaType.INSTANCE;
		}
		else {
			elementJavaType = ( (BasicPluralJavaType<?>) javaType ).getElementJavaType();
		}
		final Size size = session.getJdbcServices()
				.getDialect()
				.getSizeStrategy()
				.resolveSize( elementJdbcType, elementJavaType, null, null, null );
		final DdlTypeRegistry ddlTypeRegistry = session.getTypeConfiguration().getDdlTypeRegistry();
		final String typeName = ddlTypeRegistry.getDescriptor( elementJdbcType.getDdlTypeCode() )
				.getTypeName( size, new BasicTypeImpl<>( elementJavaType, elementJdbcType), ddlTypeRegistry );
		int cutIndex = typeName.indexOf( '(' );
		if ( cutIndex > 0 ) {
			// getTypeName for this case required length, etc, parameters.
			// Cut them out and use database defaults.
			return typeName.substring( 0, cutIndex );
		}
		else {
			return typeName;
		}
	}

	protected <T> Object[] getArray(BasicBinder<?> binder, ValueBinder<T> elementBinder, T value, WrapperOptions options)
			throws SQLException {
		final JdbcType elementJdbcType = ( (ArrayJdbcType) binder.getJdbcType() ).getElementJdbcType();
		//noinspection unchecked
		final JavaType<T> javaType = (JavaType<T>) binder.getJavaType();
		if ( elementJdbcType instanceof AggregateJdbcType ) {
			final T[] domainObjects = (T[]) javaType.unwrap( value, Object[].class, options );
			final Object[] objects = new Object[domainObjects.length];
			for ( int i = 0; i < domainObjects.length; i++ ) {
				if ( domainObjects[i] != null ) {
					objects[i] = elementBinder.getBindValue( domainObjects[i], options );
				}
			}
			return objects;
		}
		else {
			final TypeConfiguration typeConfiguration = options.getSessionFactory().getTypeConfiguration();
			final JdbcType underlyingJdbcType =
					typeConfiguration.getJdbcTypeRegistry().getDescriptor( elementJdbcType.getDefaultSqlTypeCode() );
			final Class<?> preferredJavaTypeClass = elementJdbcType.getPreferredJavaTypeClass( options );
			final Class<?> elementJdbcJavaTypeClass =
					preferredJavaTypeClass == null
							? underlyingJdbcType.getJdbcRecommendedJavaTypeMapping(null, null, typeConfiguration )
									.getJavaTypeClass()
							: preferredJavaTypeClass;
			final Class<? extends Object[]> arrayClass = (Class<? extends Object[]>)
					Array.newInstance( elementJdbcJavaTypeClass, 0 ).getClass();
			return javaType.unwrap( value, arrayClass, options );
		}
	}

	protected <X> X getArray(BasicExtractor<X> extractor, java.sql.Array array, WrapperOptions options) throws SQLException {
		if ( array != null && getElementJdbcType() instanceof AggregateJdbcType ) {
			final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) getElementJdbcType();
			final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
			final Object rawArray = array.getArray();
			final Object[] domainObjects = new Object[Array.getLength( rawArray )];
			for ( int i = 0; i < domainObjects.length; i++ ) {
				final Object[] aggregateRawValues = aggregateJdbcType.extractJdbcValues( Array.get( rawArray, i ), options );
				final StructAttributeValues attributeValues =
						StructHelper.getAttributeValues( embeddableMappingType, aggregateRawValues, options );
				domainObjects[i] = instantiate( embeddableMappingType, attributeValues, options.getSessionFactory() );
			}
			return extractor.getJavaType().wrap( domainObjects, options );
		}
		else {
			return extractor.getJavaType().wrap( array, options );
		}
	}

	@Override
	public <X> ValueBinder<X> getBinder(final JavaType<X> javaTypeDescriptor) {
		@SuppressWarnings("unchecked")
		final BasicPluralJavaType<X> pluralJavaType = (BasicPluralJavaType<X>) javaTypeDescriptor;
		final ValueBinder<X> elementBinder = elementJdbcType.getBinder( pluralJavaType.getElementJavaType() );
		return new BasicBinder<>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
				st.setArray( index, getArray( value, options ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final java.sql.Array arr = getArray( value, options );
				try {
					st.setObject( name, arr, java.sql.Types.ARRAY );
				}
				catch (SQLException ex) {
					throw new HibernateException( "JDBC driver does not support named parameters for setArray. Use positional.", ex );
				}
			}

			@Override
			public Object getBindValue(X value, WrapperOptions options) throws SQLException {
				return ( (ArrayJdbcType) getJdbcType() ).getArray( this, elementBinder, value, options );
			}

			private java.sql.Array getArray(X value, WrapperOptions options) throws SQLException {
				final ArrayJdbcType arrayJdbcType = (ArrayJdbcType) getJdbcType();
				final Object[] objects = arrayJdbcType.getArray( this, elementBinder, value, options );

				final SharedSessionContractImplementor session = options.getSession();
				final String typeName = arrayJdbcType.getElementTypeName( getJavaType(), session );
				return session.getJdbcCoordinator().getLogicalConnection().getPhysicalConnection()
						.createArrayOf( typeName, objects );
			}
		};
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
		return "ArrayTypeDescriptor(" + getElementJdbcType().toString() + ")";
	}

	/**
	 * Check equality. Needed so that ArrayJdbcType in collections correctly match each other.
	 *
	 * @param o other object
	 * @return true if the two array types share the same element type
	 */
	@Override
	public boolean equals(Object o) {
		return o != null
			&& getClass() == o.getClass()
			&& getElementJdbcType().equals( ( (ArrayJdbcType) o ).getElementJdbcType() );
	}

	@Override
	public int hashCode() {
		return getJdbcTypeCode() + 31 * getElementJdbcType().hashCode();
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc.spi;

import java.io.Serializable;
import java.sql.Types;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;
import org.hibernate.type.descriptor.jdbc.SqlTypedJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcTypeBaseline;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.type.descriptor.JdbcTypeNameMapper.isStandardTypeCode;

/**
 * A registry mapping {@link org.hibernate.type.SqlTypes JDBC type codes}
 * to implementations of the {@link JdbcType} interface.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JdbcTypeRegistry implements JdbcTypeBaseline.BaselineTarget, Serializable {
//	private static final Logger LOG = Logger.getLogger( JdbcTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private final ConcurrentHashMap<Integer, JdbcType> descriptorMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, JdbcTypeConstructor> descriptorConstructorMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, AggregateJdbcType> aggregateDescriptorMap = new ConcurrentHashMap<>();
	/**
	 * A registry for storing the constructed {@link JdbcType} for both
	 * {@link JdbcTypeConstructor#resolveType(TypeConfiguration, Dialect, JdbcType, ColumnTypeInformation)} and
	 * {@link JdbcTypeConstructor#resolveType(TypeConfiguration, Dialect, BasicType, ColumnTypeInformation)} in a single
	 * map.
	 */
	private final ConcurrentHashMap<TypeConstructedJdbcTypeKey, JdbcType> typeConstructorDescriptorMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, SqlTypedJdbcType> sqlTypedDescriptorMap = new ConcurrentHashMap<>();

	public JdbcTypeRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
		JdbcTypeBaseline.prime( this );
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addDescriptor(JdbcType jdbcType) {
		final JdbcType previous = descriptorMap.put( jdbcType.getDefaultSqlTypeCode(), jdbcType );
//		if ( previous != null && previous != jdbcType ) {
//			LOG.tracef( "addDescriptor(%s) replaced previous registration(%s)", jdbcType, previous );
//		}
	}

	@Override
	public void addDescriptor(int typeCode, JdbcType jdbcType) {
		final JdbcType previous = descriptorMap.put( typeCode, jdbcType );
//		if ( previous != null && previous != jdbcType ) {
//			LOG.tracef( "addDescriptor(%d, %s) replaced previous registration(%s)", typeCode, jdbcType, previous );
//		}
	}

	public void addDescriptorIfAbsent(JdbcType jdbcType) {
		descriptorMap.putIfAbsent( jdbcType.getDefaultSqlTypeCode(), jdbcType );
	}

	public void addDescriptorIfAbsent(int typeCode, JdbcType jdbcType) {
		descriptorMap.putIfAbsent( typeCode, jdbcType );
	}

	public JdbcType findDescriptor(int jdbcTypeCode) {
		return descriptorMap.get( jdbcTypeCode );
	}

	public JdbcType getDescriptor(int jdbcTypeCode) {
		final JdbcType descriptor = descriptorMap.get( jdbcTypeCode );
		if ( descriptor != null ) {
			return descriptor;
		}
		else {
//			if ( isStandardTypeCode( jdbcTypeCode ) ) {
//				LOG.debugf( "A standard JDBC type code [%s] was not defined in SqlTypeDescriptorRegistry",
//						jdbcTypeCode );
//			}

			// see if the typecode is part of a known type family...
			final JdbcType potentialAlternateDescriptor = getFamilyDescriptor( jdbcTypeCode );
			if ( potentialAlternateDescriptor != null ) {
				return potentialAlternateDescriptor;
			}
			else {
				// finally, create a new descriptor mapping to getObject/setObject for this type code...
				final ObjectJdbcType fallBackDescriptor = new ObjectJdbcType( jdbcTypeCode );
				addDescriptor( fallBackDescriptor );
				return fallBackDescriptor;
			}
		}
	}

	private JdbcType getFamilyDescriptor(int jdbcTypeCode) {
		final JdbcTypeFamilyInformation.Family family =
				JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode );
		if ( family != null ) {
			for ( int potentialAlternateTypeCode : family.getTypeCodes() ) {
				if ( potentialAlternateTypeCode != jdbcTypeCode ) {
					final JdbcType potentialAlternateDescriptor = descriptorMap.get( potentialAlternateTypeCode );
					if ( potentialAlternateDescriptor != null ) {
						// todo (6.0) : add a SqlTypeDescriptor#canBeAssignedFrom method ?
						return potentialAlternateDescriptor;
					}
//					if ( isStandardTypeCode( potentialAlternateTypeCode ) ) {
//						LOG.debugf( "A standard JDBC type code [%s] was not defined in SqlTypeDescriptorRegistry",
//								potentialAlternateTypeCode );
//					}
				}
			}
		}
		return null;
	}

	public AggregateJdbcType resolveAggregateDescriptor(
			int jdbcTypeCode,
			String typeName,
			EmbeddableMappingType embeddableMappingType,
			RuntimeModelCreationContext context) {
		final String registrationKey;
		if ( typeName != null ) {
			registrationKey = typeName.toLowerCase( Locale.ROOT );
			final AggregateJdbcType aggregateJdbcType = aggregateDescriptorMap.get( registrationKey );
			if ( aggregateJdbcType != null ) {
				if ( aggregateJdbcType.getEmbeddableMappingType() != embeddableMappingType ) {
					// We only register a single aggregate descriptor for reading native query results,
					// but we still return a special JdbcType per EmbeddableMappingType.
					// We do this because EmbeddableMappingType#forEachSelectable uses the SelectableMappings,
					// which are prefixed with the aggregateMapping.
					// Since the columnExpression is used as key for mutation parameters, this is important.
					// We could get rid of this if ColumnValueParameter drops the ColumnReference
					return aggregateJdbcType.resolveAggregateJdbcType( embeddableMappingType, typeName, context );
				}
				else {
					return aggregateJdbcType;
				}
			}
		}
		else {
			registrationKey = null;
		}
		return resolveAggregateDescriptor( jdbcTypeCode, typeName, embeddableMappingType, context, registrationKey );
	}

	private AggregateJdbcType resolveAggregateDescriptor(
			int jdbcTypeCode,
			String typeName,
			EmbeddableMappingType embeddableMappingType,
			RuntimeModelCreationContext context,
			String registrationKey) {
		final JdbcType descriptor = getDescriptor( jdbcTypeCode );
		if ( descriptor instanceof AggregateJdbcType aggregateJdbcType ) {
			final AggregateJdbcType resolvedJdbcType =
					aggregateJdbcType.resolveAggregateJdbcType( embeddableMappingType, typeName, context );
			cacheAggregateJdbcType( registrationKey, resolvedJdbcType );
			return resolvedJdbcType;
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							"Tried to resolve the JdbcType [%s] as AggregateJdbcType but it does not implement that interface!",
							descriptor.getClass().getName()
					)
			);
		}
	}

	private void cacheAggregateJdbcType(String registrationKey, AggregateJdbcType resolvedJdbcType) {
		if ( registrationKey != null ) {
			aggregateDescriptorMap.put( registrationKey, resolvedJdbcType );
			if ( resolvedJdbcType instanceof SqlTypedJdbcType sqlTypedJdbcType ) {
				sqlTypedDescriptorMap.put(
						sqlTypedJdbcType.getSqlTypeName().toLowerCase( Locale.ROOT ),
						sqlTypedJdbcType
				);
			}
		}
	}

	public AggregateJdbcType findAggregateDescriptor(String typeName) {
		return aggregateDescriptorMap.get( typeName.toLowerCase( Locale.ROOT ) );
	}

	public SqlTypedJdbcType findSqlTypedDescriptor(String sqlTypeName) {
		return sqlTypedDescriptorMap.get( sqlTypeName.toLowerCase( Locale.ROOT ) );
	}

	/**
	 * Construct a {@link JdbcType} via {@link JdbcTypeConstructor#resolveType(TypeConfiguration, Dialect, BasicType, ColumnTypeInformation)}
	 * or return a compatible one from this registry.
	 */
	public JdbcType resolveTypeConstructorDescriptor(
			int jdbcTypeConstructorCode,
			BasicType<?> elementType,
			@Nullable ColumnTypeInformation columnTypeInformation) {
		return resolveTypeConstructorDescriptor( jdbcTypeConstructorCode, (Object) elementType, columnTypeInformation );
	}

	/**
	 * Construct a {@link JdbcType} via {@link JdbcTypeConstructor#resolveType(TypeConfiguration, Dialect, JdbcType, ColumnTypeInformation)}
	 * or return a compatible one from this registry.
	 */
	public JdbcType resolveTypeConstructorDescriptor(
			int jdbcTypeConstructorCode,
			JdbcType elementType,
			@Nullable ColumnTypeInformation columnTypeInformation) {
		return resolveTypeConstructorDescriptor( jdbcTypeConstructorCode, (Object) elementType, columnTypeInformation );
	}

	private JdbcType resolveTypeConstructorDescriptor(
			int jdbcTypeConstructorCode,
			Object elementType,
			@Nullable ColumnTypeInformation columnTypeInformation) {
		final TypeConstructedJdbcTypeKey key =
				columnTypeInformation == null
						? new TypeConstructedJdbcTypeKey( jdbcTypeConstructorCode, elementType )
						: new TypeConstructedJdbcTypeKey( jdbcTypeConstructorCode, elementType, columnTypeInformation );
		final JdbcType descriptor = typeConstructorDescriptorMap.get( key );
		if ( descriptor != null ) {
			return descriptor;
		}
		else {
			final JdbcTypeConstructor jdbcTypeConstructor = getConstructor( jdbcTypeConstructorCode );
			if ( jdbcTypeConstructor != null ) {
				final JdbcType jdbcType = jdbcElementType( elementType, columnTypeInformation, jdbcTypeConstructor );
				final JdbcType existingType = typeConstructorDescriptorMap.putIfAbsent( key, jdbcType );
				if ( existingType != null ) {
					return existingType;
				}
				else {
					if ( jdbcType instanceof SqlTypedJdbcType sqlTypedJdbcType ) {
						sqlTypedDescriptorMap.put(
								sqlTypedJdbcType.getSqlTypeName().toLowerCase( Locale.ROOT ),
								sqlTypedJdbcType
						);
					}
					return jdbcType;
				}
			}
			else {
				return getDescriptor( jdbcTypeConstructorCode );
			}
		}
	}

	private JdbcType jdbcElementType(
			Object elementType,
			ColumnTypeInformation columnTypeInformation,
			JdbcTypeConstructor jdbcTypeConstructor) {
		final Dialect dialect = typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
		if ( elementType instanceof BasicType<?> basicType ) {
			return jdbcTypeConstructor.resolveType(
					typeConfiguration,
					dialect,
					basicType,
					columnTypeInformation
			);
		}
		else {
			return jdbcTypeConstructor.resolveType(
					typeConfiguration,
					dialect,
					(JdbcType) elementType,
					columnTypeInformation
			);
		}
	}

	public boolean hasRegisteredDescriptor(int jdbcTypeCode) {
		return descriptorMap.containsKey( jdbcTypeCode )
			|| isStandardTypeCode( jdbcTypeCode )
			|| JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode ) != null
			|| locateConstructedJdbcType( jdbcTypeCode );
	}

	private boolean locateConstructedJdbcType(int jdbcTypeCode) {
		for ( TypeConstructedJdbcTypeKey key : typeConstructorDescriptorMap.keySet() ) {
			if ( key.typeCode() == jdbcTypeCode ) {
				return true;
			}
		}
		return false;
	}

	public JdbcTypeConstructor getConstructor(int jdbcTypeCode) {
		return descriptorConstructorMap.get( jdbcTypeCode );
	}

	public void addTypeConstructor(int jdbcTypeCode, JdbcTypeConstructor jdbcTypeConstructor) {
		descriptorConstructorMap.put( jdbcTypeCode, jdbcTypeConstructor );
	}

	public void addTypeConstructor(JdbcTypeConstructor jdbcTypeConstructor) {
		addTypeConstructor( jdbcTypeConstructor.getDefaultSqlTypeCode(), jdbcTypeConstructor );
	}

	public void addTypeConstructorIfAbsent(int jdbcTypeCode, JdbcTypeConstructor jdbcTypeConstructor) {
		descriptorConstructorMap.putIfAbsent( jdbcTypeCode, jdbcTypeConstructor );
	}

	public void addTypeConstructorIfAbsent(JdbcTypeConstructor jdbcTypeConstructor) {
		addTypeConstructorIfAbsent( jdbcTypeConstructor.getDefaultSqlTypeCode(), jdbcTypeConstructor );
	}

	private record TypeConstructedJdbcTypeKey(
			int typeConstructorTypeCode,
			int typeCode,
			@Nullable Boolean nullable,
			@Nullable String typeName,
			int columnSize,
			int decimalDigits,
			Object jdbcTypeOrBasicType) {

		private TypeConstructedJdbcTypeKey(
				int typeConstructorTypeCode,
				Object jdbcTypeOrBasicType) {
			this( typeConstructorTypeCode,
					Types.OTHER,
					null,
					null,
					0,
					0,
					jdbcTypeOrBasicType );
		}

		private TypeConstructedJdbcTypeKey(
				int typeConstructorTypeCode,
				Object jdbcTypeOrBasicType,
				ColumnTypeInformation columnTypeInformation) {
			this( typeConstructorTypeCode,
					columnTypeInformation.getTypeCode(),
					columnTypeInformation.getNullable(),
					columnTypeInformation.getTypeName(),
					columnTypeInformation.getColumnSize(),
					columnTypeInformation.getDecimalDigits(),
					jdbcTypeOrBasicType
			);
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !(o instanceof TypeConstructedJdbcTypeKey that) ) {
				return false;
			}
			return typeConstructorTypeCode == that.typeConstructorTypeCode
				&& typeCode == that.typeCode
				&& columnSize == that.columnSize
				&& decimalDigits == that.decimalDigits
				&& Objects.equals( nullable, that.nullable )
				&& Objects.equals( typeName, that.typeName )
				&& jdbcTypeOrBasicType.equals( that.jdbcTypeOrBasicType );
		}

		@Override
		public int hashCode() {
			int result = typeConstructorTypeCode;
			result = 31 * result + jdbcTypeOrBasicType.hashCode();
			result = 31 * result + (nullable == null ? 0 : nullable.hashCode());
			result = 31 * result + typeCode;
			result = 31 * result + (typeName == null ? 0 : typeName.hashCode());
			result = 31 * result + columnSize;
			result = 31 * result + decimalDigits;
			return result;
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc.spi;

import java.io.Serializable;
import java.sql.Types;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.boot.model.TruthValue;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcTypeBaseline;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import org.checkerframework.checker.nullness.qual.Nullable;

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
	private static final Logger log = Logger.getLogger( JdbcTypeRegistry.class );

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
		if ( previous != null && previous != jdbcType ) {
			log.debugf( "addDescriptor(%s) replaced previous registration(%s)", jdbcType, previous );
		}
	}

	@Override
	public void addDescriptor(int typeCode, JdbcType jdbcType) {
		final JdbcType previous = descriptorMap.put( typeCode, jdbcType );
		if ( previous != null && previous != jdbcType ) {
			log.debugf( "addDescriptor(%d, %s) replaced previous registration(%s)", typeCode, jdbcType, previous );
		}
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
		JdbcType descriptor = descriptorMap.get( jdbcTypeCode );
		if ( descriptor != null ) {
			return descriptor;
		}

		if ( JdbcTypeNameMapper.isStandardTypeCode( jdbcTypeCode ) ) {
			log.debugf(
					"A standard JDBC type code [%s] was not defined in SqlTypeDescriptorRegistry",
					jdbcTypeCode
			);
		}

		// see if the typecode is part of a known type family...
		JdbcTypeFamilyInformation.Family family =
				JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode );
		if ( family != null ) {
			for ( int potentialAlternateTypeCode : family.getTypeCodes() ) {
				if ( potentialAlternateTypeCode != jdbcTypeCode ) {
					final JdbcType potentialAlternateDescriptor = descriptorMap.get( potentialAlternateTypeCode );
					if ( potentialAlternateDescriptor != null ) {
						// todo (6.0) : add a SqlTypeDescriptor#canBeAssignedFrom method ?
						return potentialAlternateDescriptor;
					}

					if ( JdbcTypeNameMapper.isStandardTypeCode( potentialAlternateTypeCode ) ) {
						log.debugf(
								"A standard JDBC type code [%s] was not defined in SqlTypeDescriptorRegistry",
								potentialAlternateTypeCode
						);
					}
				}
			}
		}

		// finally, create a new descriptor mapping to getObject/setObject for this type code...
		final ObjectJdbcType fallBackDescriptor = new ObjectJdbcType( jdbcTypeCode );
		addDescriptor( fallBackDescriptor );
		return fallBackDescriptor;
	}

	public AggregateJdbcType resolveAggregateDescriptor(
			int jdbcTypeCode,
			String typeName,
			EmbeddableMappingType embeddableMappingType,
			RuntimeModelCreationContext creationContext) {
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
					return aggregateJdbcType.resolveAggregateJdbcType(
							embeddableMappingType,
							typeName,
							creationContext
					);
				}
				return aggregateJdbcType;
			}
		}
		else {
			registrationKey = null;
		}
		final JdbcType descriptor = getDescriptor( jdbcTypeCode );
		if ( !( descriptor instanceof AggregateJdbcType ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Tried to resolve the JdbcType [%s] as AggregateJdbcType but it does not implement that interface!",
							descriptor.getClass().getName()
					)
			);
		}
		final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) descriptor;
		final AggregateJdbcType resolvedJdbcType = aggregateJdbcType.resolveAggregateJdbcType(
				embeddableMappingType,
				typeName,
				creationContext
		);
		if ( registrationKey != null ) {
			aggregateDescriptorMap.put( registrationKey, resolvedJdbcType );
		}
		return resolvedJdbcType;
	}

	public AggregateJdbcType findAggregateDescriptor(String typeName) {
		return aggregateDescriptorMap.get( typeName.toLowerCase( Locale.ROOT ) );
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
		final TypeConstructedJdbcTypeKey key = new TypeConstructedJdbcTypeKey(
				jdbcTypeConstructorCode,
				elementType,
				columnTypeInformation
		);
		final JdbcType descriptor = typeConstructorDescriptorMap.get( key );
		if ( descriptor != null ) {
			return descriptor;
		}
		final JdbcTypeConstructor jdbcTypeConstructor = getConstructor( jdbcTypeConstructorCode );
		if ( jdbcTypeConstructor != null ) {
			final JdbcType jdbcType;
			if ( elementType instanceof BasicType<?> ) {
				jdbcType = jdbcTypeConstructor.resolveType(
						typeConfiguration,
						typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect(),
						(BasicType<?>) elementType,
						columnTypeInformation
				);
			}
			else {
				jdbcType = jdbcTypeConstructor.resolveType(
						typeConfiguration,
						typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect(),
						(JdbcType) elementType,
						columnTypeInformation
				);
			}
			final JdbcType existingType = typeConstructorDescriptorMap.putIfAbsent( key, jdbcType );
			return existingType != null ? existingType : jdbcType;
		}
		else {
			return getDescriptor( jdbcTypeConstructorCode );
		}
	}

	public boolean hasRegisteredDescriptor(int jdbcTypeCode) {
		return descriptorMap.containsKey( jdbcTypeCode )
			|| JdbcTypeNameMapper.isStandardTypeCode( jdbcTypeCode )
			|| JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode ) != null;
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

	private static final class TypeConstructedJdbcTypeKey {
		private final int typeConstructorTypeCode;
		private final Object jdbcTypeOrBasicType;
		private final TruthValue nullable;
		private final int typeCode;
		private final @Nullable String typeName;
		private final int columnSize;
		private final int decimalDigits;

		public TypeConstructedJdbcTypeKey(
				int typeConstructorTypeCode,
				Object jdbcTypeOrBasicType,
				@Nullable ColumnTypeInformation columnTypeInformation) {
			this.typeConstructorTypeCode = typeConstructorTypeCode;
			this.jdbcTypeOrBasicType = jdbcTypeOrBasicType;
			if ( columnTypeInformation == null ) {
				this.nullable = TruthValue.UNKNOWN;
				this.typeCode = Types.OTHER;
				this.typeName = null;
				this.columnSize = 0;
				this.decimalDigits = 0;
			}
			else {
				this.nullable = columnTypeInformation.getNullable();
				this.typeCode = columnTypeInformation.getTypeCode();
				this.typeName = columnTypeInformation.getTypeName();
				this.columnSize = columnTypeInformation.getColumnSize();
				this.decimalDigits = columnTypeInformation.getDecimalDigits();
			}
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			TypeConstructedJdbcTypeKey that = (TypeConstructedJdbcTypeKey) o;

			if ( typeConstructorTypeCode != that.typeConstructorTypeCode ) {
				return false;
			}
			if ( typeCode != that.typeCode ) {
				return false;
			}
			if ( columnSize != that.columnSize ) {
				return false;
			}
			if ( decimalDigits != that.decimalDigits ) {
				return false;
			}
			if ( !jdbcTypeOrBasicType.equals( that.jdbcTypeOrBasicType ) ) {
				return false;
			}
			if ( nullable != that.nullable ) {
				return false;
			}
			return Objects.equals( typeName, that.typeName );
		}

		@Override
		public int hashCode() {
			int result = typeConstructorTypeCode;
			result = 31 * result + jdbcTypeOrBasicType.hashCode();
			result = 31 * result + ( nullable != null ? nullable.hashCode() : 0 );
			result = 31 * result + typeCode;
			result = 31 * result + ( typeName != null ? typeName.hashCode() : 0 );
			result = 31 * result + columnSize;
			result = 31 * result + decimalDigits;
			return result;
		}
	}
}

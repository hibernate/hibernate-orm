/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc.spi;

import java.io.Serializable;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcTypeBaseline;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

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
}

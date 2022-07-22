/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc.spi;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.type.descriptor.JdbcTypeNameMapper;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcType;
import org.hibernate.type.descriptor.jdbc.internal.JdbcTypeBaseline;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Basically a map from JDBC type code (int) -> {@link JdbcType}
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
		JdbcTypeFamilyInformation.Family family = JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode );
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

	public boolean hasRegisteredDescriptor(int jdbcTypeCode) {
		return descriptorMap.containsKey( jdbcTypeCode )
				|| JdbcTypeNameMapper.isStandardTypeCode( jdbcTypeCode )
				|| JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode ) != null;
	}
}

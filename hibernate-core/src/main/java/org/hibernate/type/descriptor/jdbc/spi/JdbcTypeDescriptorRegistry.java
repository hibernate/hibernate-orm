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
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeFamilyInformation;
import org.hibernate.type.descriptor.jdbc.ObjectJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.internal.JdbcTypeDescriptorBaseline;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

/**
 * Basically a map from JDBC type code (int) -> {@link JdbcTypeDescriptor}
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 *
 * @since 5.3
 */
public class JdbcTypeDescriptorRegistry implements JdbcTypeDescriptorBaseline.BaselineTarget, Serializable {
	private static final Logger log = Logger.getLogger( JdbcTypeDescriptorRegistry.class );

	private final ConcurrentHashMap<Integer, JdbcTypeDescriptor> descriptorMap = new ConcurrentHashMap<>();

	public JdbcTypeDescriptorRegistry(TypeConfiguration typeConfiguration) {
//		this.typeConfiguration = typeConfiguration;
		JdbcTypeDescriptorBaseline.prime( this );
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// baseline descriptors

	@Override
	public void addDescriptor(JdbcTypeDescriptor jdbcTypeDescriptor) {
		final JdbcTypeDescriptor previous = descriptorMap.put( jdbcTypeDescriptor.getJdbcTypeCode(), jdbcTypeDescriptor );
		if ( previous != null && previous != jdbcTypeDescriptor ) {
			log.debugf( "addDescriptor(%s) replaced previous registration(%s)", jdbcTypeDescriptor, previous );
		}
	}

	public void addDescriptor(int typeCode, JdbcTypeDescriptor jdbcTypeDescriptor) {
		final JdbcTypeDescriptor previous = descriptorMap.put( typeCode, jdbcTypeDescriptor );
		if ( previous != null && previous != jdbcTypeDescriptor ) {
			log.debugf( "addDescriptor(%d, %s) replaced previous registration(%s)", typeCode, jdbcTypeDescriptor, previous );
		}
	}

	public JdbcTypeDescriptor getDescriptor(int jdbcTypeCode) {
		JdbcTypeDescriptor descriptor = descriptorMap.get( jdbcTypeCode );
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
					final JdbcTypeDescriptor potentialAlternateDescriptor = descriptorMap.get( potentialAlternateTypeCode );
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
		final ObjectJdbcTypeDescriptor fallBackDescriptor = new ObjectJdbcTypeDescriptor( jdbcTypeCode );
		addDescriptor( fallBackDescriptor );
		return fallBackDescriptor;
	}

	public boolean hasRegisteredDescriptor(int jdbcTypeCode) {
		return descriptorMap.containsKey( jdbcTypeCode )
				|| JdbcTypeNameMapper.isStandardTypeCode( jdbcTypeCode )
				|| JdbcTypeFamilyInformation.INSTANCE.locateJdbcTypeFamilyByTypeCode( jdbcTypeCode ) != null;
	}
}

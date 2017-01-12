/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.type.spi.basic;

import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicTypeHelper {
	@SuppressWarnings("unchecked")
	public static RegistryKey getRegistryKey(Class javaType, TypeConfiguration typeConfiguration) {
		final JavaTypeDescriptor jdt = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( javaType );
		final SqlTypeDescriptor sdt = jdt.getJdbcRecommendedSqlType(
				typeConfiguration.getBasicTypeRegistry().getBaseJdbcRecommendedSqlTypeMappingContext()
		);

		return RegistryKey.from( jdt, sdt );
	}

	public static BasicType getRegisteredBasicType(Class javaType, TypeConfiguration typeConfiguration) {
		// todo : should we create it if not found?
		final RegistryKey registryKey = getRegistryKey( javaType, typeConfiguration );
		return typeConfiguration.getBasicTypeRegistry().getRegisteredBasicType( registryKey );
	}

	private BasicTypeHelper() {
	}
}

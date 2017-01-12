/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.basic.RegistryKey;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.descriptor.TypeDescriptorRegistryAccess;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Defines the target of contributing types, whether via dialects or {@link TypeContributor}
 *
 * @author Steve Ebersole
 */
public interface TypeContributions {
	void contributeJavaTypeDescriptor(JavaTypeDescriptor descriptor);

	void contributeSqlTypeDescriptor(SqlTypeDescriptor descriptor);

	void contributeType(BasicType type, RegistryKey key);

	TypeConfiguration getTypeConfiguration();

	default TypeDescriptorRegistryAccess getTypeDescriptorRegistryAccess() {
		return getTypeConfiguration().getTypeDescriptorRegistryAccess();
	}
}

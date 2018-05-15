/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Defines the target of contributing types, whether via dialects or {@link TypeContributor}
 *
 * @author Steve Ebersole
 */
public interface TypeContributions {
	TypeConfiguration getTypeConfiguration();

	/**
	 * Add the JavaTypeDescriptor to the {@link TypeConfiguration}'s
	 * {@link JavaTypeDescriptorRegistry}
	 */
	void contributeJavaTypeDescriptor(JavaTypeDescriptor descriptor);

	/**
	 * Add the JavaTypeDescriptor to the {@link TypeConfiguration}'s
	 * {@link JavaTypeDescriptorRegistry}
	 */
	void contributeSqlTypeDescriptor(SqlTypeDescriptor descriptor);

	void contributeType(BasicType type);
}

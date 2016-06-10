/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor;

import org.hibernate.type.descriptor.java.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptorRegistry;

/**
 * Defines combined access to a {@link JavaTypeDescriptorRegistry} and
 * {@link SqlTypeDescriptorRegistry} combo.
 *
 * @author Steve Ebersole
 */
public interface TypeDescriptorRegistryAccess {
	/**
	 * Access to the contained JavaTypeDescriptorRegistry.
	 *
	 * @return The contained JavaTypeDescriptorRegistry
	 */
	JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry();

	/**
	 * Access to the contained SqlTypeDescriptorRegistry.
	 *
	 * @return The contained SqlTypeDescriptorRegistry
	 */
	SqlTypeDescriptorRegistry getSqlTypeDescriptorRegistry();
}

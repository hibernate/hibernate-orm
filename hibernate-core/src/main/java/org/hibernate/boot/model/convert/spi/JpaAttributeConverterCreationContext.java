/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;

/**
 * Access to information that implementors of
 * {@link ConverterDescriptor#createJpaAttributeConverter} might
 * need
 *
 * @author Steve Ebersole
 */
public interface JpaAttributeConverterCreationContext {
	ManagedBeanRegistry getManagedBeanRegistry();
	JavaTypeDescriptorRegistry getJavaTypeDescriptorRegistry();
}

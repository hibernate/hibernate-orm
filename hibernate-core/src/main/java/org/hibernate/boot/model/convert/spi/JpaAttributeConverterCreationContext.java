/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.convert.spi;

import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Access to information that implementors of
 * {@link ConverterDescriptor#createJpaAttributeConverter} might
 * need
 *
 * @author Steve Ebersole
 */
public interface JpaAttributeConverterCreationContext {
	ManagedBeanRegistry getManagedBeanRegistry();
	TypeConfiguration getTypeConfiguration();

	default JavaTypeRegistry getJavaTypeRegistry() {
		return getTypeConfiguration().getJavaTypeRegistry();
	}
}

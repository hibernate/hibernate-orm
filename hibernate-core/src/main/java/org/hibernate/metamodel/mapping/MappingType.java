/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * Common descriptor for types in the mapping model - entities, embeddables, String, Integer, etc
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface MappingType {
	/**
	 * The {@linkplain JavaType descriptor} descriptor for the mapped Java type
	 */
	JavaType<?> getMappedJavaType();
}

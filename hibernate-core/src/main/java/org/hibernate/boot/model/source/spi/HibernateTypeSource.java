/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

import org.hibernate.boot.model.JavaTypeDescriptor;

/**
 * Source-agnostic descriptor for explicit user-supplied Hibernate type information
 *
 * @author Steve Ebersole
 */
public interface HibernateTypeSource {
	/**
	 * Obtain the supplied Hibernate type name.
	 *
	 * @return The Hibernate type name
	 */
	String getName();

	/**
	 * Obtain any supplied Hibernate type parameters.
	 *
	 * @return The Hibernate type parameters.
	 */
	Map<String,String> getParameters();

	/**
	 * Obtain the attribute's java type if possible.
	 *
	 * @return The java type of the attribute or {@code null}.
	 */
	JavaTypeDescriptor getJavaType();
}

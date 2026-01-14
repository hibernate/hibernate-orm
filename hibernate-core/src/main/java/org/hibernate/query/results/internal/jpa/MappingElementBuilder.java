/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.jpa;

import org.hibernate.query.results.spi.ResultBuilder;

/// Adapter for [jakarta.persistence.sql.MappingElement] as a
/// [org.hibernate.query.results.spi.ResultBuilder].
///
/// @author Steve Ebersole
public interface MappingElementBuilder<T> extends ResultBuilder {
	/// @see jakarta.persistence.sql.MappingElement#getAlias()
	String getAlias();

	/// @see jakarta.persistence.sql.MappingElement#getJavaType()
	Class<? extends T> getJavaType();
}

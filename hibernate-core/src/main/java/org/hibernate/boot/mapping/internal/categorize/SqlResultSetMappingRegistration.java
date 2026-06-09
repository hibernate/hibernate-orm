/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.persistence.SqlResultSetMapping;
import org.hibernate.models.spi.AnnotationTarget;

/// Registration of a SQL result set mapping while categorizing XML and annotation sources.
///
/// @since 9.0
/// @author Steve Ebersole
public record SqlResultSetMappingRegistration(
		String name,
		SqlResultSetMapping configuration,
		AnnotationTarget location) {
}

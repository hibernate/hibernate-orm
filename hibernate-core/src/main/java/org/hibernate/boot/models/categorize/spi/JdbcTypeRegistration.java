/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/// Global registration for a {@linkplain JdbcType JDBC type descriptor}.
///
/// @param code The JDBC type code handled by the descriptor
/// @param descriptor The JDBC type descriptor class
///
/// @see org.hibernate.annotations.JdbcTypeRegistration
/// @see org.hibernate.boot.jaxb.mapping.JaxbJdbcTypeRegistration
///
/// @since 9.0
/// @author Steve Ebersole
public record JdbcTypeRegistration(Integer code, ClassDetails descriptor) {
}

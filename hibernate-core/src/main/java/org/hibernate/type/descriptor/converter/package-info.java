/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines basic-typed value conversions, including support for handling JPA
 * {@link jakarta.persistence.AttributeConverter} instances as part of the
 * Hibernate {@link org.hibernate.type.Type} system. The main contract is
 * {@link org.hibernate.type.descriptor.converter.spi.BasicValueConverter}.
 * <p>
 * All basic value conversions are defined by this namespace, including
 * support for {@link jakarta.persistence.AttributeConverter} via
 * {@link org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter}.
 *
 * @see org.hibernate.type.descriptor.converter.spi.BasicValueConverter
 */
@Incubating
package org.hibernate.type.descriptor.converter;

import org.hibernate.Incubating;

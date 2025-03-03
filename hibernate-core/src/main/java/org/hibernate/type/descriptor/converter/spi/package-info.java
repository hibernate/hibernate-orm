/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * An SPI for basic-typed value conversions, including support for handling
 * JPA {@link jakarta.persistence.AttributeConverter} instances as part of
 * the Hibernate {@link org.hibernate.type.Type} system.
 * <p>
 * The main contract is
 * {@link org.hibernate.type.descriptor.converter.spi.BasicValueConverter},
 * which is specialized by
 * {@link org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter}
 * for adapting for JPA {@link jakarta.persistence.AttributeConverter}s.
 *
 * @see org.hibernate.type.descriptor.converter.spi.BasicValueConverter
 */
@Incubating
package org.hibernate.type.descriptor.converter.spi;

import org.hibernate.Incubating;

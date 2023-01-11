/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * An SPI for basic-typed value conversions, including support for handling
 * JPA {@link jakarta.persistence.AttributeConverter} instances as part of
 * the Hibernate {@link org.hibernate.type.Type} system.
 * <p>
 * The main contract is
 * {@link org.hibernate.type.descriptor.converter.spi.BasicValueConverter},
 * with specializations:
 * <ul>
 * <li>{@link org.hibernate.type.descriptor.converter.spi.EnumValueConverter}
 *     for Java {@code enum} conversions, and
 * <li>{@link org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter}
 *     for adapting for JPA {@link jakarta.persistence.AttributeConverter}.
 * </ul>
 *
 * @see org.hibernate.type.descriptor.converter.spi.BasicValueConverter
 */
@Incubating
package org.hibernate.type.descriptor.converter.spi;

import org.hibernate.Incubating;

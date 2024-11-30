/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

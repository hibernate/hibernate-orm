/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * Support for basic-value conversions.  The main contract is
 * {@link org.hibernate.metamodel.model.convert.spi.BasicValueConverter}.
 *
 * All basic value conversions are defined by this package including:
 * 		* Enum conversions - {@link org.hibernate.metamodel.model.convert.spi.EnumValueConverter}
 * 		* AttributeConverter - {@link org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter}
 */
package org.hibernate.metamodel.model.convert;

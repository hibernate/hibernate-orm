/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * This package contains the contracts that make up the bootstrap API
 * for Hibernate. That is, they collectively provide a way to specify
 * configuration information and construct a new instance of
 * {@link org.hibernate.SessionFactory}.
 * <p>
 * Configuring Hibernate using these APIs usually starts with
 * {@link org.hibernate.boot.MetadataBuilder} and
 * {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}.
 * See the <em>Native Bootstrapping</em> guide for more details.
 * <p>
 * Included in subpackages under this namespace are:
 * <ul>
 * <li>implementations of {@link org.hibernate.boot.MetadataBuilder}
 *     and {@link org.hibernate.boot.Metadata},
 * <li>{@linkplain org.hibernate.boot.model.naming support} for
 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
 *     and {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy},
 * <li>{@linkplain org.hibernate.boot.spi a range of SPIs} allowing
 *     integration with the process of building metadata,
 * <li>internal code for parsing and interpreting mapping information
 *     declared in XML or using annotations,
 * <li>{@linkplain org.hibernate.boot.registry implementations} of
 *     {@link org.hibernate.service.ServiceRegistry} used during
 *     the bootstrap process,
 * <li>{@linkplain org.hibernate.boot.beanvalidation support} for
 *     integrating an implementation of Bean Validation, such as
 *     Hibernate Validator, and
 * <li>{@linkplain org.hibernate.boot.model.relational some SPIs}
 *     used for schema management including support for exporting
 *     {@linkplain org.hibernate.boot.model.relational.AuxiliaryDatabaseObject
 *     auxiliary database objects}.
 * </ul>
 */
package org.hibernate.boot;

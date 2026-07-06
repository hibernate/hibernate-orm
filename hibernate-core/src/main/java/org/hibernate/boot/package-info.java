/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package contains the interfaces that make up the bootstrap API
 * for Hibernate. They collectively provide a way to specify configuration
 * information and construct a new instance of {@link org.hibernate.SessionFactory}.
 * <p>
 * Native programmatic bootstrap is exposed via
 * {@link org.hibernate.jpa.HibernatePersistenceConfiguration}, which extends
 * Jakarta Persistence's programmatic bootstrap API with Hibernate-specific
 * conveniences.
 * <pre>
 * SessionFactory sessionFactory =
 *         new org.hibernate.jpa.HibernatePersistenceConfiguration("example")
 *                 .property(AvailableSettings.HBM2DDL_AUTO, "create-drop")
 *                 .managedClass(MyEntity.class)
 *                 .createEntityManagerFactory();
 * </pre>
 * <p>
 * In more advanced scenarios,
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder}
 * might also be used.
 * <p>
 * See the <em>Native Bootstrapping</em> guide for more details.
 * <p>
 * Included in subpackages under this namespace are:
 * <ul>
 * <li>{@linkplain org.hibernate.boot.registry implementations} of
 *     {@link org.hibernate.service.ServiceRegistry} used during
 *     the bootstrap process,
 * <li>implementations of {@link org.hibernate.boot.Metadata},
 * <li>{@linkplain org.hibernate.boot.model.naming support} for
 *     {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}
 *     and {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy},
 * <li>{@linkplain org.hibernate.boot.spi a range of SPIs} allowing
 *     integration with the process of building metadata,
 * <li>internal code for parsing and interpreting mapping information
 *     declared in XML or using annotations,
 * <li>{@linkplain org.hibernate.boot.beanvalidation support} for
 *     integrating an implementation of Bean Validation, such as
 *     <a href="https://hibernate.org/validator/">Hibernate Validator</a>,
 *     and
 * <li>{@linkplain org.hibernate.boot.model.relational some SPIs}
 *     used for schema management, including support for exporting
 *     {@linkplain org.hibernate.boot.model.relational.AuxiliaryDatabaseObject
 *     auxiliary database objects}, and for determining the
 *     {@linkplain org.hibernate.boot.model.relational.ColumnOrderingStrategy
 *     order of columns} in generated DDL statements.
 * </ul>
 */
package org.hibernate.boot;

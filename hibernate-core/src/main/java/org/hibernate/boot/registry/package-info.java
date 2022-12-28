/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines service registry contracts application are may utilize for configuring Hibernate.
 * <p>
 * Service registries are hierarchical. That is, a child registry may "hide" or "override" services from its parent
 * registries. This allows for granular construction of registries as services become available.
 * <ol>
 * <li>
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry} is the base service registry, and may be constructed via
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder} if customization is needed. For non-customized
 * usage, these APIs may be bypassed completely.
 * <li>
 * The next level in a standard registry setup is the {@link org.hibernate.boot.registry.StandardServiceRegistry},
 * which may be constructed using {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder} if customization
 * is needed. The builder optionally accepts s {@link org.hibernate.boot.registry.BootstrapServiceRegistry} to use as a
 * base. If none is provided, a default instance is produced, assuming sensible defaults in Java SE and EE environments,
 * particularly with respect to classloading.
 * </ol>
 */
package org.hibernate.boot.registry;

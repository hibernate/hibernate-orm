/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines service registry contracts application are likely to want to utilize for
 * configuring Hibernate behavior.
 *
 * Service registries are designed to be hierarchical.  This works in 2 fashions.  First registries can "hide" or
 * "override" services from parent registries.  It also allows granular building of registries as services
 * become available.
 *
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry} is the base service registry, intended to be built via
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder} if you need customization.  For non-customized
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry} usage, the {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder} and
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry} can be bypassed altogether.
 *
 * Usually the next level in a standard registry set up is the {@link org.hibernate.boot.registry.StandardServiceRegistry}, intended to be built
 * by the {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder} if you need customization.  The builder optionally takes the
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry} to use as a base; if none is provided a default one is generated assuming sensible
 * defaults in Java SE and EE environments, particularly in respect to Class loading.
 */
package org.hibernate.boot.registry;

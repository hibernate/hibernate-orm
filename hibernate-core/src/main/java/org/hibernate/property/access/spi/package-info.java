/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines strategies for accessing the persistent properties of an entity or composite.
 * <p/>
 * The overall strategy of the various ways to access a property is defined by the
 * {@link org.hibernate.property.access.spi.PropertyAccessStrategy} contract.
 * <p/>
 * The access for a specific property is modeled by a
 * {@link org.hibernate.property.access.spi.PropertyAccess} instance build from the strategy, exposing
 * {@link org.hibernate.property.access.spi.Getter} and {@link org.hibernate.property.access.spi.Setter}
 * delegates for accessing the properties values.
 * <p/>
 * {@link org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies} defines the built-in
 * named strategies understood in terms of mappings.  In mappings, users may refer to those short names
 * for referring to certain built-in strategies.  Users may also implement their own
 * strategy and refer to that by fully-qualified name, or they may leverage the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service to define short-naming
 * for their custom strategies.
 */
package org.hibernate.property.access.spi;

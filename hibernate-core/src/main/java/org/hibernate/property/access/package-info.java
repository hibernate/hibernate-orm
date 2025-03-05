/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Support for accessing the persistent state of an entity or embeddable.
 * <p>
 * The overall strategy of the various ways to access a property is defined by the
 * {@link org.hibernate.property.access.spi.PropertyAccessStrategy} contract.
 * <p>
 * The access for a specific property is modeled by a
 * {@link org.hibernate.property.access.spi.PropertyAccess} instance build from the strategy, exposing
 * {@link org.hibernate.property.access.spi.Getter} and {@link org.hibernate.property.access.spi.Setter}
 * delegates for accessing the properties values.
 * <p>
 * {@link org.hibernate.property.access.spi.BuiltInPropertyAccessStrategies} defines the built-in
 * named strategies understood in terms of mappings.  In mappings, users may refer to those short names
 * for referring to certain built-in strategies.  Users may also implement their own
 * strategy and refer to that by fully-qualified name, or they may leverage the
 * {@link org.hibernate.boot.registry.selector.spi.StrategySelector} service to define short-naming
 * for their custom strategies.
 *
 * @see org.hibernate.annotations.AttributeAccessor
 * @see jakarta.persistence.Access
 *
 * @author Steve Ebersole
 */
package org.hibernate.property.access;

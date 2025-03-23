/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.property.access.spi;

import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyFieldImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMapImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.internal.PropertyAccessStrategyNoopImpl;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Describes the built-in externally-nameable {@link PropertyAccessStrategy} implementations.
 *
 * @author Steve Ebersole
 */
public enum BuiltInPropertyAccessStrategies {
	BASIC( "property", PropertyAccessStrategyBasicImpl.INSTANCE ),
	FIELD( "field", PropertyAccessStrategyFieldImpl.INSTANCE ),
	MIXED( "mixed", PropertyAccessStrategyMixedImpl.INSTANCE ),
	MAP( "map", PropertyAccessStrategyMapImpl.INSTANCE ),
	EMBEDDED( "embedded", PropertyAccessStrategyEmbeddedImpl.INSTANCE ),
	NOOP( "noop", PropertyAccessStrategyNoopImpl.INSTANCE );

	private final String externalName;
	private final PropertyAccessStrategy strategy;

	BuiltInPropertyAccessStrategies(String externalName, PropertyAccessStrategy strategy) {
		this.externalName = externalName;
		this.strategy = strategy;
	}

	public String getExternalName() {
		return externalName;
	}

	public PropertyAccessStrategy getStrategy() {
		return strategy;
	}

	public static @Nullable BuiltInPropertyAccessStrategies interpret(String name) {
		for ( BuiltInPropertyAccessStrategies strategy : values() ) {
			if ( strategy.externalName.equals( name ) ) {
				return strategy;
			}
		}
		return null;
	}
}

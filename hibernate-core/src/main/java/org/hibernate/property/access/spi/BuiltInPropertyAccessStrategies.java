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
	BASIC,
	FIELD,
	MIXED,
	MAP,
	EMBEDDED,
	NOOP;

	public String getExternalName() {
		return switch ( this ) {
			case BASIC -> "property";
			case FIELD -> "field";
			case MIXED -> "mixed";
			case MAP -> "map";
			case EMBEDDED -> "embedded";
			case NOOP -> "noop";
		};
	}

	public PropertyAccessStrategy getStrategy() {
		return switch ( this ) {
			case BASIC -> PropertyAccessStrategyBasicImpl.INSTANCE;
			case FIELD -> PropertyAccessStrategyFieldImpl.INSTANCE;
			case MIXED -> PropertyAccessStrategyMixedImpl.INSTANCE;
			case MAP -> PropertyAccessStrategyMapImpl.INSTANCE;
			case EMBEDDED -> PropertyAccessStrategyEmbeddedImpl.INSTANCE;
			case NOOP -> PropertyAccessStrategyNoopImpl.INSTANCE;
		};
	}

	public static @Nullable BuiltInPropertyAccessStrategies interpret(String name) {
		for ( var strategy : values() ) {
			if ( strategy.getExternalName().equals( name ) ) {
				return strategy;
			}
		}
		return null;
	}
}

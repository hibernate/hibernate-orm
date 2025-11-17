/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry.selector.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.boot.registry.selector.spi.DialectSelector;
import org.hibernate.dialect.Dialect;

public class AggregatedDialectSelector implements DialectSelector {

	private final List<DialectSelector> dialectSelectors;

	public AggregatedDialectSelector(Iterable<DialectSelector> dialectSelectorProvider) {
		final List<DialectSelector> dialectSelectors = new ArrayList<>();
		for ( var dialectSelector : dialectSelectorProvider ) {
			dialectSelectors.add( dialectSelector );
		}
		dialectSelectors.add( new DefaultDialectSelector() );
		this.dialectSelectors = dialectSelectors;
	}

	@Override
	public Class<? extends Dialect> resolve(final String name) {
		Objects.requireNonNull( name );
		if ( name.isEmpty() ) {
			return null;
		}
		for ( var dialectSelector : dialectSelectors ) {
			final var dialectClass = dialectSelector.resolve( name );
			if ( dialectClass != null ) {
				return dialectClass;
			}
		}

		return null;
	}

}

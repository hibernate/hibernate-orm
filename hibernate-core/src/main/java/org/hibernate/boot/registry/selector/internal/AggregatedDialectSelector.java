/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		for ( DialectSelector dialectSelector : dialectSelectorProvider ) {
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
		for ( DialectSelector dialectSelector : dialectSelectors ) {
			final Class<? extends Dialect> dialectClass = dialectSelector.resolve( name );
			if ( dialectClass != null ) {
				return dialectClass;
			}
		}

		return null;
	}

}

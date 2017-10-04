/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Spliterator;

import org.hibernate.metamodel.model.domain.spi.InheritanceCapable;
import org.hibernate.metamodel.model.domain.spi.Navigable;

/**
 * @author Steve Ebersole
 */
public class NavigableSpliterator extends AbstractNavigableSpliterator<Navigable> {
	public <T> NavigableSpliterator(InheritanceCapable<T> container, boolean includeSuperNavigables) {
		super( container, includeSuperNavigables );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Spliterator<Navigable> nextSpliterator(InheritanceCapable container) {
		return container.getDeclaredAttributes().spliterator();
	}
}

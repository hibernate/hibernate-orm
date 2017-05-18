/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.sqm.SemanticException;

/**
 * Indicates that a "navigable" referenced in the query could not be resolved
 * to a {@link Navigable} reference in a
 * situation where we expect such a navigable to exists and it is an error for
 * it to not.
 *
 * @see NavigableContainer#findNavigable
 *
 * @author Steve Ebersole
 */
public class NavigableResolutionException extends SemanticException {
	public NavigableResolutionException(String message) {
		super( message );
	}
}

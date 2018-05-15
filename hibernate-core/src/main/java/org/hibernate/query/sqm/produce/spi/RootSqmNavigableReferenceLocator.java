/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;

/**
 * Defines the ability to locate
 * Context for PathResolver implementations to locate FromElements
 *
 * @author Steve Ebersole
 */
public interface RootSqmNavigableReferenceLocator {
	/**
	 * Find a FromElement by its identification variable (JPA term for alias).  Will search any parent contexts
	 *
	 * @param identificationVariable The identification variable
	 *
	 * @return matching FromElement, or {@code null}
	 */
	SqmNavigableReference findNavigableReferenceByIdentificationVariable(String identificationVariable);

	/**
	 * Find a FromElement which exposes the given Navigable by name.  Will search any parent contexts
	 *
	 * @param navigableName The name of the Navigable to find a FromElement for
	 *
	 * @return matching FromElement, or {@code null}
	 */
	SqmNavigableReference findNavigableReferenceExposingNavigable(String navigableName);
}

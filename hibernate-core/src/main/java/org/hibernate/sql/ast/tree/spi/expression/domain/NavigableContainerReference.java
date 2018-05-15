/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;

/**
 * @author Steve Ebersole
 */
public interface NavigableContainerReference extends NavigableReference {
	@Override
	NavigableContainer getNavigable();

	NavigableReference findNavigableReference(String navigableName);
	void  addNavigableReference(NavigableReference reference);
}

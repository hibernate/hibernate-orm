/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public interface SqmCollectionIndexReference extends SqmExpression, SqmNavigableReference {
	@Override
	Navigable getReferencedNavigable();

	@Override
	SqmPluralAttributeReference getSourceReference();
}

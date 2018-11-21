/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.BasicCollectionElement;
import org.hibernate.metamodel.model.domain.spi.BasicValuedNavigable;

/**
 * Specialized SqmRestrictedCollectionElementReference for basic-valued
 * collection elements
 *
 * @author Steve Ebersole
 */
public interface SqmRestrictedCollectionElementReferenceBasic extends SqmRestrictedCollectionElementReference {
	@Override
	BasicCollectionElement getExpressableType();

	@Override
	Supplier<? extends BasicCollectionElement> getInferableType();

	@Override
	BasicValuedNavigable getReferencedNavigable();
}

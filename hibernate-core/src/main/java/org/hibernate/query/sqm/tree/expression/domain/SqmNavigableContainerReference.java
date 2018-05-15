/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;

/**
 * @author Steve Ebersole
 */
public interface SqmNavigableContainerReference
		extends SqmNavigableReference, NavigableContainerReferenceInfo, SqmFromExporter {
	@Override
	NavigableContainer getReferencedNavigable();

	default SqmNavigableReference resolveSubNavigableReference(String navigableName, SqmCreationContext creationContext) {
		return getReferencedNavigable().findNavigable( navigableName ).createSqmExpression(
				getExportedFromElement(),
				this,
				creationContext
		);
	}
}

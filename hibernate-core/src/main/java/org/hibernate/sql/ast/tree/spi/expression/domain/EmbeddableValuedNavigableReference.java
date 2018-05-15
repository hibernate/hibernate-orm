/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.query.NavigablePath;

/**
 * @author Steve Ebersole
 */
public class EmbeddableValuedNavigableReference extends AbstractNavigableContainerReference {
	public EmbeddableValuedNavigableReference(
			NavigableContainerReference containerReference,
			EmbeddedValuedNavigable navigable,
			NavigablePath navigablePath,
			LockMode lockMode) {
		super( containerReference, navigable, navigablePath, containerReference.getColumnReferenceQualifier(), lockMode );
	}

	@Override
	public EmbeddedValuedNavigable getNavigable() {
		return (EmbeddedValuedNavigable) super.getNavigable();
	}
}

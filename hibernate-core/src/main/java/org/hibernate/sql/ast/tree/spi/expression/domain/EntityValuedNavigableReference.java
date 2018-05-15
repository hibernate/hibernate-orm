/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public class EntityValuedNavigableReference extends AbstractNavigableContainerReference {
	public EntityValuedNavigableReference(
			NavigableContainerReference ourContainerReference,
			EntityValuedNavigable navigable,
			NavigablePath navigablePath,
			ColumnReferenceQualifier columnReferenceQualifier,
			LockMode lockMode) {
		super( ourContainerReference, navigable, navigablePath, columnReferenceQualifier, lockMode );
	}

	@Override
	public EntityValuedNavigable getNavigable() {
		return (EntityValuedNavigable) super.getNavigable();
	}

	@Override
	public String toString() {
		return "EntityValuedNavigableReference(" + getNavigable().getEntityName() + ")";
	}
}

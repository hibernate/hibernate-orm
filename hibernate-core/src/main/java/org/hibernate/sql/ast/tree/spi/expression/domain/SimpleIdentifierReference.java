/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityIdentifier;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public class SimpleIdentifierReference implements NavigableReference {
	private final EntityValuedNavigableReference entityReference;
	private final EntityIdentifier identifierDescriptor;
	private final NavigablePath navigablePath;

	public SimpleIdentifierReference(
			EntityValuedNavigableReference entityReference,
			EntityIdentifier identifierDescriptor,
			NavigablePath navigablePath) {
		this.entityReference = entityReference;
		this.identifierDescriptor = identifierDescriptor;
		this.navigablePath = navigablePath;
	}

	@Override
	public ColumnReferenceQualifier getColumnReferenceQualifier() {
		return entityReference.getColumnReferenceQualifier();
	}

	@Override
	public Navigable getNavigable() {
		return identifierDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public EntityValuedNavigableReference getNavigableContainerReference() {
		return entityReference;
	}
}

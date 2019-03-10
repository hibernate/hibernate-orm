/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * @author Steve Ebersole
 */
public class SqmSingularAttributeReferenceEmbedded
		extends AbstractSqmSingularAttributeReference
		implements SqmEmbeddableTypedReference {
	private final SqmNavigableJoin navigableJoin;

	public SqmSingularAttributeReferenceEmbedded(
			SqmNavigableContainerReference domainReferenceBinding,
			SqmNavigableJoin navigableJoin,
			SingularPersistentAttributeEmbedded boundNavigable,
			SqmCreationState creationState) {
		super( domainReferenceBinding, boundNavigable );
		this.navigableJoin = navigableJoin;
	}

	@Override
	public SingularPersistentAttributeEmbedded getReferencedNavigable() {
		return (SingularPersistentAttributeEmbedded) super.getReferencedNavigable();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return navigableJoin;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		// todo (6.0) : define a QueryResultProducer that is also a tuple of SqlSelections (SqlSelectionGroup
		return walker.visitEmbeddableValuedSingularAttribute( this );
	}
}

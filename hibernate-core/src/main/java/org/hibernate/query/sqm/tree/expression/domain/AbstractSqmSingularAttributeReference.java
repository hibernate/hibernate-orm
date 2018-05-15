/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmSingularAttributeReference
		extends AbstractSqmAttributeReference<SingularPersistentAttribute>
		implements SqmSingularAttributeReference {
	public AbstractSqmSingularAttributeReference(
			SqmNavigableContainerReference navigableContainerReference,
			SingularPersistentAttribute referencedNavigable) {
		super( navigableContainerReference, referencedNavigable );
	}

	@Override
	public String getUniqueIdentifier() {
		return getExportedFromElement() == null ? null : getExportedFromElement().getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		return getExportedFromElement() == null ? null : getExportedFromElement().getIdentificationVariable();
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return getExportedFromElement() == null ? null : getExportedFromElement().getIntrinsicSubclassEntityMetadata();
	}

	@Override
	public SingularPersistentAttribute getReferencedNavigable() {
		return super.getReferencedNavigable();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		if ( ! NavigableContainer.class.isInstance( getReferencedNavigable() ) ) {
			throw new SemanticException(
					"Cannot dereference non-container Navigable [" +
							getNavigablePath().getFullPath() + " (" +
							getReferencedNavigable().getClass().getName() + ")"
			);
		}

		final NavigableContainer referenceNavigableContainer = NavigableContainer.class.cast( getReferencedNavigable() );
		final Navigable navigable = referenceNavigableContainer.findNavigable( name );
		if ( navigable == null ) {
			throw new SemanticException(
					"Could not resolve navigable [" + name +
							"] relative to source : " + getSourceReference().getNavigablePath().getFullPath() +
							" (" + getSourceReference().getClass().getName() + ")"
			);
		}
		return navigable.createSqmExpression(
				getExportedFromElement(),
				(SqmNavigableContainerReference) this,
				context
		);
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new UnsupportedOperationException(  );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmNavigableJoin;

/**
 * Specialization of a "Navigable reference" for plural attributes.
 *
 * @author Steve Ebersole
 */
public class SqmPluralAttributeReference
		extends AbstractSqmAttributeReference<PluralPersistentAttribute>
		implements SqmNavigableContainerReference {

	private final SqmNavigableJoin navigableJoin;

	public SqmPluralAttributeReference(
			SqmNavigableContainerReference containerReference,
			SqmNavigableJoin navigableJoin,
			PluralPersistentAttribute attribute,
			SqmCreationState creationState) {
		super( containerReference, attribute );

		this.navigableJoin = navigableJoin;
	}

	@Override
	public SqmNavigableJoin getExportedFromElement() {
		return navigableJoin;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getPersistentCollectionDescriptor().getPersistenceType();
	}

	@Override
	public String getUniqueIdentifier() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public String getIdentificationVariable() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		throw new NotYetImplementedException(  );
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitPluralAttribute( this );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		return getReferencedNavigable().getPersistentCollectionDescriptor()
				.findNavigable( name )
				.createSqmExpression( this, creationState );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		return null;
	}
}

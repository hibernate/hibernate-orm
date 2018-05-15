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
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
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

	private final SqmNavigableJoin join;

	public SqmPluralAttributeReference(
			SqmNavigableContainerReference containerReference,
			PluralPersistentAttribute attribute,
			SqmCreationContext creationContext) {
		super( containerReference, attribute );

		this.join = creationContext.getCurrentFromElementBuilder().buildNavigableJoin( this );
	}

	@Override
	public SqmNavigableJoin getExportedFromElement() {
		return join;
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
			SqmCreationContext context) {
		return getReferencedNavigable().getPersistentCollectionDescriptor()
				.findNavigable( name )
				.createSqmExpression( getExportedFromElement(), this, context );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		return null;
	}
}

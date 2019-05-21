/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.mapping.spi.Navigable;
import org.hibernate.metamodel.model.mapping.spi.NavigableContainer;
import org.hibernate.metamodel.model.mapping.PersistentCollectionDescriptor;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmMinElementPath<T> extends AbstractSqmSpecificPluralPartPath<T> {
	public static final String NAVIGABLE_NAME = "{min-element}";

	public SqmMinElementPath(SqmPath<?> pluralDomainPath) {
		super(
				pluralDomainPath.getNavigablePath().append( NAVIGABLE_NAME ),
				pluralDomainPath,
				pluralDomainPath.sqmAs( PersistentCollectionDescriptor.class ).getElementDescriptor()
		);
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		if ( getReferencedPathSource() instanceof NavigableContainer<?> ) {
			final Navigable subNavigable = ( (NavigableContainer) getReferencedPathSource() ).findNavigable( name );
			getPluralDomainPath().prepareForSubNavigableReference( subNavigable, isTerminal, creationState );
			return subNavigable.createSqmExpression( this, creationState );
		}

		throw new SemanticException( "Collection element cannot be de-referenced : " + getPluralDomainPath().getNavigablePath() );
	}

	@Override
	public SqmPathSource<?, T> getReferencedPathSource() {
		return getCollectionDescriptor().getElementDescriptor();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitMinElementPath( this );
	}
}

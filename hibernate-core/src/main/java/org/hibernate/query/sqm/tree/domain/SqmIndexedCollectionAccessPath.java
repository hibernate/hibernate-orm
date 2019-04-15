/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralValuedNavigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmIndexedCollectionAccessPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final SqmExpression<?> selectorExpression;
	private final PersistentCollectionDescriptor<?,?,T> collectionDescriptor;


	public SqmIndexedCollectionAccessPath(
			SqmPath<?> pluralDomainPath,
			SqmExpression<?> selectorExpression) {
		super(
				pluralDomainPath.getNavigablePath().append( "[]" ),
				pluralDomainPath.sqmAs( PluralValuedNavigable.class ).getCollectionDescriptor().getElementDescriptor(),
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.selectorExpression = selectorExpression;

		this.collectionDescriptor = pluralDomainPath.sqmAs( PluralValuedNavigable.class ).getCollectionDescriptor();
	}

	public SqmExpression getSelectorExpression() {
		return selectorExpression;
	}

	@Override
	public NavigablePath getNavigablePath() {
		// todo (6.0) : this would require some String-ified form of the selector
		return null;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		final Navigable subNavigable = ( (NavigableContainer) collectionDescriptor.getElementDescriptor() )
				.findNavigable( name );

		return subNavigable.createSqmExpression( this, creationState );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitIndexedPluralAccessPath( this );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		if ( getReferencedNavigable() instanceof EntityValuedNavigable ) {
			throw new NotYetImplementedFor6Exception();
		}

		throw new UnsupportedOperationException(  );
	}
}

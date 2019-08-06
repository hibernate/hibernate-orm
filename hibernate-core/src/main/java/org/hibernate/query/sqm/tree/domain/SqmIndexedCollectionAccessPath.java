/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.PathException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class SqmIndexedCollectionAccessPath<T> extends AbstractSqmPath<T> implements SqmPath<T> {
	private final SqmExpression<?> selectorExpression;


	public SqmIndexedCollectionAccessPath(
			SqmPath<?> pluralDomainPath,
			SqmExpression<?> selectorExpression) {
		//noinspection unchecked
		super(
				pluralDomainPath.getNavigablePath().append( "[]" ),
				(PluralPersistentAttribute) pluralDomainPath.getReferencedPathSource(),
				pluralDomainPath,
				pluralDomainPath.nodeBuilder()
		);
		this.selectorExpression = selectorExpression;

	}

	public SqmExpression getSelectorExpression() {
		return selectorExpression;
	}

	@Override
	public PluralPersistentAttribute<?,?,T> getReferencedPathSource() {
		//noinspection unchecked
		return (PluralPersistentAttribute) super.getReferencedPathSource();
	}

	@Override
	public NavigablePath getNavigablePath() {
		// todo (6.0) : this would require some String-ified form of the selector
		return null;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPathSource subPathSource = getReferencedPathSource().getElementPathSource().findSubPathSource( name );
		//noinspection unchecked
		return subPathSource.createSqmPath( this, creationState );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitIndexedPluralAccessPath( this );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(Class<S> treatJavaType) throws PathException {
		return treatAs( nodeBuilder().getDomainModel().entity( treatJavaType ) );
	}

	@Override
	public <S extends T> SqmTreatedPath<T, S> treatAs(EntityDomainType<S> treatTarget) throws PathException {
		if ( getReferencedPathSource().getSqmPathType() instanceof EntityDomainType ) {
			//noinspection unchecked
			return new SqmTreatedSimplePath( this, treatTarget, nodeBuilder() );
		}

		throw new UnsupportedOperationException(  );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.criteria.PathException;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEntityValuedSimplePath<T> extends AbstractSqmSimplePath<T> {
	public SqmEntityValuedSimplePath(
			NavigablePath navigablePath,
			EntityValuedNavigable<T> referencedNavigable,
			SqmPath lhs,
			NodeBuilder nodeBuilder) {
		super( navigablePath, referencedNavigable, lhs, nodeBuilder );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		final EntityValuedNavigable referencedNavigable = getReferencedNavigable();
		final Navigable navigable = referencedNavigable.findNavigable( name );

		prepareForSubNavigableReference( referencedNavigable, isTerminal, creationState );

		assert getLhs() == null || creationState.getProcessingStateStack()
				.getCurrent()
				.getPathRegistry()
				.findPath( getLhs().getNavigablePath() ) != null;

		return navigable.createSqmExpression( this, creationState );
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitEntityValuedPath( this );
	}

	@Override
	public EntityValuedNavigable<T> getReferencedNavigable() {
		return (EntityValuedNavigable<T>) super.getReferencedNavigable();
	}

	private boolean dereferenced;

	@Override
	public void prepareForSubNavigableReference(
			Navigable subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		if ( dereferenced ) {
			// nothing to do, already dereferenced
			return;
		}

		log.tracef(
				"`SqmEntityValuedSimplePath#prepareForSubNavigableReference` : %s -> %s",
				getNavigablePath().getFullPath(),
				subNavigable.getNavigableName()
		);

		SqmCreationHelper.resolveAsLhs( getLhs(), this, subNavigable, isSubReferenceTerminal, creationState );

		dereferenced = true;
	}

	@Override
	public EntityValuedNavigable<T> getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public EntityJavaDescriptor<T> getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends T> SqmTreatedSimplePath<T,S> treatAs(Class<S> treatJavaType) throws PathException {
		final EntityTypeDescriptor<S> treatTargetDescriptor = nodeBuilder().getDomainModel().entity( treatJavaType );
		return new SqmTreatedSimplePath(
				this,
				treatTargetDescriptor,
				nodeBuilder()
		);
	}

}

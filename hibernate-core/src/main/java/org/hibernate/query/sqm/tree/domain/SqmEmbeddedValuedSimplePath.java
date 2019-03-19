/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.EmbeddedValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.SqmCreationHelper;
import org.hibernate.query.sqm.produce.SqmPathRegistry;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEmbeddedValuedSimplePath extends AbstractSqmSimplePath {
	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			EmbeddedValuedNavigable referencedNavigable,
			SqmPath lhs) {
		super( navigablePath, referencedNavigable, lhs );
	}

	public SqmEmbeddedValuedSimplePath(
			NavigablePath navigablePath,
			EmbeddedValuedNavigable referencedNavigable,
			SqmPath lhs, String explicitAlias) {
		super( navigablePath, referencedNavigable, lhs, explicitAlias );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		final Navigable subNavigable = getReferencedNavigable().findNavigable( name );

		prepareForSubNavigableReference( subNavigable, isTerminal, creationState );
		return subNavigable.createSqmExpression(
				this,
				creationState
		);
	}

	@Override
	public EmbeddedValuedNavigable getReferencedNavigable() {
		return (EmbeddedValuedNavigable) super.getReferencedNavigable();
	}

	@Override
	public EmbeddedValuedNavigable getExpressableType() {
		return (EmbeddedValuedNavigable) super.getExpressableType();
	}

	@Override
	public Supplier<? extends EmbeddedValuedNavigable> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public EmbeddableJavaDescriptor getJavaTypeDescriptor() {
		return (EmbeddableJavaDescriptor) super.getJavaTypeDescriptor();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEmbeddableValuedPath( this );
	}

	private boolean dereferenced;

	@Override
	public void prepareForSubNavigableReference(
			Navigable subNavigable,
			boolean isSubReferenceTerminal,
			SqmCreationState creationState) {
		if ( dereferenced ) {
			// nothing to do
			return;
		}

		SqmCreationHelper.resolveAsLhs( getLhs(), this, subNavigable, isSubReferenceTerminal, creationState );

		final SqmPathRegistry pathRegistry = creationState.getProcessingStateStack().getCurrent().getPathRegistry();

		pathRegistry.resolvePath(
				getNavigablePath(),
				navigablePath -> {
					final NavigablePath lhsNavigablePath = getLhs().getNavigablePath();
					final SqmFrom lhsFrom = pathRegistry.findFromByPath( lhsNavigablePath );
					if ( lhsFrom == null ) {
						throw new ParsingException( "Unable to resolve SqmFrom : `" + lhsNavigablePath.getFullPath() + '`' );
					}
					return lhsFrom;
				}
		);

		dereferenced = true;
	}

//	@Override
//	public DomainResult createDomainResult(
//			String resultVariable,
//			DomainResultCreationState creationState,
//			DomainResultCreationContext creationContext) {
//		return new CompositeResultImpl( getNavigablePath(), getReferencedNavigable(), resultVariable, creationState );
//	}
}

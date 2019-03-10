/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import java.util.function.Supplier;

import org.hibernate.annotations.Remove;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;

/**
 * @author Steve Ebersole
 */
@Remove
@Deprecated
public class SqmDiscriminatorReference extends AbstractSqmNavigableReference implements SqmNavigableReference {
	private final SqmEntityTypedReference entityReference;

	private final DiscriminatorDescriptor<?> discriminatorDescriptor;
	private final NavigablePath navigablePath;

	public SqmDiscriminatorReference(SqmEntityTypedReference entityReference) {
		this.entityReference = entityReference;

		this.discriminatorDescriptor = entityReference.getReferencedNavigable()
				.getEntityDescriptor()
				.getHierarchy()
				.getDiscriminatorDescriptor();

		this.navigablePath = entityReference.getNavigablePath().append( DiscriminatorDescriptor.NAVIGABLE_NAME );
	}

	@Override
	public SqmEntityTypedReference getSourceReference() {
		return entityReference;
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return entityReference;
	}

	@Override
	public Navigable getReferencedNavigable() {
		return discriminatorDescriptor;
	}

	@Override
	public SqmPath getLhs() {
		return entityReference;
	}

	@Override
	public String getUniqueIdentifier() {
		return entityReference.getUniqueIdentifier();
	}

	@Override
	public String getIdentificationVariable() {
		return entityReference.getIdentificationVariable();
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return null;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}


	@Override
	public Class getJavaType() {
		return Class.class;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public ExpressableType getExpressableType() {
		return discriminatorDescriptor;
	}

	@Override
	public Supplier<? extends ExpressableType> getInferableType() {
		return () -> discriminatorDescriptor;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitDiscriminatorReference( this );
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return discriminatorDescriptor.getJavaTypeDescriptor();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return entityReference.getExportedFromElement();
	}
}

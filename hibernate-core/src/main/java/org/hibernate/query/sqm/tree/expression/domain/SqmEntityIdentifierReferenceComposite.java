/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierComposite;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmEntityIdentifierReferenceComposite
		extends AbstractSqmNavigableReference
		implements SqmEntityIdentifierReference, SqmEmbeddableTypedReference {

	private final SqmEntityTypedReference sourceBinding;
	private final EntityIdentifierComposite navigable;

	public SqmEntityIdentifierReferenceComposite(SqmEntityTypedReference sourceBinding, EntityIdentifierComposite navigable) {
		this.sourceBinding = sourceBinding;
		this.navigable = navigable;
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return sourceBinding.getExportedFromElement();
	}

	@Override
	public SqmNavigableContainerReference getSourceReference() {
		return sourceBinding;
	}

	@Override
	public EntityIdentifierComposite getReferencedNavigable() {
		return navigable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return getSourceReference().getNavigablePath().append( getReferencedNavigable().getNavigableName() );
	}

	@Override
	public EntityIdentifierComposite getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public EntityIdentifierComposite getInferableType() {
		return getReferencedNavigable();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityIdentifierReference( this );
	}

	@Override
	public String asLoggableText() {
		return navigable.asLoggableText();
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return this;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return navigable.getJavaTypeDescriptor();
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
	public String getUniqueIdentifier() {
		throw new RuntimeException( "These methods need to be re-thought" );
	}

	@Override
	public String getIdentificationVariable() {
		throw new RuntimeException( "These methods need to be re-thought" );
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		throw new RuntimeException( "These methods need to be re-thought" );
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		return getReferencedNavigable().getEmbeddedDescriptor()
				.findNavigable( name )
				.createSqmExpression( getExportedFromElement(), this, context );
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

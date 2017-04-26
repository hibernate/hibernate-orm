/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.entity.spi.IdentifierDescriptorComposite;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmFrom;

/**
 * @author Steve Ebersole
 */
public class SqmEntityIdentifierReferenceComposite
		extends AbstractSqmNavigableReference
		implements SqmEntityIdentifierReference, SqmEmbeddableTypedReference {

	private final SqmEntityTypedReference sourceBinding;
	private final IdentifierDescriptorComposite navigable;

	public SqmEntityIdentifierReferenceComposite(SqmEntityTypedReference sourceBinding, IdentifierDescriptorComposite navigable) {
		this.sourceBinding = sourceBinding;
		this.navigable = navigable;
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return sourceBinding.getExportedFromElement();
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		sourceBinding.injectExportedFromElement( sqmFrom );
	}

	@Override
	public SqmNavigableSourceReference getSourceReference() {
		return sourceBinding;
	}

	@Override
	public IdentifierDescriptorComposite getReferencedNavigable() {
		return navigable;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return getSourceReference().getNavigablePath().append( getReferencedNavigable().getNavigableName() );
	}

	@Override
	public IdentifierDescriptorComposite getExpressionType() {
		return getReferencedNavigable();
	}

	@Override
	public IdentifierDescriptorComposite getInferableType() {
		return getReferencedNavigable();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityIdentifierBinding( this );
	}

	@Override
	public String asLoggableText() {
		return navigable.asLoggableText();
	}
}

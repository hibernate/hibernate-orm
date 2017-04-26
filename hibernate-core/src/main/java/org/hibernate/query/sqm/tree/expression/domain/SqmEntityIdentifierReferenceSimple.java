/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.entity.spi.IdentifierDescriptorSimple;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;

/**
 * @author Steve Ebersole
 */
public class SqmEntityIdentifierReferenceSimple
		extends AbstractSqmNavigableReference
		implements SqmEntityIdentifierReference {

	private final SqmEntityTypedReference source;
	private final IdentifierDescriptorSimple entityIdentifier;

	public SqmEntityIdentifierReferenceSimple(SqmEntityTypedReference source, IdentifierDescriptorSimple entityIdentifier) {
		this.source = source;
		this.entityIdentifier = entityIdentifier;
	}

	@Override
	public ExpressableType getExpressionType() {
		return entityIdentifier;
	}

	@Override
	public ExpressableType getInferableType() {
		return entityIdentifier;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitEntityIdentifierBinding( this );
	}

	@Override
	public String asLoggableText() {
		return entityIdentifier.asLoggableText();
	}

	@Override
	public SqmNavigableSourceReference getSourceReference() {
		return source;
	}

	@Override
	public IdentifierDescriptorSimple getReferencedNavigable() {
		return entityIdentifier;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return source.getNavigablePath().append( entityIdentifier.getNavigableName() );
	}
}

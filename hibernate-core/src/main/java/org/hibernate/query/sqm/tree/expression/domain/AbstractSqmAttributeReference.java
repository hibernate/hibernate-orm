/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmFromExporter;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSqmAttributeReference<A extends PersistentAttribute>
		extends AbstractSqmNavigableReference
		implements SqmAttributeReference, SqmFromExporter {
	private final SqmNavigableSourceReference sourceBinding;
	private final A attribute;
	private final NavigablePath propertyPath;

	private SqmAttributeJoin join;

	public AbstractSqmAttributeReference(SqmNavigableSourceReference sourceBinding, A attribute) {
		if ( sourceBinding == null ) {
			throw new IllegalArgumentException( "Source for AttributeBinding cannot be null" );
		}
		if ( attribute == null ) {
			throw new IllegalArgumentException( "Attribute for AttributeBinding cannot be null" );
		}

		this.sourceBinding = sourceBinding;
		this.attribute = attribute;

		this.propertyPath = sourceBinding.getNavigablePath().append( attribute.getAttributeName() );
	}

	@SuppressWarnings("unchecked")
	public AbstractSqmAttributeReference(SqmAttributeJoin join) {
		this(
				join.getBinding().getSourceReference(),
				(A) join.getAttributeBinding().getReferencedNavigable()
		);
		injectExportedFromElement( join );
	}

	@Override
	public void injectExportedFromElement(SqmFrom attributeJoin) {
		if ( this.join != null && this.join != attributeJoin ) {
			throw new IllegalArgumentException( "Attempting to create multiple SqmFrom references for a single AttributeBinding" );
		}
		this.join = (SqmAttributeJoin) attributeJoin;
	}

	@Override
	public SqmNavigableSourceReference getSourceReference() {
		// attribute binding must have a source
		return sourceBinding;
	}

	@Override
	public A getReferencedNavigable() {
		return attribute;
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return join;
	}

	@Override
	public ExpressableType getExpressionType() {
		return getReferencedNavigable();
	}

	@Override
	public ExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitAttributeReferenceExpression( this );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public String asLoggableText() {
		if ( join == null || join.getIdentificationVariable() == null ) {
			return getClass().getSimpleName() + '(' + sourceBinding.asLoggableText() + '.' + attribute.getAttributeName() + ")";
		}
		else {
			return getClass().getSimpleName() + '(' + sourceBinding.asLoggableText() + '.' + attribute.getAttributeName() + " : " + join.getIdentificationVariable() + ")";
		}
	}
}

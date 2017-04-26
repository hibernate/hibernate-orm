/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.queryable.spi.EntityValuedExpressableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmEntityReference extends AbstractSqmNavigableReference
		implements SqmNavigableReference, SqmNavigableSourceReference, SqmEntityTypedReference {
	private static final Logger log = Logger.getLogger( SqmEntityReference.class );

	private final SqmNavigableSourceReference sourceBinding;
	private final EntityValuedExpressableType entityReference;
	private NavigablePath propertyPath;

	private SqmFrom exportedFromElement;

	public SqmEntityReference(EntityValuedExpressableType entityReference) {
		this.entityReference = entityReference;
		this.sourceBinding = null;
		this.propertyPath = new NavigablePath( null, entityReference.getEntityName() );
	}

	public SqmEntityReference(SqmNavigableSourceReference sourceBinding, EntityValuedExpressableType entityReference) {
		this.sourceBinding = sourceBinding;
		this.entityReference = entityReference;

		this.propertyPath = new NavigablePath( sourceBinding.getNavigablePath(), entityReference.getEntityName() );
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		log.debugf(
				"Injecting SqmFrom [%s] into EntityBindingImpl [%s], was [%s]",
				sqmFrom,
				this,
				this.exportedFromElement
		);
		exportedFromElement = sqmFrom;
		propertyPath = new NavigablePath( null, entityReference.getEntityName() + "(" + sqmFrom.getIdentificationVariable() + ")" );
	}

	@Override
	public SqmNavigableSourceReference getSourceReference() {
		return sourceBinding;
	}

	@Override
	public EntityValuedExpressableType getReferencedNavigable() {
		return entityReference;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public EntityValuedExpressableType getExpressionType() {
		return entityReference;
	}

	@Override
	public EntityValuedExpressableType getInferableType() {
		return getExpressionType();
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return (T) exportedFromElement.getBinding();
//		return exportedFromElement.accept( walker );
	}

	@Override
	public String asLoggableText() {
		return entityReference.asLoggableText();
	}
}

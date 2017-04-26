/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.domain.SqmExpressableTypeEntity;
import org.hibernate.query.sqm.domain.type.SqmDomainTypeEntity;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmMinElementReferenceEntity extends AbstractSpecificSqmElementReference implements SqmMinElementReference,
		SqmEntityTypedReference {
	private static final Logger log = Logger.getLogger( SqmMinElementReferenceEntity.class );

	private SqmFrom exportedFromElement;

	public SqmMinElementReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public SqmExpressableTypeEntity getExpressionType() {
		return getReferencedNavigable();
	}

	@Override
	public SqmExpressableTypeEntity getInferableType() {
		return getExpressionType();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return (SqmExpressableTypeEntity) getPluralAttributeBinding().getReferencedNavigable().getElementDescriptor();
	}

	@Override
	public SqmDomainTypeEntity getExportedDomainType() {
		return (SqmDomainTypeEntity) super.getExportedDomainType();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		log.debugf(
				"Injecting SqmFrom [%s] into MinElementBindingEntity [%s], was [%s]",
				sqmFrom,
				this,
				this.exportedFromElement
		);
		exportedFromElement = sqmFrom;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return null;
	}
}

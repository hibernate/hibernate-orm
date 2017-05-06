/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.persister.collection.spi.CollectionElementEntity;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.query.sqm.NotYetImplementedException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmCollectionElementReferenceEntity extends AbstractSqmCollectionElementReference implements
		SqmCollectionElementReference,
		SqmEntityTypedReference {
	private static final Logger log = Logger.getLogger( SqmCollectionElementReferenceEntity.class );

	private SqmFrom exportedFromElement;

	public SqmCollectionElementReferenceEntity(SqmPluralAttributeReference pluralAttributeBinding) {
		super( pluralAttributeBinding );
	}

	@Override
	public CollectionElementEntity getExpressionType() {
		return getReferencedNavigable();
	}

	@Override
	public CollectionElementEntity getInferableType() {
		return getExpressionType();
	}

	@Override
	public CollectionElementEntity getReferencedNavigable() {
		return (CollectionElementEntity) super.getReferencedNavigable();
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return exportedFromElement;
	}

	@Override
	public void injectExportedFromElement(SqmFrom sqmFrom) {
		log.debugf(
				"Injecting SqmFrom [%s] into CollectionElementBindingEntity [%s], was [%s]",
				sqmFrom,
				this,
				this.exportedFromElement
		);
		exportedFromElement = sqmFrom;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		throw new NotYetImplementedException(  );
	}

	@Override
	public EntityPersister getIntrinsicSubclassEntityPersister() {
		// todo (6.0) : override this to account for implicit or explicit Downcasts
		return super.getIntrinsicSubclassEntityPersister();
	}

	@Override
	public String getUniqueIdentifier() {
		// todo (6.0) : for the entity element classification we should point to the referenced entity's uid
		return super.getUniqueIdentifier();
	}
}

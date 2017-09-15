/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.domain;

import org.hibernate.metamodel.model.domain.spi.CollectionElementEntity;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmFrom;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SqmIndexedElementReferenceEntity
		extends AbstractSqmIndexedElementReference
		implements SqmRestrictedCollectionElementReferenceEntity {
	private static final Logger log = Logger.getLogger( SqmIndexedElementReferenceEntity.class );

	private SqmFrom exportedFromElement;

	public SqmIndexedElementReferenceEntity(
			SqmPluralAttributeReference pluralAttributeBinding,
			SqmExpression indexSelectionExpression) {
		super( pluralAttributeBinding, indexSelectionExpression );
	}

	@Override
	public CollectionElementEntity getReferencedNavigable() {
		return (CollectionElementEntity) super.getReferencedNavigable();
	}

	@Override
	public CollectionElementEntity getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public CollectionElementEntity getInferableType() {
		return getExpressableType();
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
	public EntityDescriptor getIntrinsicSubclassEntityMetadata() {
		// todo (6.0) : override this to account for implicit or explicit Downcasts
		return super.getIntrinsicSubclassEntityMetadata();
	}

	@Override
	public String getUniqueIdentifier() {
		// todo (6.0) : for the entity index classification we should point to the referenced entity's uid
		return super.getUniqueIdentifier();
	}
}

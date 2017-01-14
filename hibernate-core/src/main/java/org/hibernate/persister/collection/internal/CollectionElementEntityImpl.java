/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.persister.collection.spi.AbstractCollectionElement;
import org.hibernate.persister.collection.spi.CollectionElementEntity;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.domain.SqmNavigable;
import org.hibernate.sqm.domain.SqmPluralAttributeElement;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEntityImpl
		extends AbstractCollectionElement<EntityType>
		implements CollectionElementEntity {

	public CollectionElementEntityImpl(
			CollectionPersister persister,
			EntityType ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	public EntityType getExportedDomainType() {
		return (EntityType) super.getExportedDomainType();
	}

	@Override
	public EntityPersister getEntityPersister() {
		return getOrmType().getEntityPersister();
	}

	@Override
	public SqmNavigable findNavigable(String navigableName) {
		return getEntityPersister().findNavigable( navigableName );
	}

	@Override
	public String getEntityName() {
		return getEntityPersister().getEntityName();
	}

	@Override
	public String getJpaEntityName() {
		return getEntityPersister().getJpaEntityName();
	}

	@Override
	public SqmPluralAttributeElement.ElementClassification getClassification() {
		return SqmPluralAttributeElement.ElementClassification.ONE_TO_MANY;
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.ENTITY;
	}
}

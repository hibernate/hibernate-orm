/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;
import javax.persistence.metamodel.Type;

import org.hibernate.persister.collection.spi.AbstractCollectionIndex;
import org.hibernate.persister.collection.spi.CollectionIndexEntity;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.type.spi.EntityType;
import org.hibernate.sqm.domain.SqmNavigable;
import org.hibernate.sqm.domain.SqmPluralAttributeIndex;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEntityImpl
		extends AbstractCollectionIndex<EntityType>
		implements CollectionIndexEntity {
	public CollectionIndexEntityImpl(
			CollectionPersister persister,
			EntityType ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	public SqmNavigable findNavigable(String navigableName) {
		return getEntityPersister().findNavigable( navigableName );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return getOrmType().getEntityPersister();
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
	public SqmPluralAttributeIndex.IndexClassification getClassification() {
		// todo : distinguish between OneToMany and ManyToMany
		return SqmPluralAttributeIndex.IndexClassification.ONE_TO_MANY;
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.ENTITY;
	}
}

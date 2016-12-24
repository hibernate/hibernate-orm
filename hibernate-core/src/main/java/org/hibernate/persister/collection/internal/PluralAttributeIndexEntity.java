/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.collection.internal;

import java.util.List;
import java.util.Optional;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.AbstractPluralAttributeIndex;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.EntityType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexEntity extends AbstractPluralAttributeIndex<EntityType> {
	private final EntityPersister indexEntityPersister;

	public PluralAttributeIndexEntity(
			CollectionPersister persister,
			EntityPersister indexEntityPersister,
			EntityType ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
		this.indexEntityPersister = indexEntityPersister;
	}

	@Override
	public IndexClassification getClassification() {
		return IndexClassification.ONE_TO_MANY;
	}

	@Override
	public DomainReference getType() {
		return this;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.of( indexEntityPersister );
	}
}

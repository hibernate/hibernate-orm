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
import org.hibernate.persister.collection.spi.CollectionIndexEmbedded;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.sqm.domain.SqmNavigable;
import org.hibernate.sqm.domain.SqmPluralAttributeIndex;

/**
 * @author Steve Ebersole
 */
public class CollectionIndexEmbeddedImpl
		extends AbstractCollectionIndex<EmbeddedType>
		implements CollectionIndexEmbedded {
	public CollectionIndexEmbeddedImpl(
			CollectionPersister persister,
			EmbeddedType ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	public EmbeddedPersister getEmbeddablePersister() {
		return getOrmType().getEmbeddablePersister();
	}

	@Override
	public Type.PersistenceType getPersistenceType() {
		return Type.PersistenceType.EMBEDDABLE;
	}

	@Override
	public SqmNavigable findNavigable(String navigableName) {
		return getEmbeddablePersister().findNavigable( navigableName );
	}

	@Override
	public SqmPluralAttributeIndex.IndexClassification getClassification() {
		return SqmPluralAttributeIndex.IndexClassification.EMBEDDABLE;
	}
}

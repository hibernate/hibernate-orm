/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.internal;

import java.util.List;

import org.hibernate.persister.collection.spi.AbstractCollectionElement;
import org.hibernate.persister.collection.spi.CollectionElementEmbedded;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.sqm.domain.SqmNavigable;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class CollectionElementEmbeddedImpl
		extends AbstractCollectionElement<EmbeddedType>
		implements CollectionElementEmbedded  {
	public CollectionElementEmbeddedImpl(
			CollectionPersister persister,
			EmbeddedType ormType,
			List<Column> columns) {
		super( persister, ormType, columns );
	}

	@Override
	public CollectionPersister getSource() {
		return super.getSource();
	}

	@Override
	public EmbeddedType getExportedDomainType() {
		return (EmbeddedType) super.getExportedDomainType();
	}

	@Override
	public SqmNavigable findNavigable(String navigableName) {
		return getEmbeddablePersister().findNavigable( navigableName );
	}

	@Override
	public EmbeddedPersister getEmbeddablePersister() {
		return getOrmType().getEmbeddablePersister();
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.collection.internal;

import java.util.Optional;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.internal.CompositeContainer;
import org.hibernate.persister.common.internal.CompositeReference;
import org.hibernate.persister.common.spi.AbstractPluralAttributeIndex;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.sql.convert.spi.TableGroupProducer;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexEmbeddable extends AbstractPluralAttributeIndex<CompositeType>
		implements CompositeReference {
	private final EmbeddablePersister embeddablePersister;

	public PluralAttributeIndexEmbeddable(CollectionPersister persister, EmbeddablePersister embeddablePersister) {
		super( persister, embeddablePersister.getOrmType(),embeddablePersister.collectColumns() );
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public CompositeContainer getCompositeContainer() {
		return embeddablePersister;
	}

	@Override
	public EmbeddablePersister getEmbeddablePersister() {
		return embeddablePersister;
	}

	@Override
	public IndexClassification getClassification() {
		return IndexClassification.EMBEDDABLE;
	}

	@Override
	public DomainReference getType() {
		return this;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}

	@Override
	public TableGroupProducer resolveTableGroupProducer() {
		return getCompositeContainer().resolveTableGroupProducer();
	}

	@Override
	public boolean canCompositeContainCollections() {
		return false;
	}
}

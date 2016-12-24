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
import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.common.internal.CompositeContainer;
import org.hibernate.persister.common.internal.CompositeReference;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.sql.convert.spi.TableGroupProducer;
import org.hibernate.sqm.domain.DomainReference;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementEmbeddable implements PluralAttributeElement<CompositeType>, CompositeReference {
	private final CollectionPersister collectionPersister;
	private final EmbeddablePersister embeddablePersister;

	public PluralAttributeElementEmbeddable(CollectionPersister collectionPersister, EmbeddablePersister embeddablePersister) {
		this.collectionPersister = collectionPersister;
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public CompositeContainer getCompositeContainer() {
		return collectionPersister;
	}

	@Override
	public EmbeddablePersister getEmbeddablePersister() {
		return embeddablePersister;
	}

	@Override
	public ElementClassification getClassification() {
		return ElementClassification.EMBEDDABLE;
	}

	@Override
	public CompositeType getOrmType() {
		return embeddablePersister.getOrmType();
	}

	@Override
	public DomainReference getType() {
		return this;
	}

	@Override
	public String asLoggableText() {
		return "PluralAttributeElement(" + collectionPersister.getRole() + " [" + getOrmType().getName() + "])" ;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}

	@Override
	public List<Column> getColumns() {
		return embeddablePersister.collectColumns();
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

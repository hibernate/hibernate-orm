/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.collection.spi.PluralAttributeElement;
import org.hibernate.persister.embeddable.EmbeddablePersister;
import org.hibernate.sqm.domain.EmbeddableType;
import org.hibernate.sqm.domain.PluralAttribute;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementEmbeddable implements PluralAttributeElement<CompositeType, EmbeddableType> {
	private final EmbeddablePersister embeddablePersister;

	public PluralAttributeElementEmbeddable(EmbeddablePersister embeddablePersister) {
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public PluralAttribute.ElementClassification getElementClassification() {
		return PluralAttribute.ElementClassification.EMBEDDABLE;
	}

	@Override
	public CompositeType getOrmType() {
		return embeddablePersister.getOrmType();
	}

	@Override
	public EmbeddableType getSqmType() {
		return embeddablePersister;
	}

	public EmbeddablePersister getEmbeddablePersister() {
		return embeddablePersister;
	}
}

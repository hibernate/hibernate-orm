/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.internal;

import org.hibernate.persister.collection.spi.PluralAttributeIndex;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.sqm.domain.EmbeddableType;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexEmbeddable implements PluralAttributeIndex<CompositeType, EmbeddableType> {
	private final EmbeddablePersister embeddablePersister;

	public PluralAttributeIndexEmbeddable(EmbeddablePersister embeddablePersister) {
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public CompositeType getOrmType() {
		return embeddablePersister.getOrmType();
	}

	@Override
	public EmbeddableType getSqmType() {
		return (EmbeddableType) embeddablePersister.asManagedType();
	}

	@Override
	public Column[] getColumns() {
		return embeddablePersister.collectColumns();
	}
}

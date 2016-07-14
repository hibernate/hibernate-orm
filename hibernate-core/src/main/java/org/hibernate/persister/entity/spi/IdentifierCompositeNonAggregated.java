/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.embeddable.EmbeddablePersister;
import org.hibernate.persister.entity.spi.IdentifierDescriptorImplementor;
import org.hibernate.sqm.domain.Type;

/**
 * @author Steve Ebersole
 */
public class IdentifierCompositeNonAggregated implements IdentifierDescriptorImplementor {
	// todo : IdClass handling eventually

	private final EmbeddablePersister embeddablePersister;

	public IdentifierCompositeNonAggregated(EmbeddablePersister embeddablePersister) {
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public Column[] getColumns() {
		return embeddablePersister.collectColumns();
	}

	@Override
	public Type getIdType() {
		return embeddablePersister;
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return false;
	}

	@Override
	public String getReferableAttributeName() {
		return null;
	}
}

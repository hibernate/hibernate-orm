/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import org.hibernate.persister.embedded.spi.EmbeddedReference;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public interface CollectionElementEmbedded<J> extends CollectionElement<J,EmbeddedType<J>>, EmbeddedReference<J> {
	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	EmbeddedType<J> getOrmType();

	@Override
	default ElementClassification getClassification() {
		return ElementClassification.EMBEDDABLE;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.sqm.domain.SqmPluralAttribute;

/**
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<O,C,E>
		extends PersistentAttribute<O,C>, NavigableSource<E>, TypeExporter, javax.persistence.metamodel.PluralAttribute<O,C,E>, SqmPluralAttribute {
	@Override
	org.hibernate.type.spi.CollectionType getOrmType();

	CollectionPersister<O,C,E> getCollectionPersister();
}

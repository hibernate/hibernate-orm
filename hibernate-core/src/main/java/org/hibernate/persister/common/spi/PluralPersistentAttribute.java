/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.collection.spi.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public interface PluralPersistentAttribute<O,C,E>
		extends PersistentAttribute<O,C>, NavigableSource<C>, TypeExporter<C>, javax.persistence.metamodel.PluralAttribute<O,C,E> {
	@Override
	org.hibernate.type.spi.CollectionType<O,C,E> getOrmType();

	CollectionPersister<O,C,E> getCollectionPersister();
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.CollectionType;

/**
 * @author Steve Ebersole
 */
public interface CollectionDefinition {
	public CollectionPersister getCollectionPersister();
	public CollectionType getCollectionType();

	public CollectionIndexDefinition getIndexDefinition();
	public CollectionElementDefinition getElementDefinition();
}

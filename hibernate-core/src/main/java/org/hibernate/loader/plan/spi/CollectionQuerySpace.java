/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.persister.collection.CollectionPersister;

/**
 * Models a QuerySpace for a persistent collection.
 * <p/>
 * It's {@link #getDisposition()} result will be {@link Disposition#COLLECTION}
 *
 * @author Steve Ebersole
 */
public interface CollectionQuerySpace extends QuerySpace {
	/**
	 * Retrieve the collection persister this QuerySpace refers to.
	 *
	 * @return The collection persister.
	 */
	public CollectionPersister getCollectionPersister();
}

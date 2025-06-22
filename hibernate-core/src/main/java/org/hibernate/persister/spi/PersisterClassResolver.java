/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.spi;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.service.Service;

/**
 * Given an entity or collection mapping, resolve the appropriate persister class to use.
 * <p>
 * The persister class is chosen according to the following rules:<ol>
 *     <li>the persister class defined explicitly via annotation or XML</li>
 *     <li>the persister class returned by the installed {@link PersisterClassResolver}</li>
 *     <li>the default provider as chosen by Hibernate Core (best choice most of the time)</li>
 * </ol>
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public interface PersisterClassResolver extends Service {
	/**
	 * Returns the entity persister class for a given entityName or null
	 * if the entity persister class should be the default.
	 *
	 * @param metadata The entity metadata
	 *
	 * @return The entity persister class to use
	 */
	Class<? extends EntityPersister> getEntityPersisterClass(PersistentClass metadata);

	/**
	 * Returns the collection persister class for a given collection role or null
	 * if the collection persister class should be the default.
	 *
	 * @param metadata The collection metadata
	 *
	 * @return The collection persister class to use
	 */
	Class<? extends CollectionPersister> getCollectionPersisterClass(Collection metadata);
}

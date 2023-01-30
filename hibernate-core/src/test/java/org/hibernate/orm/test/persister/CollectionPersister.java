/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.persister;

import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionDataAccess;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.collection.OneToManyPersister;

/**
 * @author Shawn Clowater
 */
//tag::entity-persister-mapping[]

public class CollectionPersister
        extends OneToManyPersister {

    public CollectionPersister(
            Collection collectionBinding,
            CollectionDataAccess cacheAccessStrategy,
            RuntimeModelCreationContext creationContext)
            throws MappingException, CacheException {
        super(collectionBinding, cacheAccessStrategy, creationContext);
    }
}
//end::entity-persister-mapping[]

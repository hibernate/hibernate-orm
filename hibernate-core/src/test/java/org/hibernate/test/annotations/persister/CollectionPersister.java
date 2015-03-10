package org.hibernate.test.annotations.persister;

import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.collection.OneToManyPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * @author Shawn Clowater
 */
public class CollectionPersister extends OneToManyPersister {
	public CollectionPersister(
			Collection collectionBinding,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			PersisterCreationContext creationContext) throws MappingException, CacheException {
		super( collectionBinding, cacheAccessStrategy, creationContext );
	}
}

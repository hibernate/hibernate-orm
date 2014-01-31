package org.hibernate.test.annotations.persister;

import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.persister.collection.OneToManyPersister;

/**
 * @author Shawn Clowater
 */
public class CollectionPersister extends OneToManyPersister {
	public CollectionPersister(
			AbstractPluralAttributeBinding collection,
			CollectionRegionAccessStrategy cacheAccessStrategy,
			MetadataImplementor metadataImplementor,
			SessionFactoryImplementor factory) throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, metadataImplementor, factory );
	}
}

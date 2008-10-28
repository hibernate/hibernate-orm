package org.hibernate.test.annotations.persister;

import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.access.CollectionRegionAccessStrategy;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.persister.collection.OneToManyPersister;

/**
 * @author Shawn Clowater
 */
public class CollectionPersister extends OneToManyPersister {
	public CollectionPersister(Collection collection, CollectionRegionAccessStrategy cache, Configuration cfg,
							   SessionFactoryImplementor factory) throws MappingException, CacheException {
		super( collection, cache, cfg, factory );
	}
}
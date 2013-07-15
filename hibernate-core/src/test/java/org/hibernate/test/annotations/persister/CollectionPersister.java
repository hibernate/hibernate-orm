package org.hibernate.test.annotations.persister;

import org.hibernate.MappingException;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.spi.access.CollectionRegionAccessStrategy;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.metamodel.spi.binding.AbstractPluralAttributeBinding;
import org.hibernate.persister.collection.OneToManyPersister;

/**
 * @author Shawn Clowater
 */
public class CollectionPersister extends OneToManyPersister {
	@SuppressWarnings( {"UnusedDeclaration"})
	public CollectionPersister(Collection collection, CollectionRegionAccessStrategy cache,
							   SessionFactoryImplementor factory) throws MappingException, CacheException {
		super( collection, cache,  factory );
	}

	public CollectionPersister(AbstractPluralAttributeBinding collection, CollectionRegionAccessStrategy cacheAccessStrategy, SessionFactoryImplementor factory)
			throws MappingException, CacheException {
		super( collection, cacheAccessStrategy, factory );
	}
}

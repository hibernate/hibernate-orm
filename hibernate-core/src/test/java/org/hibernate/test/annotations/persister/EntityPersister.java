package org.hibernate.test.annotations.persister;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * @author Shawn Clowater
 */
public class EntityPersister extends SingleTableEntityPersister {
	public EntityPersister(
			PersistentClass persistentClass,
			EntityRegionAccessStrategy cache,
			NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
			PersisterCreationContext creationContext) throws HibernateException {
		super( persistentClass, cache, naturalIdRegionAccessStrategy, creationContext );
	}
}

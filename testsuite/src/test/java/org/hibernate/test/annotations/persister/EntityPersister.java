package org.hibernate.test.annotations.persister;

import org.hibernate.HibernateException;
import org.hibernate.cache.CacheConcurrencyStrategy;
import org.hibernate.cache.access.EntityRegionAccessStrategy;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;

/**
 * @author Shawn Clowater
 */
public class EntityPersister extends SingleTableEntityPersister {
	public EntityPersister(PersistentClass persistentClass, EntityRegionAccessStrategy cache,
						   SessionFactoryImplementor factory, Mapping cfg) throws HibernateException {
		super( persistentClass, cache, factory, cfg );
	}
}
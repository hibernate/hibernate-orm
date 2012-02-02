package org.hibernate.test.annotations.persister;
import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityRegionAccessStrategy;
import org.hibernate.cache.spi.access.NaturalIdRegionAccessStrategy;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;

/**
 * @author Shawn Clowater
 */
public class EntityPersister extends SingleTableEntityPersister {
	public EntityPersister(PersistentClass persistentClass, EntityRegionAccessStrategy cache,
							NaturalIdRegionAccessStrategy naturalIdRegionAccessStrategy,
						   SessionFactoryImplementor factory,  Mapping cfg) throws HibernateException {
		super( persistentClass, cache, naturalIdRegionAccessStrategy, factory, cfg );
	}
}
package org.hibernate.engine.spi;

import org.hibernate.cfg.Configuration;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.service.spi.SessionFactoryServiceInitiator;
import org.hibernate.internal.CacheImpl;

/**
 * @author Strong Liu <stliu@hibernate.org>
 */
public class CacheInitiator implements SessionFactoryServiceInitiator<CacheImplementor> {
	public static final CacheInitiator INSTANCE = new CacheInitiator();

	@Override
	public CacheImplementor initiateService(SessionFactoryImplementor sessionFactory, Configuration configuration, ServiceRegistryImplementor registry) {
		return new CacheImpl( sessionFactory );
	}

	@Override
	public CacheImplementor initiateService(SessionFactoryImplementor sessionFactory, MetadataImplementor metadata, ServiceRegistryImplementor registry) {
		return new CacheImpl( sessionFactory );
	}

	@Override
	public Class<CacheImplementor> getServiceInitiated() {
		return CacheImplementor.class;
	}
}

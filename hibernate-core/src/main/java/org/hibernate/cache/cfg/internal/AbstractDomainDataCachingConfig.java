package org.hibernate.cache.cfg.internal;

import org.hibernate.cache.cfg.spi.DomainDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainDataCachingConfig implements DomainDataCachingConfig {
	private final AccessType accessType;

	public AbstractDomainDataCachingConfig(AccessType accessType) {
		this.accessType = accessType;
	}

	@Override
	public AccessType getAccessType() {
		return accessType;
	}
}

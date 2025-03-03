/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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

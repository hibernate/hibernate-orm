/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.cfg.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.cache.cfg.spi.EntityDataCachingConfig;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.metamodel.model.domain.NavigableRole;

/**
 * @author Steve Ebersole
 */
public class EntityDataCachingConfigImpl
		extends AbstractDomainDataCachingConfig
		implements EntityDataCachingConfig {
	private final NavigableRole navigableRole;
	private final Supplier<Comparator> versionComparatorAccess;
	private final boolean isEntityMutable;

	private final Set<NavigableRole> cachedTypes = new HashSet<>();

	public EntityDataCachingConfigImpl(
			NavigableRole rootEntityName,
			Supplier<Comparator> versionComparatorAccess,
			boolean isEntityMutable,
			AccessType accessType) {
		super( accessType );
		this.navigableRole = rootEntityName;
		this.versionComparatorAccess = versionComparatorAccess;
		this.isEntityMutable = isEntityMutable;
	}

	@Override
	public Supplier<Comparator> getVersionComparatorAccess() {
		return versionComparatorAccess;
	}

	@Override
	public boolean isMutable() {
		return isEntityMutable;
	}

	@Override
	public boolean isVersioned() {
		return getVersionComparatorAccess() != null;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Set<NavigableRole> getCachedTypes() {
		return cachedTypes;
	}

	public void addCachedType(NavigableRole typeRole) {
		cachedTypes.add( typeRole );
	}
}

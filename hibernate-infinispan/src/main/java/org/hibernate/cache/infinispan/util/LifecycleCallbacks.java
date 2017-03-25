/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.infinispan.util;

import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.lifecycle.AbstractModuleLifecycle;

import java.util.Map;

public class LifecycleCallbacks extends AbstractModuleLifecycle {

	@Override
	public void cacheManagerStarting(GlobalComponentRegistry gcr, GlobalConfiguration globalCfg) {
		Map<Integer, AdvancedExternalizer<?>> externalizerMap = globalCfg.serialization().advancedExternalizers();
		externalizerMap.put( Externalizers.UUID, new Externalizers.UUIDExternalizer() );
		externalizerMap.put( Externalizers.TOMBSTONE, new Tombstone.Externalizer() );
		externalizerMap.put( Externalizers.EXCLUDE_TOMBSTONES_FILTER, new Tombstone.ExcludeTombstonesFilterExternalizer() );
		externalizerMap.put( Externalizers.TOMBSTONE_UPDATE, new TombstoneUpdate.Externalizer() );
		externalizerMap.put( Externalizers.FUTURE_UPDATE, new FutureUpdate.Externalizer() );
		externalizerMap.put( Externalizers.VERSIONED_ENTRY, new VersionedEntry.Externalizer() );
		externalizerMap.put( Externalizers.EXCLUDE_EMPTY_EXTRACT_VALUE, new VersionedEntry.ExcludeEmptyExtractValueExternalizer() );
	}

}

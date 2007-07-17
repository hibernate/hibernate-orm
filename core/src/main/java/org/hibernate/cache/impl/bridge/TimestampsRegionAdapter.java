package org.hibernate.cache.impl.bridge;

import org.hibernate.cache.TimestampsRegion;
import org.hibernate.cache.Cache;
import org.hibernate.cfg.Settings;

/**
 * Adapter specifically briding {@link TimestampsRegion} to {@link Cache}.
*
* @author Steve Ebersole
 */
public class TimestampsRegionAdapter extends BaseGeneralDataRegionAdapter implements TimestampsRegion {
	protected TimestampsRegionAdapter(Cache underlyingCache, Settings settings) {
		super( underlyingCache, settings );
	}
}

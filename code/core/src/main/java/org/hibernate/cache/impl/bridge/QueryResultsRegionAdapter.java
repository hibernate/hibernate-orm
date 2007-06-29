package org.hibernate.cache.impl.bridge;

import org.hibernate.cache.QueryResultsRegion;
import org.hibernate.cache.Cache;
import org.hibernate.cfg.Settings;

/**
 * Adapter specifically briding {@link QueryResultsRegion} to {@link Cache}.
*
* @author Steve Ebersole
 */
public class QueryResultsRegionAdapter extends BaseGeneralDataRegionAdapter implements QueryResultsRegion {
	protected QueryResultsRegionAdapter(Cache underlyingCache, Settings settings) {
		super( underlyingCache, settings );
	}
}

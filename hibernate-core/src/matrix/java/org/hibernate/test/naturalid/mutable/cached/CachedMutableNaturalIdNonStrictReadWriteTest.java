package org.hibernate.test.naturalid.mutable.cached;

import org.hibernate.cfg.Configuration;
import org.hibernate.testing.cache.CachingRegionFactory;

public class CachedMutableNaturalIdNonStrictReadWriteTest extends
		CachedMutableNaturalIdTest {

	@Override
	public void configure(Configuration cfg) {
		super.configure(cfg);
		cfg.setProperty( CachingRegionFactory.DEFAULT_ACCESSTYPE, "nonstrict-read-write" );
	}
}

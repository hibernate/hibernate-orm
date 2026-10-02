package org.hibernate.query.sqm.spi;

import jakarta.annotation.Nullable;

// Used by Hibernate Reactive
public interface InterpretationsKeySource extends CacheabilityInfluencers {
	@Nullable
	Class<?> getResultType();
}

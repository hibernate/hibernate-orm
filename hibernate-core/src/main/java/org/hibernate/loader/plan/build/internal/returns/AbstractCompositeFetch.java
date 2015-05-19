/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.build.internal.returns;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan.spi.CompositeFetch;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractCompositeFetch extends AbstractCompositeReference implements CompositeFetch {
	protected static final FetchStrategy FETCH_STRATEGY = new FetchStrategy( FetchTiming.IMMEDIATE, FetchStyle.JOIN );

	protected AbstractCompositeFetch(
			ExpandingCompositeQuerySpace compositeQuerySpace,
			boolean allowCollectionFetches,
			PropertyPath propertyPath) {
		super( compositeQuerySpace, allowCollectionFetches, propertyPath );
	}

	@Override
	public FetchStrategy getFetchStrategy() {
		return FETCH_STRATEGY;
	}

	@Override
	public String getAdditionalJoinConditions() {
		return null;
	}

	// this is being removed to be more ogm/search friendly
	@Override
	public String[] toSqlSelectFragments(String alias) {
		return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
	}
}

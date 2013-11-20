/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

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
package org.hibernate.loader.plan2.build.internal.returns;

import org.hibernate.loader.PropertyPath;
import org.hibernate.loader.plan2.build.spi.ExpandingCompositeQuerySpace;
import org.hibernate.loader.plan2.spi.CompositeFetch;
import org.hibernate.loader.plan2.spi.FetchSource;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public class CompositeFetchImpl extends AbstractCompositeFetch implements CompositeFetch {
	private final FetchSource source;

	protected CompositeFetchImpl(
			FetchSource source,
			CompositeType compositeType,
			ExpandingCompositeQuerySpace compositeQuerySpace,
			boolean allowCollectionFetches,
			PropertyPath propertyPath) {
		super( compositeType, compositeQuerySpace, allowCollectionFetches, propertyPath );
		this.source = source;
	}

	@Override
	public FetchSource getSource() {
		return source;
	}
}

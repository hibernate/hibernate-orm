/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * Support for non-aggregated composite values
 *
 * @author Steve Ebersole
 */
public class NonAggregatedCompositeSqmPathSource<J> extends AbstractSqmPathSource<J> implements CompositeSqmPathSource<J> {
	public NonAggregatedCompositeSqmPathSource(
			String localName,
			SqmPathSource<J> pathModel,
			BindableType bindableType,
			ManagedDomainType<J> container) {
		super( localName, pathModel, container, bindableType );
	}

	@Override
	public ManagedDomainType<J> getSqmPathType() {
		return (ManagedDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) getSqmPathType().findAttribute( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		return new NonAggregatedCompositeSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}
}

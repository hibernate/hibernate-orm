/*
 * SPDX-License-Identifier: Apache-2.0
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
public class NonAggregatedCompositeSqmPathSource<J>
		extends AbstractSqmPathSource<J> implements CompositeSqmPathSource<J> {

	private final ManagedDomainType<J> container;

	public NonAggregatedCompositeSqmPathSource(
			String localName,
			SqmPathSource<J> pathModel,
			BindableType bindableType,
			ManagedDomainType<J> container) {
		super( localName, pathModel, container, bindableType );
		this.container = container;
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) container.findAttribute( name );
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

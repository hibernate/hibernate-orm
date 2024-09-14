/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.IllegalPathUsageException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.NonAggregatedCompositeSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * Support for non-aggregated composite values
 *
 * @author Steve Ebersole
 */
public class NonAggregatedCompositeSqmPathSource extends AbstractSqmPathSource implements CompositeSqmPathSource {
	public NonAggregatedCompositeSqmPathSource(
			String localName,
			BindableType bindableType,
			ManagedDomainType container) {
		super( localName, container, bindableType );
	}

	@Override
	public ManagedDomainType<?> getSqmPathType() {
		return (ManagedDomainType<?>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) throws IllegalPathUsageException {
		return (SqmPathSource<?>) getSqmPathType().findAttribute( name );
	}

	@Override
	public SqmPath createSqmPath(SqmPath lhs, SqmCreationState creationState) {
		return new NonAggregatedCompositeSimplePath(
				lhs.getNavigablePath().append( getPathName() ),
				this,
				lhs,
				creationState.getCreationContext().getNodeBuilder()
		);
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class EntitySqmPathSource<J> extends AbstractSqmPathSource<J> {
	public EntitySqmPathSource(
			String localPathName,
			EntityDomainType<J> domainType,
			BindableType jpaBindableType,
			NodeBuilder nodeBuilder) {
		super( localPathName, domainType, jpaBindableType, nodeBuilder );
	}

	@Override
	public EntityDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (EntityDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return (SqmPathSource<?>) getSqmPathType().findAttribute( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		return new SqmEntityValuedSimplePath<>(
				lhs.getNavigablePath().append( getPathName() ),
				this,
				lhs,
				getNodeBuilder()
		);
	}
}

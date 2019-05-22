/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;

/**
 * @author Steve Ebersole
 */
public class AnyMappingSqmPathSource<J> extends AbstractSqmPathSource<J> {
	@SuppressWarnings("WeakerAccess")
	public AnyMappingSqmPathSource(
			String localPathName,
			AnyMappingDomainType<J> domainType,
			BindableType jpaBindableType,
			NodeBuilder nodeBuilder) {
		super( localPathName, domainType, jpaBindableType, nodeBuilder );
	}

	@Override
	public AnyMappingDomainType<J> getSqmPathType() {
		//noinspection unchecked
		return (AnyMappingDomainType<J>) super.getSqmPathType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmCreationState creationState) {
		throw new NotYetImplementedFor6Exception();
	}
}

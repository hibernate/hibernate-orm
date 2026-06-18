/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nullable;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.SqmJoinable;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPluralPartJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.domain.SqmMappedSuperclassDomainType;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassSqmPathSource<J> extends AbstractSqmPathSource<J> implements SqmJoinable<Object, J> {
	private final boolean isGeneric;
	private final SqmMappedSuperclassDomainType<J> domainType;

	public MappedSuperclassSqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			SqmMappedSuperclassDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.isGeneric = isGeneric;
		this.domainType = domainType;
	}

	@Override
	public SqmMappedSuperclassDomainType<J> getPathType() {
		return domainType;
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		return getPathType().findSubPathSource( name );
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		return new SqmEntityValuedSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}

	@Override
	public SqmPluralPartJoin<Object, J> createSqmJoin(
			SqmFrom<?, Object> lhs,
			SqmJoinType joinType,
			@Nullable String alias,
			boolean fetched,
			SqmCreationState creationState) {
		return new SqmPluralPartJoin<>(
				lhs,
				pathModel,
				alias,
				joinType,
				creationState.getCreationContext().getNodeBuilder()
		);
	}

	@Override
	public String getName() {
		return getPathName();
	}
}

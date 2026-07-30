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
import org.hibernate.query.sqm.tree.spi.domain.SqmEntityDomainType;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EntitySqmPathSource<J> extends AbstractSqmPathSource<J> implements SqmJoinable<Object, J> {
	private final boolean isGeneric;
	private final boolean reportGenericBindableJavaType;
	private final JavaType<?> bindableJavaType;
	private final SqmEntityDomainType<J> domainType;

	public EntitySqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			SqmEntityDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		this( localPathName, pathModel, domainType, jpaBindableType, isGeneric, isGeneric );
	}

	public EntitySqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			SqmEntityDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric,
			boolean reportGenericBindableJavaType) {
		this( localPathName, pathModel, domainType, domainType.getExpressibleJavaType(), jpaBindableType, isGeneric,
				reportGenericBindableJavaType );
	}

	public EntitySqmPathSource(
			String localPathName,
			SqmPathSource<J> pathModel,
			SqmEntityDomainType<J> domainType,
			JavaType<?> bindableJavaType,
			BindableType jpaBindableType,
			boolean isGeneric,
			boolean reportGenericBindableJavaType) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.domainType = domainType;
		this.bindableJavaType = bindableJavaType;
		this.isGeneric = isGeneric;
		this.reportGenericBindableJavaType = reportGenericBindableJavaType;
	}

	@Override
	public SqmEntityDomainType<J> getPathType() {
		return domainType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<J> getBindableJavaType() {
		return reportGenericBindableJavaType ? (Class<J>) Object.class : (Class<J>) bindableJavaType.getJavaTypeClass();
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		return getPathType().findSubPathSource( name );
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name, boolean includeSubtypes) {
		return getPathType().findSubPathSource( name, includeSubtypes );
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

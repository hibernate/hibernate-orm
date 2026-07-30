/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import jakarta.annotation.Nullable;
import org.hibernate.query.sqm.spi.SqmJoinable;
import org.hibernate.query.sqm.spi.SqmPathSource;
import org.hibernate.query.sqm.tree.spi.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmEmbeddableDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class EmbeddedSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements CompositeSqmPathSource<J> {
	private final boolean isGeneric;
	private final boolean reportGenericBindableJavaType;
	private final JavaType<?> bindableJavaType;
	private final SqmEmbeddableDomainType<J> domainType;

	public EmbeddedSqmPathSource(
			String localPathName,
			@Nullable SqmPathSource<J> pathModel,
			SqmEmbeddableDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		this( localPathName, pathModel, domainType, jpaBindableType, isGeneric, isGeneric );
	}

	public EmbeddedSqmPathSource(
			String localPathName,
			@Nullable SqmPathSource<J> pathModel,
			SqmEmbeddableDomainType<J> domainType,
			BindableType jpaBindableType,
			boolean isGeneric,
			boolean reportGenericBindableJavaType) {
		this( localPathName, pathModel, domainType, domainType.getExpressibleJavaType(), jpaBindableType, isGeneric,
				reportGenericBindableJavaType );
	}

	public EmbeddedSqmPathSource(
			String localPathName,
			@Nullable SqmPathSource<J> pathModel,
			SqmEmbeddableDomainType<J> domainType,
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
	public SqmEmbeddableDomainType<J> getPathType() {
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
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		return new SqmEmbeddedValuedSimplePath<>(
				pathModel instanceof SqmJoinable<?, ?> sqmJoinable
						? sqmJoinable.createNavigablePath( lhs, null )
						: PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
	}
}

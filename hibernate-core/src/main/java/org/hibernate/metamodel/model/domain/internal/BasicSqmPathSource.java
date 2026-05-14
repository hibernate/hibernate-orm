/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.TerminalPathException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmBooleanValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmComparableValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmNumericValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmTemporalValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmTextValuedSimplePath;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;
import static org.hibernate.metamodel.model.domain.internal.AttributeTypes.classification;

/**
 * @author Steve Ebersole
 */
public class BasicSqmPathSource<J>
		extends AbstractSqmPathSource<J>
		implements ReturnableType<J> {
	private final JavaType<?> relationalJavaType;
	private final boolean isGeneric;

	public BasicSqmPathSource(
			String localPathName,
			@Nullable SqmPathSource<J> pathModel,
			BasicDomainType<J> domainType,
			JavaType<?> relationalJavaType,
			BindableType jpaBindableType,
			boolean isGeneric) {
		super( localPathName, pathModel, domainType, jpaBindableType );
		this.relationalJavaType = relationalJavaType;
		this.isGeneric = isGeneric;
	}

	@Override
	public String getTypeName() {
		return super.getTypeName();
	}

//	@Override
//	public SqmDomainType<J> getSqmType() {
//		return getPathType();
//	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		final String path = pathModel.getPathName();
		final String pathDesc = path == null || path.startsWith( "{" ) ? " " : " '" + pathModel.getPathName() + "' ";
		throw new TerminalPathException( "Terminal path" + pathDesc + "has no attribute '" + name + "'" );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		final var path = PathHelper.append( lhs, this, intermediatePathSource );
		final var nodeBuilder = lhs.nodeBuilder();
		return switch ( classification( getJavaType() ) ) {
			case TEXT -> //noinspection unchecked
					(SqmPath<J>) new SqmTextValuedSimplePath(
						path,
						(SqmPathSource<String>) pathModel,
						lhs,
						nodeBuilder
					);
			case BOOLEAN -> //noinspection unchecked
					(SqmPath<J>) new SqmBooleanValuedSimplePath(
						path,
						(SqmPathSource<Boolean>) pathModel,
						lhs,
						nodeBuilder
					);
			case NUMERIC -> //noinspection unchecked,rawtypes
					new SqmNumericValuedSimplePath(
						path,
						pathModel,
						lhs,
						nodeBuilder
					);
			case TEMPORAL -> //noinspection unchecked,rawtypes
					new SqmTemporalValuedSimplePath(
						path,
						pathModel,
						lhs,
						nodeBuilder
					);
			case COMPARABLE -> //noinspection unchecked,rawtypes
					new SqmComparableValuedSimplePath(
						path,
						pathModel,
						lhs,
						nodeBuilder
					);
			case BASIC ->
					new SqmBasicValuedSimplePath<>(
							path,
							pathModel,
							lhs,
							nodeBuilder
					);
		};
	}

	@Override
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	@Override
	public Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public JavaType<?> getRelationalJavaType() {
		return relationalJavaType;
	}

	@Override
	public boolean isGeneric() {
		return isGeneric;
	}

	@Override
	public String toString() {
		return "BasicSqmPathSource("
			+ getPathName() + " : " + getJavaType().getSimpleName()
			+ ")";
	}
}

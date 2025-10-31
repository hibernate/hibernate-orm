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
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;

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
		String path = pathModel.getPathName();
		String pathDesc = path == null || path.startsWith( "{" ) ? " " : " '" + pathModel.getPathName() + "' ";
		throw new TerminalPathException( "Terminal path" + pathDesc + "has no attribute '" + name + "'" );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		return new SqmBasicValuedSimplePath<>(
				PathHelper.append( lhs, this, intermediatePathSource ),
				pathModel,
				lhs,
				lhs.nodeBuilder()
		);
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
		return "BasicSqmPathSource(" +
				getPathName() + " : " + getJavaType().getSimpleName() +
				")";
	}
}

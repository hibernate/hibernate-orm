/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.internal.PathHelper;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmBasicValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEmbeddedValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmEntityValuedSimplePath;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleSqmPathSourceNew<J> implements SqmPathSource<J> {
	private final String localPathName;
	private final SqmPathSource<J> pathSource;
	private final SqmDomainType<J> sqmPathType;

	public AnonymousTupleSqmPathSourceNew(String localPathName, SqmPathSource<J> pathSource, SqmDomainType<J> sqmPathType) {
		this.localPathName = localPathName;
		this.pathSource = pathSource;
		this.sqmPathType = sqmPathType;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return pathSource.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public String getPathName() {
		return localPathName;
	}

	@Override
	public SqmDomainType<J> getPathType() {
		return sqmPathType;
	}

	@Override
	public BindableType getBindableType() {
		return pathSource.getBindableType();
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return pathSource.getExpressibleJavaType();
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
		return pathSource.findSubPathSource( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
		if ( sqmPathType instanceof BasicDomainType<?> ) {
			return new SqmBasicValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( sqmPathType instanceof EmbeddableDomainType<?> ) {
			return new SqmEmbeddedValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( sqmPathType instanceof EntityDomainType<?> ) {
			return new SqmEntityValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}

		throw new UnsupportedOperationException( "Unsupported path source: " + sqmPathType );
	}
}

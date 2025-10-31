/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tuple.internal;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.DomainType;
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
public class AnonymousTupleSqmPathSource<J> implements SqmPathSource<J> {
	private final String localPathName;
	private final SqmPath<J> path;

	public AnonymousTupleSqmPathSource(String localPathName, SqmPath<J> path) {
		this.localPathName = localPathName;
		this.path = path;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return path.getNodeJavaType().getJavaTypeClass();
	}

	@Override
	public String getPathName() {
		return localPathName;
	}

	@Override
	public SqmDomainType<J> getPathType() {
//		return path.getNodeType().getPathType();
		return path.getResolvedModel().getPathType();
	}

	@Override
	public BindableType getBindableType() {
//		return path.getNodeType().getBindableType();
		return path.getResolvedModel().getBindableType();
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return path.getNodeJavaType();
	}

	@Override
	public @Nullable SqmPathSource<?> findSubPathSource(String name) {
//		return path.getNodeType().findSubPathSource( name );
		return path.getReferencedPathSource().findSubPathSource( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, @Nullable SqmPathSource<?> intermediatePathSource) {
//		final DomainType<?> domainType = path.getNodeType().getPathType();
		final DomainType<?> domainType = path.getReferencedPathSource().getPathType();
		if ( domainType instanceof BasicDomainType<?> ) {
			return new SqmBasicValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( domainType instanceof EmbeddableDomainType<?> ) {
			return new SqmEmbeddedValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}
		else if ( domainType instanceof EntityDomainType<?> ) {
			return new SqmEntityValuedSimplePath<>(
					PathHelper.append( lhs, this, intermediatePathSource ),
					this,
					lhs,
					lhs.nodeBuilder()
			);
		}

		throw new UnsupportedOperationException( "Unsupported path source: " + domainType );
	}
}

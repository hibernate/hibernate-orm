/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.derived;

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
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleSqmPathSource<J> implements SqmPathSource<J> {
	private final String localPathName;
	private final SqmPath<J> path;

	public AnonymousTupleSqmPathSource(
			String localPathName,
			SqmPath<J> path) {
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
	public DomainType<J> getSqmPathType() {
		return path.getNodeType().getSqmPathType();
	}

	@Override
	public BindableType getBindableType() {
		return path.getNodeType().getBindableType();
	}

	@Override
	public JavaType<J> getExpressibleJavaType() {
		return path.getNodeJavaType();
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		return path.getNodeType().findSubPathSource( name );
	}

	@Override
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		final DomainType<?> domainType = path.getNodeType().getSqmPathType();
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

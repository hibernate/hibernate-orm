/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * @author Steve Ebersole
 */
public class FullyQualifiedReflectivePath implements SemanticPathPart, FullyQualifiedReflectivePathSource {
	private final FullyQualifiedReflectivePathSource pathSource;
	private final String localName;

	public FullyQualifiedReflectivePath(
			FullyQualifiedReflectivePathSource pathSource,
			String localName) {
		this.pathSource = pathSource;
		this.localName = localName;
	}

	@Override
	public FullyQualifiedReflectivePath resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		if ( isTerminal ) {
			return new FullyQualifiedReflectivePathTerminal<>( this, name, creationState );
		}
		else {
			return new FullyQualifiedReflectivePath( this, name );
		}
	}

	@Override
	public SqmPath<?> resolveIndexedAccess(
			SqmExpression<?> selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException( "Fully qualified reflective paths cannot contain indexed access" );
	}

	@Override
	public FullyQualifiedReflectivePathSource getParent() {
		return pathSource;
	}

	@Override
	public String getLocalName() {
		return localName;
	}

	@Override
	public String getFullPath() {
		return pathSource.getFullPath() + '.' + localName;
	}

	@Override
	public FullyQualifiedReflectivePath append(String subPathName) {
		return new FullyQualifiedReflectivePath( this, subPathName );
	}
}

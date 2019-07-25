/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;

/**
 * Specialized "intermediate" SemanticPathPart for processing domain model paths
 *
 * @author Steve Ebersole
 */
public class DomainPathPart implements SemanticPathPart {
	private SqmPath<?> currentPath;

	@SuppressWarnings("WeakerAccess")
	public DomainPathPart(SqmPath<?> basePath) {
		this.currentPath = basePath;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPath<?> lhs = currentPath;
		final SqmPathSource subPathSource = lhs.getReferencedPathSource().findSubPathSource( name );
		//noinspection unchecked
		currentPath = subPathSource.createSqmPath( lhs, creationState );
		if ( isTerminal ) {
			return currentPath;
		}
		else {
			lhs.registerImplicitJoinPath( currentPath );
			return this;
		}
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}

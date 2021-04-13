/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SqmPathSource;
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
		assert currentPath != null;
	}

	SqmExpression<?> getSqmExpression() {
		return currentPath;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		HqlLogging.QUERY_LOGGER.tracef(
				"Resolving DomainPathPart(%s) sub-part : %s",
				currentPath,
				name
		);
// if we want to allow re-use of matched unaliased SqmFrom nodes
//
//		final SqmPathRegistry pathRegistry = creationState.getCurrentProcessingState().getPathRegistry();
//		final NavigablePath possibleImplicitAliasPath = lhs.getNavigablePath().append( name );
//		final SqmPath fromByImplicitAlias = pathRegistry.findPath( possibleImplicitAliasPath );
//
//		if ( fromByImplicitAlias != null ) {
//			if ( fromByImplicitAlias instanceof SqmFrom ) {
//				final String explicitPathAlias = fromByImplicitAlias.getExplicitAlias();
//				if ( explicitPathAlias == null || Objects.equals( possibleImplicitAliasPath.getFullPath(), explicitPathAlias ) ) {
//					currentPath = fromByImplicitAlias;
//					return isTerminal ? currentPath : this;
//				}
//			}
//		}
		currentPath = (SqmPath<?>) currentPath.get( name );
		if ( isTerminal ) {
			return currentPath;
		}
		else {
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

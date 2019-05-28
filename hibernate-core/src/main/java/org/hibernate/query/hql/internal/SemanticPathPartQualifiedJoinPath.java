/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * SemanticPathPart handling specific to processing a qualified join path
 *
 * @author Steve Ebersole
 */
public class SemanticPathPartQualifiedJoinPath implements SemanticPathPart {
	private final SqmRoot sqmRoot;

	private final SqmJoinType joinType;
	private final boolean fetch;
	private final String alias;

	@SuppressWarnings("WeakerAccess")
	public SemanticPathPartQualifiedJoinPath(SqmRoot sqmRoot, SqmJoinType joinType, boolean fetch, String alias) {
		this.sqmRoot = sqmRoot;
		this.joinType = joinType;
		this.fetch = fetch;
		this.alias = alias;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmCreationProcessingState processingState = creationState.getCurrentProcessingState();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();

		final SqmFrom fromByAlias = pathRegistry.findFromByAlias( name );
		if ( fromByAlias != null ) {
			return fromByAlias;
		}

		final SqmFrom fromExposing = pathRegistry.findFromExposing( name );
		if ( fromExposing != null ) {
			final SqmAttributeJoin join = ( (SqmJoinable) fromExposing.getReferencedPathSource().findSubPathSource(
					name ) ).createSqmJoin(
					fromExposing,
					joinType,
					isTerminal ? alias : null,
					fetch,
					creationState
			);
			pathRegistry.register( join );
			return join;
		}

		// otherwise it has to be an entity join

		if ( isTerminal ) {
			final EntityDomainType<?> entityType = processingState.getCreationState()
					.getCreationContext()
					.getJpaMetamodel()
					.entity( name );
			if ( entityType == null ) {
				throw new SemanticException( "Could not resolve qualified join path: " + name );
			}
			final SqmEntityJoin<?> entityJoin = new SqmEntityJoin<>( entityType, alias, joinType, sqmRoot );
			pathRegistry.register( entityJoin );
			return entityJoin;
		}

		return new SemanticPathPartDelayedEntityJoinHandler( name, sqmRoot, joinType, alias );
	}

	@Override
	public SqmPath resolveIndexedAccess(
			SqmExpression selector,
			boolean isTerminal,
			SqmCreationState creationState) {
		throw new SemanticException( "Illegal index-access as qualified join path" );
	}

	private static class SemanticPathPartDelayedEntityJoinHandler implements SemanticPathPart {
		private final SqmRoot sqmRoot;

		private final SqmJoinType joinType;
		private final String alias;

		private String pathSoFar;

		private SemanticPathPartDelayedEntityJoinHandler(
				String baseName,
				SqmRoot sqmRoot,
				SqmJoinType joinType,
				String alias) {
			this.sqmRoot = sqmRoot;
			this.joinType = joinType;
			this.alias = alias;

			this.pathSoFar = baseName;
		}

		@Override
		public SemanticPathPart resolvePathPart(
				String name,
				boolean isTerminal,
				SqmCreationState creationState) {
			pathSoFar = pathSoFar + '.' + name;
			if ( ! isTerminal ) {
				return this;
			}

			final SqmCreationProcessingState processingState = creationState.getCurrentProcessingState();
			final SqmPathRegistry pathRegistry = processingState.getPathRegistry();

			final EntityDomainType<?> entityType = processingState.getCreationState()
					.getCreationContext()
					.getJpaMetamodel()
					.entity( name );
			if ( entityType == null ) {
				throw new SemanticException( "Could not resolve qualified join path: " + name );
			}
			final SqmEntityJoin<?> entityJoin = new SqmEntityJoin<>( entityType, alias, joinType, sqmRoot );
			pathRegistry.register( entityJoin );

			return entityJoin;
		}

		@Override
		public SqmPath resolveIndexedAccess(
				SqmExpression selector,
				boolean isTerminal,
				SqmCreationState creationState) {
			throw new SemanticException( "Illegal index-access as qualified join path" );
		}
	}

}

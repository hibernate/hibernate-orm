/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.SqmJoinable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.produce.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmRoot;

/**
 * Specialized "intermediate" SemanticPathPart for processing domain model paths
 *
 * @author Steve Ebersole
 */
public class QualifiedJoinPathConsumer implements DotIdentifierConsumer {
	private final SqmCreationState creationState;
	private final SqmRoot sqmRoot;

	private final SqmJoinType joinType;
	private final boolean fetch;
	private final String alias;

	private SqmFrom currentPath;

	public QualifiedJoinPathConsumer(
			SqmRoot<?> sqmRoot,
			SqmJoinType joinType,
			boolean fetch,
			String alias,
			SqmCreationState creationState) {
		this.sqmRoot = sqmRoot;
		this.joinType = joinType;
		this.fetch = fetch;
		this.alias = alias;
		this.creationState = creationState;
	}

	@Override
	public void consumeIdentifier(String identifier, boolean isBase, boolean isTerminal) {
		if ( isBase ) {
			assert currentPath == null;

			this.currentPath = resolvePathBase( identifier, isTerminal, creationState );
		}
		else {
			assert currentPath != null;
			currentPath = createJoin( currentPath, identifier, isTerminal, creationState );
		}
	}

	private SqmFrom resolvePathBase(String identifier, boolean isTerminal, SqmCreationState creationState) {
		final SqmCreationProcessingState processingState = creationState.getCurrentProcessingState();
		final SqmPathRegistry pathRegistry = processingState.getPathRegistry();

		final SqmFrom pathRootByAlias = pathRegistry.findFromByAlias( identifier );

		if ( pathRootByAlias != null ) {
			// identifier is an alias (identification variable)

			if ( isTerminal ) {
				throw new SemanticException( "Cannot join to root : " + identifier );
			}

			return pathRootByAlias;
		}

		final SqmFrom pathRootByExposedNavigable = pathRegistry.findFromExposing( identifier );
		if ( pathRootByExposedNavigable != null ) {
			return createJoin( pathRootByExposedNavigable, identifier, isTerminal, creationState );
		}

		// todo (6.0) : another alternative here is an entity-join (entity name as rhs rather than attribute path)
		//		- need to account for that here, which may need delayed resolution in the case of a
		//			qualified entity reference (FQN)

		throw new SemanticException( "Could not determine how to resolve qualified join base : " + identifier );
	}

	private SqmFrom createJoin(SqmFrom lhs, String identifier, boolean isTerminal, SqmCreationState creationState) {
		final SqmPathSource subPathSource = lhs.getReferencedPathSource().findSubPathSource( identifier );
		final SqmAttributeJoin join = ( (SqmJoinable) subPathSource ).createSqmJoin(
				lhs,
				joinType,
				isTerminal ? alias : null,
				fetch,
				creationState
		);
		lhs.addSqmJoin( join );
		creationState.getCurrentProcessingState().getPathRegistry().register( join );
		return join;
	}

	@Override
	public SemanticPathPart getConsumedPart() {
		return currentPath;
	}
}

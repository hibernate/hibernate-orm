/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.produce.SqmCreationProcessingState;
import org.hibernate.query.sqm.produce.SqmPathRegistry;
import org.hibernate.query.sqm.produce.SqmProductionException;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;

/**
 * @author Steve Ebersole
 */
public class SelectClauseDotIdentifierConsumer extends BasicDotIdentifierConsumer {
	public SelectClauseDotIdentifierConsumer(Supplier<SqmCreationProcessingState> processingStateSupplier) {
		super( processingStateSupplier );
	}

	@Override
	protected SemanticPathPart resolveSubPart(String identifier, boolean isTerminal) {
		if ( getConsumedPart() instanceof SqmPath ) {
			final SqmPath consumedPart = (SqmPath) getConsumedPart();

			final NavigablePath subNavigablePath = consumedPart.getNavigablePath().append( identifier );
			final SqmPathRegistry sqmPathRegistry = getCreationProcessingState().getPathRegistry();

			final Navigable subNavigable = ( (NavigableContainer) consumedPart.getReferencedNavigable() ).findNavigable( identifier );

			if ( subNavigable instanceof Joinable ) {
				// see if we have a SqmFrom registration for the path already

				final SqmFrom subNavigableFromByPath = sqmPathRegistry.findFromByPath( subNavigablePath );
				if ( subNavigableFromByPath != null ) {
					return subNavigableFromByPath;
				}

				SqmFrom lhs = sqmPathRegistry.findFromByPath( consumedPart.getNavigablePath() );
				if ( lhs == null ) {
					final SqmPath lhsPath = sqmPathRegistry.findPath( consumedPart.getNavigablePath() );
					lhsPath.prepareForSubNavigableReference( subNavigable, false, getCreationProcessingState().getCreationState() );
					lhs = sqmPathRegistry.findFromByPath( consumedPart.getNavigablePath() );
				}

				if ( lhs == null ) {
					throw new SqmProductionException( "Could not locate LHS SqmFrom for Navigable reference : " + subNavigablePath.getFullPath() );
				}

				final SqmAttributeJoin sqmJoin = ( (Joinable) subNavigable ).createSqmJoin(
						lhs,
						SqmJoinType.INNER,
						null,
						true,
						getCreationProcessingState().getCreationState()
				);

				//noinspection unchecked
				lhs.addSqmJoin( sqmJoin );
				sqmPathRegistry.register( sqmJoin );

				return sqmJoin;
			}
			else {
				//noinspection unchecked
				return sqmPathRegistry.resolvePath(
						subNavigablePath,
						np -> subNavigable.createSqmExpression(
								consumedPart,
								getCreationProcessingState().getCreationState()
						)
				);
			}
		}

		return super.resolveSubPart( identifier, isTerminal );
	}


	@Override
	protected SemanticPathPart createBasePart() {
		return new BaseLocalSequencePartExt();
	}

	public class BaseLocalSequencePartExt extends BaseLocalSequencePart {
		@Override
		public SemanticPathPart resolvePathPart(
				String identifier,
				String currentContextKey,
				boolean isTerminal,
				SqmCreationState creationState) {
			final SqmPathRegistry sqmPathRegistry = creationState.getProcessingStateStack()
					.getCurrent()
					.getPathRegistry();

			final SqmFrom pathRootByAlias = sqmPathRegistry.findFromByAlias( identifier );
			if ( pathRootByAlias != null ) {
				// identifier is an alias (identification variable)
				// 		- should not need preparation as a LHS, but to be sure....
				final Navigable subNavigable = pathRootByAlias.getReferencedNavigable().findNavigable( identifier );
				pathRootByAlias.prepareForSubNavigableReference( subNavigable, false, creationState );
				return pathRootByAlias;
			}

			final SqmFrom pathRootByExposedNavigable = sqmPathRegistry.findFromExposing( identifier );
			if ( pathRootByExposedNavigable != null ) {
				// identifier is an "unqualified attribute reference"

				final Navigable subNavigable = pathRootByExposedNavigable.getReferencedNavigable().findNavigable( identifier );
				pathRootByExposedNavigable.prepareForSubNavigableReference( subNavigable, false, creationState );

				if ( subNavigable instanceof Joinable ) {
					final SqmAttributeJoin sqmJoin = ( (Joinable) subNavigable ).createSqmJoin(
							pathRootByExposedNavigable,
							SqmJoinType.INNER,
							null,
							true,
							creationState
					);
					//noinspection unchecked
					pathRootByExposedNavigable.addSqmJoin( sqmJoin );
					getCreationProcessingState().getPathRegistry().register( sqmJoin );

					return sqmJoin;
				}
				else {
					//noinspection unchecked
					return sqmPathRegistry.resolvePath(
							pathRootByExposedNavigable.getNavigablePath().append( identifier ),
							np ->  subNavigable.createSqmExpression( pathRootByExposedNavigable, creationState )
					);
				}
			}

			return super.resolvePathPart( identifier, currentContextKey, isTerminal, creationState );
		}
	}
}

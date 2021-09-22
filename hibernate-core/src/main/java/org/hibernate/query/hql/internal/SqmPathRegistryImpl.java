/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.internal.util.MutableInteger;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.hql.HqlLogging;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.AliasCollisionException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmAliasedNode;

/**
 * Container for indexing needed while building an SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmPathRegistryImpl implements SqmPathRegistry {
	private final SqmCreationProcessingState associatedProcessingState;
	private final JpaCompliance jpaCompliance;

	private final Map<NavigablePath, SqmPath<?>> sqmPathByPath = new HashMap<>();
	private final Map<NavigablePath, SqmFrom<?, ?>> sqmFromByPath = new HashMap<>();

	private final Map<String, SqmFrom<?, ?>> sqmFromByAlias = new HashMap<>();

	private final List<SqmAliasedNode<?>> simpleSelectionNodes = new ArrayList<>();

	public SqmPathRegistryImpl(SqmCreationProcessingState associatedProcessingState) {
		this.associatedProcessingState = associatedProcessingState;
		this.jpaCompliance = associatedProcessingState.getCreationState().getCreationContext().getJpaMetamodel().getJpaCompliance();
	}

	@Override
	public void register(SqmPath<?> sqmPath) {
		SqmTreeCreationLogger.LOGGER.tracef( "SqmProcessingIndex#register(SqmPath) : %s", sqmPath.getNavigablePath().getFullPath() );

		// Generally we:
		//		1) add the path to the path-by-path map
		//		2) if the path is a from, we add it to the from-by-path map
		//		3) if the path is a from and defines an alias, we add it to the from-by-alias map
		//
		// Regarding part #1 (add to the path-by-path map), it is ok for a SqmFrom to replace a
		// 		non-SqmFrom.  This should equate to, e.g., an implicit join.

		if ( sqmPath instanceof SqmFrom<?, ?> ) {
			final SqmFrom<?, ?> sqmFrom = (SqmFrom<?, ?>) sqmPath;

			final String alias = sqmPath.getExplicitAlias();
			if ( alias != null ) {
				final String aliasToUse = jpaCompliance.isJpaQueryComplianceEnabled()
						? alias.toLowerCase( Locale.getDefault() )
						: alias;

				final SqmFrom<?, ?> previousFrom = sqmFromByAlias.put( aliasToUse, sqmFrom );

				if ( previousFrom != null ) {
					throw new AliasCollisionException(
							String.format(
									Locale.ENGLISH,
									"Alias [%s] used for multiple from-clause elements : %s, %s",
									alias,
									previousFrom,
									sqmPath
							)
					);
				}
			}

			final SqmFrom<?, ?> previousFromByPath = sqmFromByPath.put( sqmPath.getNavigablePath(), sqmFrom );

			if ( previousFromByPath != null ) {
				// this should never happen
				throw new ParsingException(
						String.format(
								Locale.ROOT,
								"Registration for SqmFrom [%s] overrode previous registration: %s -> %s",
								sqmPath.getNavigablePath(),
								previousFromByPath,
								sqmFrom
						)
				);
			}
		}

		final SqmPath<?> previousPath = sqmPathByPath.put( sqmPath.getNavigablePath(), sqmPath );

		if ( previousPath instanceof SqmFrom ) {
			// this should never happen
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"Registration for path [%s] overrode previous registration: %s -> %s",
							sqmPath.getNavigablePath(),
							previousPath,
							sqmPath
					)
			);
		}
	}

	@Override
	public <X> SqmPath<X> findPath(NavigablePath path) {
		final SqmPath<?> found = sqmPathByPath.get( path );
		if ( found != null ) {
			//noinspection unchecked
			return (SqmPath<X>) found;
		}

		if ( associatedProcessingState.getParentProcessingState() != null ) {
			final SqmFrom<?, X> containingQueryFrom = associatedProcessingState.getParentProcessingState()
					.getPathRegistry()
					.findFromByPath( path );
			if ( containingQueryFrom != null ) {
				// todo (6.0) create a correlation?
				return containingQueryFrom;
			}
		}

		return null;
	}

	@Override
	public <X extends SqmFrom<?, ?>> X findFromByPath(NavigablePath navigablePath) {
		final SqmFrom<?, ?> found = sqmFromByPath.get( navigablePath );
		if ( found != null ) {
			//noinspection unchecked
			return (X) found;
		}

		if ( associatedProcessingState.getParentProcessingState() != null ) {
			final X containingQueryFrom = associatedProcessingState.getParentProcessingState()
					.getPathRegistry()
					.findFromByPath( navigablePath );
			if ( containingQueryFrom != null ) {
				// todo (6.0) create a correlation?
				return containingQueryFrom;
			}
		}

		return null;
	}

	@Override
	public <X extends SqmFrom<?, ?>> X findFromByAlias(String alias) {
		final String localAlias = jpaCompliance.isJpaQueryComplianceEnabled()
				? alias.toLowerCase( Locale.getDefault() )
				: alias;

		final SqmFrom<?, ?> registered = sqmFromByAlias.get( localAlias );

		if ( registered != null ) {
			//noinspection unchecked
			return (X) registered;
		}

		if ( associatedProcessingState.getParentProcessingState() != null ) {
			return associatedProcessingState.getParentProcessingState().getPathRegistry().findFromByAlias( alias );
		}

		return null;
	}

	@Override
	public <X extends SqmFrom<?, ?>> X findFromExposing(String navigableName) {
		// todo (6.0) : atm this checks every from-element every time, the idea being to make sure there
		//  	is only one such element obviously that scales poorly across larger from-clauses.  Another
		//  	(configurable?) option would be to simply pick the first one as a perf optimization

		SqmFrom<?, ?> found = null;
		for ( Map.Entry<NavigablePath, SqmFrom<?, ?>> entry : sqmFromByPath.entrySet() ) {
			final SqmFrom<?, ?> fromElement = entry.getValue();
			if ( definesAttribute( fromElement.getReferencedPathSource(), navigableName ) ) {
				if ( found != null ) {
					throw new IllegalStateException( "Multiple from-elements expose unqualified attribute : " + navigableName );
				}
				found = fromElement;
			}
		}

		if ( found == null ) {
			if ( associatedProcessingState.getParentProcessingState() != null ) {
				HqlLogging.QUERY_LOGGER.debugf(
						"Unable to resolve unqualified attribute [%s] in local from-clause; checking parent ",
						navigableName
				);
				found = associatedProcessingState.getParentProcessingState().getPathRegistry().findFromExposing( navigableName );
			}
		}

		HqlLogging.QUERY_LOGGER.debugf(
				"Unable to resolve unqualified attribute [%s] in local from-clause",
				navigableName
		);

		//noinspection unchecked
		return (X) found;
	}

	@Override
	public <X extends SqmFrom<?, ?>> X resolveFrom(NavigablePath navigablePath, Function<NavigablePath, SqmFrom<?, ?>> creator) {
		SqmTreeCreationLogger.LOGGER.tracef( "SqmProcessingIndex#resolvePath(NavigablePath) : %s", navigablePath );

		final SqmFrom<?, ?> existing = sqmFromByPath.get( navigablePath );
		if ( existing != null ) {
			//noinspection unchecked
			return (X) existing;
		}

		final SqmFrom<?, ?> sqmFrom = creator.apply( navigablePath );
		register( sqmFrom );
		//noinspection unchecked
		return (X) sqmFrom;
	}

	@Override
	public <X extends SqmFrom<?, ?>> X resolveFrom(SqmPath<?> path) {
		SqmTreeCreationLogger.LOGGER.tracef( "SqmProcessingIndex#resolvePath(SqmPath) : %s", path );

		final SqmFrom<?, ?> existing = sqmFromByPath.get( path.getNavigablePath() );
		if ( existing != null ) {
			//noinspection unchecked
			return (X) existing;
		}

		final SqmFrom<?, ?> sqmFrom = resolveFrom( path.getLhs() ).join( path.getNavigablePath().getUnaliasedLocalName() );
		register( sqmFrom );
		//noinspection unchecked
		return (X) sqmFrom;
	}

	@Override
	public <X> SqmPath<X> resolvePath(NavigablePath navigablePath, Function<NavigablePath, SqmPath<X>> creator) {
		SqmTreeCreationLogger.LOGGER.tracef( "SqmProcessingIndex#resolvePath(NavigablePath) : %s", navigablePath );

		final SqmPath<?> existing = sqmPathByPath.get( navigablePath );
		if ( existing != null ) {
			//noinspection unchecked
			return (SqmPath<X>) existing;
		}

		final SqmPath<X> sqmPath = creator.apply( navigablePath );
		register( sqmPath );
		return sqmPath;
	}

	private boolean definesAttribute(SqmPathSource<?> containerType, String name) {
		return containerType.findSubPathSource( name ) != null;
	}

	@Override
	public SqmAliasedNode<?> findAliasedNodeByAlias(String alias) {
		assert alias != null;

		final String aliasToUse = jpaCompliance.isJpaQueryComplianceEnabled()
				? alias.toLowerCase( Locale.getDefault() )
				: alias;

		for ( int i = 0; i < simpleSelectionNodes.size(); i++ ) {
			final SqmAliasedNode<?> node = simpleSelectionNodes.get( i );
			if ( aliasToUse.equals( node.getAlias() ) ) {
				return node;
			}
		}

		return null;
	}

	@Override
	public Integer findAliasedNodePosition(String alias) {
		if ( alias == null ) {
			return null;
		}

		final String aliasToUse = jpaCompliance.isJpaQueryComplianceEnabled()
				? alias.toLowerCase( Locale.getDefault() )
				: alias;

		// NOTE : 1-based

		for ( int i = 0; i < simpleSelectionNodes.size(); i++ ) {
			final SqmAliasedNode<?> node = simpleSelectionNodes.get( i );
			if ( aliasToUse.equals( node.getAlias() ) ) {
				return i + 1;
			}
		}

		return null;
	}

	@Override
	public SqmAliasedNode<?> findAliasedNodeByPosition(int position) {
		// NOTE : 1-based

		return simpleSelectionNodes.get( position - 1 );
	}

	@Override
	public void register(SqmAliasedNode<?> node) {
		checkResultVariable( node );
		simpleSelectionNodes.add( node );
	}

	private void checkResultVariable(SqmAliasedNode<?> selection) {
		final String alias = selection.getAlias();
		if ( alias == null ) {
			return;
		}

		final Integer position = findAliasedNodePosition( alias );
		if ( position != null ) {
			throw new AliasCollisionException(
					String.format(
							Locale.ENGLISH,
							"Alias [%s] is already used in same select clause [position=%s]",
							alias,
							position
					)
			);
		}

		final SqmFrom<?, ?> registeredFromElement = sqmFromByAlias.get( alias );
		if ( registeredFromElement != null ) {
			if ( !registeredFromElement.equals( selection.getSelectableNode() ) ) {
				throw new AliasCollisionException(
						String.format(
								Locale.ENGLISH,
								"Alias [%s] used in select-clause [%s] also used in from-clause [%s]",
								alias,
								selection.getSelectableNode(),
								registeredFromElement
						)
				);
			}
		}
	}
}

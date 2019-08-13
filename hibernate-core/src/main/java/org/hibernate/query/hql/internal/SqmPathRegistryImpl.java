/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.hql.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.hibernate.query.NavigablePath;
import org.hibernate.query.QueryLogger;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.AliasCollisionException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.sqm.SqmTreeCreationLogger;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.select.SqmSelection;

/**
 * Container for indexing needed while building an SQM tree.
 *
 * @author Steve Ebersole
 */
public class SqmPathRegistryImpl implements SqmPathRegistry {
	private final SqmCreationProcessingState associatedProcessingState;

	private final Map<NavigablePath, SqmPath> sqmPathByPath = new HashMap<>();
	private final Map<NavigablePath, SqmFrom> sqmFromByPath = new HashMap<>();

	private final Map<String, SqmFrom> sqmFromByAlias = new HashMap<>();

	private final LinkedHashMap<String, SqmSelection> sqmSelectionsByAlias = new LinkedHashMap<>();

	public SqmPathRegistryImpl(SqmCreationProcessingState associatedProcessingState) {
		this.associatedProcessingState = associatedProcessingState;
	}

	@Override
	public void register(SqmPath sqmPath) {
		SqmTreeCreationLogger.LOGGER.tracef( "SqmProcessingIndex#register(SqmPath) : %s", sqmPath.getNavigablePath().getFullPath() );

		// Generally we:
		//		1) add the path to the path-by-path map
		//		2) if the path is a from, we add it to the from-by-path map
		//		3) if the path is a from and defines an alias, we add it to the from-by-alias map
		//
		// Regarding part #1 (add to the path-by-path map), it is ok for a SqmFrom to replace a
		// 		non-SqmFrom.  This should equate to, e.g., an implicit join.

		if ( sqmPath instanceof SqmFrom ) {
			final SqmFrom sqmFrom = (SqmFrom) sqmPath;

			final String alias = sqmPath.getExplicitAlias();
			if ( alias != null ) {
				final SqmFrom previousFrom = sqmFromByAlias.put( alias, sqmFrom );
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

			final SqmFrom previousFromByPath = sqmFromByPath.put( sqmPath.getNavigablePath(), sqmFrom );

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

		final SqmPath previousPath = sqmPathByPath.put( sqmPath.getNavigablePath(), sqmPath );

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
	public SqmPath findPath(NavigablePath path) {
		final SqmPath found = sqmPathByPath.get( path );
		if ( found != null ) {
			return found;
		}

		if ( associatedProcessingState.getParentProcessingState() != null ) {
			final SqmFrom containingQueryFrom = associatedProcessingState.getParentProcessingState()
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
	public SqmFrom findFromByPath(NavigablePath navigablePath) {
		final SqmFrom found = sqmFromByPath.get( navigablePath );
		if ( found != null ) {
			return found;
		}

		if ( associatedProcessingState.getParentProcessingState() != null ) {
			final SqmFrom containingQueryFrom = associatedProcessingState.getParentProcessingState()
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
	public SqmFrom findFromByAlias(String alias) {
		final SqmFrom registered = sqmFromByAlias.get( alias );
		if ( registered != null ) {
			return registered;
		}

		if ( associatedProcessingState.getParentProcessingState() != null ) {
			return associatedProcessingState.getParentProcessingState().getPathRegistry().findFromByAlias( alias );
		}

		return null;
	}

	@Override
	public SqmFrom findFromExposing(String navigableName) {
		// todo (6.0) : atm this checks every from-element every time, the idea being to make sure there
		//  	is only one such element obviously that scales poorly across larger from-clauses.  Another
		//  	(configurable?) option would be to simply pick the first one as a perf optimization

		SqmFrom found = null;
		for ( SqmFrom fromElement : sqmFromByPath.values() ) {
			if ( definesAttribute( fromElement.getReferencedPathSource(), navigableName ) ) {
				if ( found != null ) {
					throw new IllegalStateException( "Multiple from-elements expose unqualified attribute : " + navigableName );
				}
				found = fromElement;
			}
		}

		if ( found == null ) {
			if ( associatedProcessingState.getParentProcessingState() != null ) {
				QueryLogger.QUERY_LOGGER.debugf(
						"Unable to resolve unqualified attribute [%s] in local from-clause; checking parent ",
						navigableName
				);
				found = associatedProcessingState.getParentProcessingState().getPathRegistry().findFromExposing( navigableName );
			}
		}

		return found;
	}

	@Override
	public SqmPath resolvePath(NavigablePath navigablePath, Function<NavigablePath, SqmPath> creator) {
		SqmTreeCreationLogger.LOGGER.tracef( "SqmProcessingIndex#resolvePath(NavigablePath) : %s", navigablePath );

		final SqmPath existing = sqmPathByPath.get( navigablePath );
		if ( existing != null ) {
			return existing;
		}

		final SqmPath sqmPath = creator.apply( navigablePath );
		register( sqmPath );
		return sqmPath;
	}

	private boolean definesAttribute(SqmPathSource containerType, String name) {
		return containerType.findSubPathSource( name ) != null;
	}

	@Override
	public SqmSelection findSelectionByAlias(String alias) {
		return sqmSelectionsByAlias.get( alias );
	}

	@Override
	public SqmSelection findSelectionByPosition(int position) {
		// NOTE : 1-based
		//		so incoming position must be between >= 1 and <= map.size

		if ( position >= 1 && position <= sqmSelectionsByAlias.size() ) {
			int i = 1;
			for ( Map.Entry<String, SqmSelection> entry : sqmSelectionsByAlias.entrySet() ) {
				if ( position == i++ ) {
					return entry.getValue();
				}
			}
		}

		return null;
	}

	@Override
	public void register(SqmSelection selection) {
		if ( selection.getAlias() != null ) {
			checkResultVariable( selection );
			sqmSelectionsByAlias.put( selection.getAlias(), selection );
		}
	}

	private void checkResultVariable(SqmSelection selection) {
		final String alias = selection.getAlias();

		if ( sqmSelectionsByAlias.containsKey( alias ) ) {
			throw new AliasCollisionException(
					String.format(
							Locale.ENGLISH,
							"Alias [%s] is already used in same select clause",
							alias
					)
			);
		}

		final SqmFrom registeredFromElement = sqmFromByAlias.get( alias );
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

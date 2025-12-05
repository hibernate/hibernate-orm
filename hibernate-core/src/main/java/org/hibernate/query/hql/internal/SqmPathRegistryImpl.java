/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.hql.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.SqmCreationProcessingState;
import org.hibernate.query.hql.spi.SqmPathRegistry;
import org.hibernate.query.sqm.AliasCollisionException;
import org.hibernate.query.sqm.ParsingException;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmAliasedNode;
import org.hibernate.query.sqm.tree.select.SqmSubQuery;
import org.hibernate.spi.NavigablePath;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

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
		this.jpaCompliance =
				associatedProcessingState.getCreationState().getCreationContext()
						.getNodeBuilder().getJpaCompliance();
	}

	private String handleAliasCaseSensitivity(String alias) {
		return jpaCompliance.isJpaQueryComplianceEnabled()
				? alias.toLowerCase( Locale.getDefault() )
				: alias;
	}

	@Override
	public void register(SqmPath<?> sqmPath) {

		// Generally we:
		//		1) add the path to the path-by-path map
		//		2) if the path is a from, we add it to the from-by-path map
		//		3) if the path is a from and defines an alias, we add it to the from-by-alias map
		//
		// Regarding part #1 (add to the path-by-path map), it is ok for a SqmFrom to replace a
		// 		non-SqmFrom.  This should equate to, e.g., an implicit join.

		final var navigablePath = sqmPath.getNavigablePath();

		if ( sqmPath instanceof SqmFrom<?, ?> sqmFrom ) {
			registerByAliasOnly( sqmFrom );
			final var previousFromByPath = sqmFromByPath.put( navigablePath, sqmFrom );
			if ( previousFromByPath != null ) {
				// this should never happen
				throw new ParsingException(
						String.format(
								Locale.ROOT,
								"Registration for SqmFrom [%s] overrode previous registration: %s -> %s",
								navigablePath,
								previousFromByPath,
								sqmFrom
						)
				);
			}
		}

		final var previousPath = sqmPathByPath.put( navigablePath, sqmPath );
		if ( previousPath instanceof SqmFrom ) {
			// this should never happen
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"Registration for path [%s] overrode previous registration: %s -> %s",
							navigablePath,
							previousPath,
							sqmPath
					)
			);
		}
	}

	private static String fromPath(SqmFrom<?, ?> sqmFrom, boolean first) {
		//TODO: the qualified path, but not using getFullPath() which has cruft
		final String keyword;
		if ( sqmFrom instanceof SqmRoot && first ) {
			keyword = "from ";
		}
		else if ( sqmFrom instanceof SqmJoin ) {
			keyword = first ? "join " : " join ";
		}
		else {
			keyword = first ? "" : ", ";
		}
		final String path = sqmFrom.getNavigablePath().getLocalName();
		final String alias = sqmFrom.getExplicitAlias();
		return keyword + (alias == null ? path : path + " as " + alias);
	}

	@Override
	public void registerByAliasOnly(SqmFrom<?, ?> sqmFrom) {
		final String alias = sqmFrom.getExplicitAlias();
		if ( alias != null ) {
			final var previousFrom = sqmFromByAlias.put( handleAliasCaseSensitivity( alias ), sqmFrom );
			if ( previousFrom != null ) {
				throw new AliasCollisionException(
						String.format(
								Locale.ENGLISH,
								"Duplicate identification variable '%s' in 'from' clause [%s%s]",
								alias,
								fromPath( previousFrom, true ),
								fromPath( sqmFrom, false )
						)
				);
			}
		}
	}

	@Override
	public <E> void replace(SqmEntityJoin<?,E> sqmJoin, SqmRoot<E> sqmRoot) {
		final String alias = sqmJoin.getExplicitAlias();
		if ( alias != null ) {
			final var previousFrom = sqmFromByAlias.put( handleAliasCaseSensitivity( alias ), sqmJoin );
			if ( previousFrom != null && !( previousFrom instanceof SqmRoot ) ) {
				throw new AliasCollisionException(
						String.format(
								Locale.ENGLISH,
								"Duplicate identification variable '%s' in 'join' clause [%s%s]",
								alias,
								fromPath( previousFrom, true ),
								fromPath( sqmJoin, false )
						)
				);
			}
		}

		final var navigablePath = sqmJoin.getNavigablePath();

		final var previousFromByPath = sqmFromByPath.put( navigablePath, sqmJoin );
		if ( previousFromByPath != null && !( previousFromByPath instanceof SqmRoot ) ) {
			// this should never happen
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"Registration for SqmFrom [%s] overrode previous registration: %s -> %s",
							navigablePath,
							previousFromByPath,
							sqmJoin
					)
			);
		}

		final var previousPath = sqmPathByPath.put( navigablePath, sqmJoin );
		if ( previousPath instanceof SqmFrom && !( previousPath instanceof SqmRoot ) ) {
			// this should never happen
			throw new ParsingException(
					String.format(
							Locale.ROOT,
							"Registration for path [%s] overrode previous registration: %s -> %s",
							navigablePath,
							previousPath,
							sqmJoin
					)
			);
		}
	}

	@Override
	public <X extends SqmFrom<?, ?>> @Nullable X findFromByPath(NavigablePath navigablePath) {
		//noinspection unchecked
		return (X) sqmFromByPath.get( navigablePath );
	}

	@Override
	public <X extends SqmFrom<?, ?>> @Nullable X findFromByAlias(String alias, boolean searchParent) {
		final String localAlias = handleAliasCaseSensitivity( alias );

		final var registered = sqmFromByAlias.get( localAlias );
		if ( registered != null ) {
			//noinspection unchecked
			return (X) registered;
		}

		var parentProcessingState = associatedProcessingState.getParentProcessingState();
		if ( searchParent && parentProcessingState != null ) {
			X parentRegistered;
			do {
				parentRegistered =
						parentProcessingState.getPathRegistry()
								.findFromByAlias( alias, false );
				parentProcessingState = parentProcessingState.getParentProcessingState();
			}
			while (parentProcessingState != null && parentRegistered == null);
			if ( parentRegistered != null ) {
				// If a parent query contains the alias, we need to create a correlation on the subquery
				final var correlated = correlate( parentRegistered );
				register( correlated );
				//noinspection unchecked
				return (X) correlated;
			}
		}

		final boolean onlyOneFrom = sqmFromByPath.size() == 1;
		if ( onlyOneFrom && localAlias.equalsIgnoreCase( "this" ) ) {
			final var root = (SqmRoot<?>) sqmFromByPath.entrySet().iterator().next().getValue();
			if (  root.getAlias() == null ) {
				//noinspection unchecked
				return (X) root;
			}
		}
		return null;
	}

	private <X extends SqmFrom<?, ?>> SqmFrom<?, ?> correlate(X parentRegistered) {
		final var selectQuery = (SqmSubQuery<?>) associatedProcessingState.getProcessingQuery();
		if ( parentRegistered instanceof Root<?> root ) {
			return selectQuery.correlate( root );
		}
		else if ( parentRegistered instanceof Join<?, ?> join ) {
			return selectQuery.correlate( join );
		}
		else {
			throw new UnsupportedOperationException( "Can't correlate from node: " + parentRegistered );
		}
	}

	@Override
	public <X extends SqmFrom<?, ?>> @Nullable X findFromExposing(String navigableName) {
		// todo (6.0) : atm this checks every from-element every time, the idea being to make sure there
		//  	is only one such element obviously that scales poorly across larger from-clauses.  Another
		//  	(configurable?) option would be to simply pick the first one as a perf optimization

		SqmFrom<?, ?> found = null;
		for ( var entry : sqmFromByPath.entrySet() ) {
			final var fromElement = entry.getValue();
			if ( definesAttribute( fromElement.getReferencedPathSource(), navigableName ) ) {
				if ( found != null ) {
					throw new SemanticException( "Ambiguous unqualified attribute reference '" + navigableName +
							"' (qualify the attribute reference by an identification variable)" );
				}
				found = fromElement;
			}
		}

		if ( found == null ) {
			final var processingState = associatedProcessingState.getParentProcessingState();
			if ( processingState != null ) {
//				QUERY_LOGGER.tracef(
//						"Unable to resolve unqualified attribute [%s] in local from-clause; checking parent ",
//						navigableName
//				);
				found = processingState.getPathRegistry().findFromExposing( navigableName );
			}
		}

//		QUERY_LOGGER.tracef(
//				"Unable to resolve unqualified attribute [%s] in local from-clause",
//				navigableName
//		);

		//noinspection unchecked
		return (X) found;
	}

	@Override
	public <X extends SqmFrom<?, ?>> X resolveFrom(NavigablePath navigablePath, Function<NavigablePath, SqmFrom<?, ?>> creator) {
		final var existing = sqmFromByPath.get( navigablePath );
		if ( existing != null ) {
			//noinspection unchecked
			return (X) existing;
		}
		else {
			final var sqmFrom = creator.apply( navigablePath );
			register( sqmFrom );
			//noinspection unchecked
			return (X) sqmFrom;
		}
	}

	@Override
	public <X extends SqmFrom<?, ?>> X resolveFromByPath(NavigablePath navigablePath) {
		final X from = findFromByPath( navigablePath );
		if ( from == null ) {
			throw new SemanticException( "Cannot resolve find from by navigable path '" + navigablePath + "'" );
		}
		return from;
	}

	@Override
	public <X extends SqmFrom<?, ?>> X resolveFrom(SqmPath<?> path) {
		final var navigablePath = path.getNavigablePath();
		final var existing = sqmFromByPath.get( navigablePath );
		if ( existing != null ) {
			//noinspection unchecked
			return (X) existing;
		}
		else {
			final var lhs = path.getLhs();
			if ( lhs == null ) {
				throw new SemanticException( "Cannot resolve path '" + path + "'" );
			}
			final var sqmFrom = resolveFrom( lhs ).join( path.getNavigablePath().getLocalName() );
			register( sqmFrom );
			//noinspection unchecked
			return (X) sqmFrom;
		}
	}

	private boolean definesAttribute(SqmPathSource<?> containerType, String name) {
		return !( containerType.getSqmType() instanceof BasicDomainType )
			&& containerType.findSubPathSource( name, true ) != null;
	}

	@Override
	public @Nullable SqmAliasedNode<?> findAliasedNodeByAlias(String alias) {
		assert alias != null;
		final String aliasToUse = handleAliasCaseSensitivity( alias );
		for ( int i = 0; i < simpleSelectionNodes.size(); i++ ) {
			final var node = simpleSelectionNodes.get( i );
			if ( aliasToUse.equals( node.getAlias() ) ) {
				return node;
			}
		}
		return null;
	}

	@Override
	public @Nullable Integer findAliasedNodePosition(String alias) {
		if ( alias != null ) {
			final String aliasToUse = handleAliasCaseSensitivity( alias );
			// NOTE: 1-based
			for ( int i = 0; i < simpleSelectionNodes.size(); i++ ) {
				final var node = simpleSelectionNodes.get( i );
				if ( aliasToUse.equals( node.getAlias() ) ) {
					return i + 1;
				}
			}
		}
		return null;
	}

	@Override
	public @Nullable SqmAliasedNode<?> findAliasedNodeByPosition(int position) {
		// NOTE: 1-based
		return position > simpleSelectionNodes.size()
				? null
				: simpleSelectionNodes.get( position - 1 );
	}

	@Override
	public void register(SqmAliasedNode<?> node) {
		checkResultVariable( node );
		simpleSelectionNodes.add( node );
	}

	private void checkResultVariable(SqmAliasedNode<?> selection) {
		final String alias = selection.getAlias();
		if ( alias != null ) {
			final Integer position = findAliasedNodePosition( alias );
			if ( position != null ) {
				throw new AliasCollisionException(
						String.format(
								Locale.ENGLISH,
								"Duplicate alias '%s' at position %s in 'select' clause",
								alias,
								position
						)
				);
			}
		}
	}
}

package org.hibernate.query.hql.internal;

import org.hibernate.query.PathException;
import org.hibernate.query.SemanticException;
import org.hibernate.query.hql.spi.DotIdentifierConsumer;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.spi.SqmJoinable;
import org.hibernate.query.sqm.spi.SqmCreationHelper;
import org.hibernate.query.sqm.tree.spi.SqmJoinType;
import org.hibernate.query.sqm.tree.spi.domain.AbstractSqmAttributeJoin;
import org.hibernate.query.sqm.tree.spi.domain.SqmPath;
import org.hibernate.query.sqm.tree.spi.domain.SqmPolymorphicRootDescriptor;
import org.hibernate.query.sqm.tree.spi.domain.SqmTreatedFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmCteJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmEntityJoin;
import org.hibernate.query.sqm.tree.spi.from.SqmFrom;
import org.hibernate.query.sqm.tree.spi.from.SqmRoot;

import org.hibernate.query.sqm.tree.spi.from.SqmTreatedAttributeJoin;

import static org.hibernate.query.sqm.internal.SqmUtil.findCompatibleFetchJoin;

/**
 * Specialized "intermediate" SemanticPathPart for processing domain model paths.
 *
 * @author Steve Ebersole
 */
public class QualifiedJoinPathConsumer implements DotIdentifierConsumer {
	private final SqmCreationState creationState;
	private final SqmRoot<?> sqmRoot;

	private final SqmJoinType joinType;
	private final boolean fetch;
	private final String alias;
	private final boolean allowReuse;

	private ConsumerDelegate delegate;
	private boolean nested;

	public QualifiedJoinPathConsumer(
			SqmRoot<?> sqmRoot,
			SqmJoinType joinType,
			boolean fetch,
			String alias,
			boolean allowReuse,
			SqmCreationState creationState) {
		this.sqmRoot = sqmRoot;
		this.joinType = joinType;
		this.fetch = fetch;
		this.alias = alias;
		this.allowReuse = allowReuse;
		this.creationState = creationState;
	}

	public QualifiedJoinPathConsumer(
			SqmFrom<?, ?> sqmFrom,
			SqmJoinType joinType,
			boolean fetch,
			String alias,
			SqmCreationState creationState) {
		this.sqmRoot = null;
		this.joinType = joinType;
		this.fetch = fetch;
		this.alias = alias;
		// This constructor is only used for entity names, so no need for join reuse
		this.allowReuse = false;
		this.creationState = creationState;
		this.delegate = new AttributeJoinDelegate( sqmFrom );
	}

	public boolean isNested() {
		return nested;
	}

	public void setNested(boolean nested) {
		this.nested = nested;
	}

	@Override
	public SemanticPathPart getConsumedPart() {
		return delegate.getConsumedPart();
	}

	@Override
	public void consumeIdentifier(String identifier, boolean isBase, boolean isTerminal) {
		if ( isBase && delegate == null ) {
			delegate = resolveBase( identifier, !nested && isTerminal );
		}
		else {
			assert delegate != null;
			delegate.consumeIdentifier(
					identifier,
					!nested && isTerminal,
					// Non-nested joins shall allow reuse, but nested ones (i.e. in treat)
					// only allow join reuse for non-terminal parts
					allowReuse && (!nested || !isTerminal)
			);
		}
	}

	@Override
	public void consumeTreat(String entityName, boolean isTerminal) {
		assert delegate != null;
		delegate.consumeTreat( entityName, isTerminal );
	}

	private ConsumerDelegate resolveBase(String identifier, boolean isTerminal) {
		final var processingState = creationState.getCurrentProcessingState();
		final var pathRegistry = processingState.getPathRegistry();
		final SqmFrom<?, Object> pathRootByAlias = pathRegistry.findFromByAlias( identifier, true );
		final var pathRoot = pathRootByAlias != null ? pathRootByAlias : pathRegistry.findFromExposing( identifier );
		if ( pathRoot == null ) {
			return resolveEntityName( identifier, isTerminal );
		}
		// An unqualified attribute may resolve directly against the parent query's path registry.
		if ( (joinType == SqmJoinType.RIGHT || joinType == SqmJoinType.FULL)
				&& ( pathRoot.isCorrelated() || pathRootByAlias == null
						&& pathRegistry.findFromByPath( pathRoot.getNavigablePath() ) != pathRoot ) ) {
			throw new SemanticException( "A " + joinType + " join to an outer query path is not supported",
					creationState instanceof SemanticQueryBuilder<?> builder ? builder.getQuery() : null );
		}
		return pathRootByAlias == null
				? resolveExposed( identifier, isTerminal, pathRoot )
				: resolveAlias( identifier, isTerminal, pathRootByAlias );
	}

	private ExpectingEntityJoinDelegate resolveEntityName(String identifier, boolean isTerminal) {
		return new ExpectingEntityJoinDelegate(
				identifier,
				isTerminal,
				sqmRoot,
				joinType,
				alias,
				fetch,
				creationState
		);
	}

	private AttributeJoinDelegate resolveExposed(String identifier, boolean isTerminal, SqmFrom<?, ?> pathRoot) {
		return new AttributeJoinDelegate( createJoin( pathRoot, identifier, isTerminal, true ) );
	}

	private AttributeJoinDelegate resolveAlias(String identifier, boolean isTerminal, SqmFrom<?, Object> pathRootByAlias) {
		// identifier is an alias (identification variable)
		if (isTerminal) {
			throw new SemanticException( "Cannot join to root entity '" + identifier + "'",
					creationState instanceof SemanticQueryBuilder<?> builder ? builder.getQuery() : null );
		}
		else {
			return new AttributeJoinDelegate( pathRootByAlias );
		}
	}

	private <U> SqmFrom<?, ?> createJoin(
			SqmFrom<?, U> lhs,
			String name,
			boolean isTerminal,
			boolean allowReuse) {
		final var subPathSource = lhs.getResolvedModel().getSubPathSource( name, true );
		if ( allowReuse ) {
			if ( !isTerminal ) {
				for ( var sqmJoin : lhs.getSqmJoins() ) {
					// In order for an HQL join to be reusable, it must have the same path source,
					if ( sqmJoin.getModel() == subPathSource
						// and must not have a join condition.
						&& sqmJoin.getJoinPredicate() == null ) {
						if ( !lhs.isCorrelated() || sqmRoot == null || sqmJoin.findRoot() == sqmRoot ) {
							return sqmJoin;
						}
						// The same outer path in a different FROM item needs its own join and alias.
						allowReuse = false;
					}
				}
			}
			else if ( fetch ) {
				final var compatibleFetchJoin = findCompatibleFetchJoin( lhs, subPathSource, joinType );
				if ( compatibleFetchJoin != null ) {
					if ( alias != null ) {
						throw new IllegalStateException( "Cannot fetch the same association twice with a different alias" );
					}
					return compatibleFetchJoin;
				}
			}
		}
		if ( !(subPathSource instanceof SqmJoinable) ) {
			throw new SemanticException( "Joining on basic value elements is not supported",
					((SemanticQueryBuilder<?>) creationState).getQuery() );
		}
		@SuppressWarnings("unchecked")
		final var joinSource = (SqmJoinable<U, ?>) subPathSource;
		return createJoin( lhs, isTerminal, allowReuse, joinSource );
	}

	private <U,V> SqmFrom<?, ?> createJoin(
			SqmFrom<?,U> lhs,
			boolean isTerminal,
			boolean allowReuse,
			SqmJoinable<U,V> joinSource) {
		final var join = joinSource.createSqmJoin(
				lhs,
				joinType,
				isTerminal ? alias : allowReuse ? SqmCreationHelper.IMPLICIT_ALIAS : null,
				isTerminal && fetch,
				creationState
		);
		if ( lhs.isCorrelated() && sqmRoot != null && !sqmRoot.isCorrelated()
				&& join instanceof AbstractSqmAttributeJoin<?, ?> attributeJoin ) {
			attributeJoin.setDeclaringRoot( sqmRoot );
		}
		lhs.addSqmJoin( join );
		creationState.getCurrentProcessingState().getPathRegistry().register( join );
		return join;
	}

	private interface ConsumerDelegate {
		void consumeIdentifier(String identifier, boolean isTerminal, boolean allowReuse);
		void consumeTreat(String typeName, boolean isTerminal);
		SemanticPathPart getConsumedPart();
	}

	private class AttributeJoinDelegate implements ConsumerDelegate {
		private SqmFrom<?, ?> currentPath;

		private AttributeJoinDelegate(SqmFrom<?, ?> base) {
			this.currentPath = base;
		}

		@Override
		public void consumeIdentifier(String identifier, boolean isTerminal, boolean allowReuse) {
			currentPath = createJoin( currentPath, identifier, isTerminal, allowReuse );
		}

		@Override
		public void consumeTreat(String typeName, boolean isTerminal) {
			currentPath = treat( typeName, isTerminal );
			creationState.getCurrentProcessingState().getPathRegistry().register( currentPath );
		}

		private SqmTreatedFrom<?, ?, ?> treat(String typeName, boolean isTerminal) {
			if ( isTerminal ) {
				return fetch
						? treatTerminalFetch( currentPath, typeName )
						: treatTerminal( currentPath, typeName );
			}
			else {
				return treatNonTerminal( currentPath, typeName );
			}
		}

		private <L,R> SqmTreatedFrom<?, ?, ?> treatNonTerminal(SqmFrom<L,R> path, String typeName) {
			return path.treatAs( treatTarget( path, typeName ) );
		}

		private <L,R> SqmTreatedAttributeJoin<?, ?, ?> treatTerminalFetch(SqmFrom<L,R> path, String typeName) {
			final var attributeJoin = (SqmAttributeJoin<L,R>) path;
			return attributeJoin.treatAs( treatTarget( path, typeName ), alias, true );
		}

		private <L,R> SqmTreatedFrom<?, ?, ?> treatTerminal(SqmFrom<L,R> path, String typeName) {
			return path.treatAs( treatTarget( path, typeName ), alias );
		}

		private <T> Class<? extends T> treatTarget(SqmPath<T> path, String typeName) {
			final var javaType =
					creationState.getCreationContext().getJpaMetamodel()
							.managedType( typeName ).getJavaType();
			return javaType.asSubclass( path.getJavaType() );
		}

		@Override
		public SemanticPathPart getConsumedPart() {
			return currentPath;
		}
	}

	private static class ExpectingEntityJoinDelegate implements ConsumerDelegate {
		private final SqmCreationState creationState;
		private final SqmRoot<?> sqmRoot;

		private final SqmJoinType joinType;
		private final boolean fetch;
		private final String alias;

		private final StringBuilder path = new StringBuilder();

		private SqmPath<?> join;

		public ExpectingEntityJoinDelegate(
				String identifier,
				boolean isTerminal,
				SqmRoot<?> sqmRoot,
				SqmJoinType joinType,
				String alias,
				boolean fetch,
				SqmCreationState creationState) {
			this.creationState = creationState;
			this.sqmRoot = sqmRoot;
			this.joinType = joinType;
			this.fetch = fetch;
			this.alias = alias;

			consumeIdentifier( identifier, isTerminal, true );
		}

		@Override
		public void consumeIdentifier(String identifier, boolean isTerminal, boolean allowReuse) {
			if ( !path.isEmpty() ) {
				path.append( '.' );
			}
			path.append( identifier );
			if ( isTerminal ) {
				final String fullPath = path.toString();
				final var joinedEntityType =
						creationState.getCreationContext().getJpaMetamodel()
								.getHqlEntityReference( fullPath );
				if ( joinedEntityType == null ) {
					final var cteStatement = creationState.findCteStatement( fullPath );
					if ( cteStatement != null ) {
						if ( fetch ) {
							throw new SemanticException(
									"Fetch join target '" + fullPath + "' is a CTE (only associations may be fetched)",
									creationState instanceof SemanticQueryBuilder<?> builder ? builder.getQuery() : null
							);
						}
						//noinspection rawtypes,unchecked
						join = new SqmCteJoin( cteStatement, alias, joinType, sqmRoot );
						creationState.getCurrentProcessingState().getPathRegistry().register( join );
						return;
					}
					throw new PathException( "Could not resolve join path '" + fullPath + "'" );
				}

				assert ! ( joinedEntityType instanceof SqmPolymorphicRootDescriptor );

				if ( fetch ) {
					throw new SemanticException(
							"Fetch join target '" + fullPath + "' is an entity (only associations may be fetched)",
							creationState instanceof SemanticQueryBuilder<?> builder ? builder.getQuery() : null
					);
				}

				join = new SqmEntityJoin<>( joinedEntityType, alias, joinType, sqmRoot );
				creationState.getCurrentProcessingState().getPathRegistry().register( join );
			}
		}

		@Override
		public void consumeTreat(String typeName, boolean isTerminal) {
			throw new UnsupportedOperationException();
		}

		@Override
		public SemanticPathPart getConsumedPart() {
			return join;
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;

/**
 * @author Steve Ebersole
 */
public class DomainResultCreationStateImpl implements DomainResultCreationState {
	private final List<ResultBuilder> resultBuilders;
	private final Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders;
	private final SqlAstCreationStateImpl sqlAstCreationState;
	private final FromClauseAccessImpl fromClauseAccess;

	private final LegacyFetchResolverImpl legacyFetchResolver;

	public DomainResultCreationStateImpl(
			List<ResultBuilder> resultBuilders,
			Map<String, Map<String, DynamicFetchBuilderLegacy>> legacyFetchBuilders,
			SessionFactoryImplementor sessionFactory) {
		this.resultBuilders = resultBuilders;
		this.legacyFetchBuilders = legacyFetchBuilders;
		this.fromClauseAccess = new FromClauseAccessImpl();
		this.sqlAstCreationState = new SqlAstCreationStateImpl( fromClauseAccess, sessionFactory );

		this.legacyFetchResolver = new LegacyFetchResolverImpl();
	}

	public FromClauseAccessImpl getFromClauseAccess() {
		return fromClauseAccess;
	}

	@Override
	public SqlAstCreationStateImpl getSqlAstCreationState() {
		return sqlAstCreationState;
	}

	@FunctionalInterface
	public interface LegacyFetchResolver {
		DynamicFetchBuilderLegacy resolve(String ownerTableAlias, String fetchedPartPath);
	}

	private static class LegacyFetchResolverImpl implements LegacyFetchResolver {
		private final Map<String,Map<String, DynamicFetchBuilderLegacy>> legacyFetchResolvers;

		public LegacyFetchResolverImpl() {
			this.legacyFetchResolvers = new HashMap<>();
		}

		@Override
		public DynamicFetchBuilderLegacy resolve(String ownerTableAlias, String fetchedPartPath) {
			final Map<String, DynamicFetchBuilderLegacy> fetchBuilders = legacyFetchResolvers.get( ownerTableAlias );
			if ( fetchBuilders == null ) {
				return null;
			}

			return fetchBuilders.get( fetchedPartPath );
		}
	}

	public LegacyFetchResolver getLegacyFetchResolver() {
		return legacyFetchResolver;
	}

	@Override
	public List<Fetch> visitFetches(FetchParent fetchParent) {
		throw new UnsupportedOperationException();
	}

}

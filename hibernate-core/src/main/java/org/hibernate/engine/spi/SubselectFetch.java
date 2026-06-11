/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.graph.spi.AppliedGraph;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;

/**
 * Encapsulates details related to entities which contain sub-select-fetchable
 * collections and which were loaded in a Session so that those collections may
 * be sub-select fetched later during initialization
 */
public class SubselectFetch {
	private final QuerySpec loadingSqlAst;
	private final TableGroup ownerTableGroup;
	private final JdbcParametersList loadingJdbcParameters;
	private final JdbcParameterBindings loadingJdbcParameterBindings;
	private final Set<EntityKey> resultingEntityKeys;

	public SubselectFetch(
			QuerySpec loadingSqlAst,
			TableGroup ownerTableGroup,
			JdbcParametersList loadingJdbcParameters,
			JdbcParameterBindings loadingJdbcParameterBindings,
			Set<EntityKey> resultingEntityKeys) {
		this.loadingSqlAst = loadingSqlAst;
		this.ownerTableGroup = ownerTableGroup;
		this.loadingJdbcParameters = loadingJdbcParameters;
		this.loadingJdbcParameterBindings = loadingJdbcParameterBindings;
		this.resultingEntityKeys = resultingEntityKeys;
	}

	public JdbcParametersList getLoadingJdbcParameters() {
		// todo (6.0) : do not believe this is needed
		// 		- see org.hibernate.loader.ast.internal.LoaderSelectBuilder.generateSelect(org.hibernate.engine.spi.SubselectFetch)
		return loadingJdbcParameters;
	}

	/**
	 * The SQL AST select from which the owner was loaded
	 */
	public QuerySpec getLoadingSqlAst() {
		return loadingSqlAst;
	}

	/**
	 * The TableGroup for the owner within the {@link #getLoadingSqlAst()}
	 */
	public TableGroup getOwnerTableGroup() {
		return ownerTableGroup;
	}

	/**
	 * The JDBC parameter bindings related to {@link #getLoadingSqlAst()} for
	 * the specific execution that loaded the owners
	 */
	public JdbcParameterBindings getLoadingJdbcParameterBindings() {
		return loadingJdbcParameterBindings;
	}

	/**
	 *The entity-keys of all owners loaded from a particular execution
	 * <p>
	 * Used for "empty collection" handling mostly
	 */
	public Set<EntityKey> getResultingEntityKeys() {
		return resultingEntityKeys;
	}

	@Override
	public String toString() {
		return "SubselectFetch(" + ownerTableGroup.getNavigablePath() + ")";
	}

	public static RegistrationHandler createRegistrationHandler(
			BatchFetchQueue batchFetchQueue,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			JdbcParameterBindings jdbcParameterBindings) {
		return createRegistrationHandler( batchFetchQueue, sqlAst, jdbcParameters, jdbcParameterBindings, null );
	}

	public static RegistrationHandler createRegistrationHandler(
			BatchFetchQueue batchFetchQueue,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			JdbcParameterBindings jdbcParameterBindings,
			AppliedGraph appliedGraph) {
		// we allow this now
		return sqlAst.getQuerySpec().getFromClause().getRoots().isEmpty()
				? NO_OP_REG_HANDLER
				: new StandardRegistrationHandler( batchFetchQueue, sqlAst,
						jdbcParameters, jdbcParameterBindings, appliedGraph );
	}

	public interface RegistrationHandler {
		void addKey(EntityHolder holder);
	}

	private static final RegistrationHandler NO_OP_REG_HANDLER = holder -> {};

	public static class StandardRegistrationHandler implements RegistrationHandler {
		private final BatchFetchQueue batchFetchQueue;
		private final SelectStatement loadingSqlAst;
		private final JdbcParametersList loadingJdbcParameters;
		private final JdbcParameterBindings loadingJdbcParameterBindings;
		private final AppliedGraph appliedGraph;
		private final Map<NavigablePath, SubselectFetch> subselectFetches = new HashMap<>();

		private StandardRegistrationHandler(
				BatchFetchQueue batchFetchQueue,
				SelectStatement loadingSqlAst,
				JdbcParametersList loadingJdbcParameters,
				JdbcParameterBindings loadingJdbcParameterBindings,
				AppliedGraph appliedGraph) {
			this.batchFetchQueue = batchFetchQueue;
			this.loadingSqlAst = loadingSqlAst;
			this.loadingJdbcParameters = loadingJdbcParameters;
			this.loadingJdbcParameterBindings = loadingJdbcParameterBindings;
			this.appliedGraph = appliedGraph;
		}

		@Override
		public void addKey(EntityHolder holder) {
			if ( batchFetchQueue.getSession().getLoadQueryInfluencers()
					.hasSubselectLoadableCollections( holder.getDescriptor(), appliedGraph ) ) {
				final var path = castNonNull( holder.getEntityInitializer() ).getNavigablePath();
				final var querySpec = loadingSqlAst.getQuerySpec();
				final var subselectFetch = subselectFetches.computeIfAbsent(
						path,
						navigablePath -> new SubselectFetch(
								querySpec,
								querySpec.getFromClause().findTableGroup( path ),
								loadingJdbcParameters,
								loadingJdbcParameterBindings,
								new HashSet<>()
						)
				);
				final var entityKey = holder.getEntityKey();
				subselectFetch.resultingEntityKeys.add( entityKey );
				batchFetchQueue.addSubselect( entityKey, subselectFetch );
			}
		}
	}
}

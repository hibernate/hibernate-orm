/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.engine.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.internal.util.NullnessUtil;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.entity.EntityInitializer;

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
	 *
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
			TableGroup tableGroup,
			JdbcParametersList jdbcParameters,
			JdbcParameterBindings jdbcParameterBindings) {

		return new StandardRegistrationHandler(
				batchFetchQueue,
				sqlAst,
				tableGroup,
				jdbcParameters,
				jdbcParameterBindings
		);
	}

	public static RegistrationHandler createRegistrationHandler(
			BatchFetchQueue batchFetchQueue,
			SelectStatement sqlAst,
			JdbcParametersList jdbcParameters,
			JdbcParameterBindings jdbcParameterBindings) {
		final List<TableGroup> roots = sqlAst.getQuerySpec().getFromClause().getRoots();
		if ( roots.isEmpty() ) {
			// we allow this now
			return NO_OP_REG_HANDLER;
		}

		return createRegistrationHandler( batchFetchQueue, sqlAst, roots.get( 0 ), jdbcParameters, jdbcParameterBindings );
	}

	public interface RegistrationHandler {
		void addKey(EntityHolder holder);
	}

	private static final RegistrationHandler NO_OP_REG_HANDLER = new RegistrationHandler() {
		@Override
		public void addKey(EntityHolder holder) {
		}
	} ;

	public static class StandardRegistrationHandler implements RegistrationHandler {
		private final BatchFetchQueue batchFetchQueue;
		private final SelectStatement loadingSqlAst;
		private final JdbcParametersList loadingJdbcParameters;
		private final JdbcParameterBindings loadingJdbcParameterBindings;
		private final Map<NavigablePath, SubselectFetch> subselectFetches = new HashMap<>();

		private StandardRegistrationHandler(
				BatchFetchQueue batchFetchQueue,
				SelectStatement loadingSqlAst,
				TableGroup ownerTableGroup,
				JdbcParametersList loadingJdbcParameters,
				JdbcParameterBindings loadingJdbcParameterBindings) {
			this.batchFetchQueue = batchFetchQueue;
			this.loadingSqlAst = loadingSqlAst;
			this.loadingJdbcParameters = loadingJdbcParameters;
			this.loadingJdbcParameterBindings = loadingJdbcParameterBindings;
		}

		@Override
		public void addKey(EntityHolder holder) {
			if ( batchFetchQueue.getSession().getLoadQueryInfluencers()
					.hasSubselectLoadableCollections( holder.getDescriptor() ) ) {
				final EntityInitializer<?> entityInitializer = NullnessUtil.castNonNull( holder.getEntityInitializer() );
				final SubselectFetch subselectFetch = subselectFetches.computeIfAbsent(
						entityInitializer.getNavigablePath(),
						navigablePath -> new SubselectFetch(
								loadingSqlAst.getQuerySpec(),
								loadingSqlAst.getQuerySpec()
										.getFromClause()
										.findTableGroup( entityInitializer.getNavigablePath() ),
								loadingJdbcParameters,
								loadingJdbcParameterBindings,
								new HashSet<>()
						)
				);
				subselectFetch.resultingEntityKeys.add( holder.getEntityKey() );
				batchFetchQueue.addSubselect( holder.getEntityKey(), subselectFetch );
			}
		}
	}
}

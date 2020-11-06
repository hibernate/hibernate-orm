/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import java.util.HashSet;
import java.util.List;

import org.hibernate.engine.spi.BatchFetchQueue;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SubselectFetch;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;

/**
 * Given as the target for {@link ExecutionContext#registerLoadingEntityEntry} calls when
 * performing multi-loads to apply sub-select fetching support.
 */
public class LoadingEntityCollector {

	private final SubselectFetch subselectFetch;
	private final BatchFetchQueue batchFetchQueue;

	LoadingEntityCollector(
			EntityValuedModelPart loadingEntityDescriptor,
			QuerySpec loadingSqlAst,
			List<JdbcParameter> jdbcParameters,
			JdbcParameterBindings jdbcParameterBindings,
			BatchFetchQueue batchFetchQueue) {
		this.batchFetchQueue = batchFetchQueue;
		this.subselectFetch = new SubselectFetch(
				loadingEntityDescriptor,
				loadingSqlAst,
				loadingSqlAst.getFromClause().getRoots().get( 0 ),
				jdbcParameters,
				jdbcParameterBindings,
				new HashSet<>()
		);

	}

	public void collectLoadingEntityKey(EntityKey entityKey) {
		subselectFetch.getResultingEntityKeys().add( entityKey );
		batchFetchQueue.addSubselect( entityKey, subselectFetch );
	}
}

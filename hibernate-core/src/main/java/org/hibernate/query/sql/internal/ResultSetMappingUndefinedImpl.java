/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.sql.spi.ast.SqlSelectionImpl;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.select.QueryResult;
import org.hibernate.sql.ast.tree.spi.select.ResolvedResultSetMapping;
import org.hibernate.sql.ast.tree.spi.select.ResultSetAccess;
import org.hibernate.sql.ast.tree.spi.select.SqlSelection;
import org.hibernate.sql.exec.spi.ResultSetMapping;

/**
 * @author Steve Ebersole
 */
public class ResultSetMappingUndefinedImpl implements ResultSetMapping {
	@Override
	public ResolvedResultSetMapping resolve(
			ResultSetAccess jdbcResultsAccess,
			SharedSessionContractImplementor persistenceContext) {
		final int columnCount = jdbcResultsAccess.getColumnCount();
		final List<SqlSelection> sqlSelections = CollectionHelper.arrayList( columnCount );
		final List<QueryResult> queryResults = CollectionHelper.arrayList( columnCount );

		for ( int columnPosition = 0; columnPosition < columnCount; columnPosition++ ) {
			final String columnName = jdbcResultsAccess.resolveColumnName( columnPosition );
			final SqlSelection sqlSelection = new SqlSelectionImpl( columnName, columnPosition );
			sqlSelections.add( sqlSelection );

			// todo (6.0) - need to build the QueryResultScalar
			//		however, QueryResultScalar is currently very BasicType-centric
			//		which we need to change.  I think this is as simple as
			//		moving the stuff we need to BasicValuedExpressableType
			//		and dropping BasicValuedExpressableType#getBasicType

			throw new NotYetImplementedException(  );
//			queryResults.add( ... );
		}

		return new ResolvedResultSetMapping() {
			@Override
			public List<SqlSelection> getSqlSelections() {
				return sqlSelections;
			}

			@Override
			public List<QueryResult> getQueryResults() {
				return queryResults;
			}
		};
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.internal;

import java.util.List;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.query.sql.spi.ResolvingSqlSelectionImpl;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.ResultSetMapping;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.ResultSetMappingDescriptor;

/**
 * @author Steve Ebersole
 */
public class ResultSetMappingDescriptorUndefined implements ResultSetMappingDescriptor {
	@Override
	public ResultSetMapping resolve(
			JdbcValuesMetadata jdbcResultsMetadata,
			ResolutionContext resolutionContext) {
		final int columnCount = jdbcResultsMetadata.getColumnCount();
		final List<SqlSelection> sqlSelections = CollectionHelper.arrayList( columnCount );
		final List<QueryResult> queryResults = CollectionHelper.arrayList( columnCount );

		for ( int columnPosition = 0; columnPosition < columnCount; columnPosition++ ) {
			final String columnName = jdbcResultsMetadata.resolveColumnName( columnPosition );
			final SqlSelection sqlSelection = new ResolvingSqlSelectionImpl( columnName, columnPosition );
			sqlSelections.add( sqlSelection );

			// todo (6.0) - need to build the QueryResultScalar
			//		however, QueryResultScalar is currently very BasicType-centric
			//		which we need to change.  I think this is as simple as
			//		moving the stuff we need to BasicValuedExpressableType
			//		and dropping BasicValuedExpressableType#getBasicType

			throw new NotYetImplementedFor6Exception(  );
//			queryResults.add( ... );
		}

		return new ResultSetMapping() {
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

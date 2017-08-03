/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.internal.ScalarQueryResultImpl;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.SqlSelectable;
import org.hibernate.sql.results.spi.SqlSelectionReader;

/**
 * @author Steve Ebersole
 */
public class QueryResultBuilderScalar
		implements WrappableQueryResultBuilder, SqlSelectable {
	private final String columnName;
	private final BasicValuedExpressableType type;

	public QueryResultBuilderScalar(String columnName, BasicValuedExpressableType type) {
		this.columnName = columnName;
		this.type = type;
	}

	@Override
	public QueryResult buildReturn(NodeResolutionContext resolutionContext) {
		return new ScalarQueryResultImpl(
				columnName,
				resolutionContext.getSqlSelectionResolver().resolveSqlSelection( this ),
				type
		);
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return type.getBasicType().getSqlSelectionReader();
	}
}

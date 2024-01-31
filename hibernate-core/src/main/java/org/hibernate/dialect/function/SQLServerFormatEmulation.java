/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;
import jakarta.persistence.TemporalType;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server behaves strangely when the first argument to format is of the type time, so we cast to datetime.
 *
 * @author Christian Beikov
 */
public class SQLServerFormatEmulation extends FormatFunction {

	public SQLServerFormatEmulation(TypeConfiguration typeConfiguration) {
		super( "format", typeConfiguration );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final Expression datetime = (Expression) arguments.get(0);

		sqlAppender.appendSql("format(");
		if ( needsDateTimeCast( datetime ) ) {
			sqlAppender.appendSql("cast(");
			datetime.accept( walker );
			sqlAppender.appendSql(" as datetime)");
		}
		else {
			datetime.accept( walker );
		}
		sqlAppender.appendSql(',');
		arguments.get( 1 ).accept( walker );
		sqlAppender.appendSql(')');
	}

	private boolean needsDateTimeCast(Expression datetime) {
		final boolean isTime = TypeConfiguration.getSqlTemporalType( datetime.getExpressionType() ) == TemporalType.TIME;
		if ( isTime ) {
			// Since SQL Server has no dedicated type for time with time zone, we use the offsetdatetime which has a date part
			return datetime.getExpressionType()
					.getSingleJdbcMapping()
					.getJdbcType()
					.getDefaultSqlTypeCode() != SqlTypes.TIME_WITH_TIMEZONE;
		}
		return false;
	}
}

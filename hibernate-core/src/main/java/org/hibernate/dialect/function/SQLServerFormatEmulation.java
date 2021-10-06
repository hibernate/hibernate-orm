/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;
import jakarta.persistence.TemporalType;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * SQL Server behaves strangely when the first argument to format is of the type time, so we cast to datetime.
 *
 * @author Christian Beikov
 */
public class SQLServerFormatEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final SQLServerDialect dialect;

	public SQLServerFormatEmulation(SQLServerDialect dialect, TypeConfiguration typeConfiguration) {
		super(
				"format",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
		);
		this.dialect = dialect;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		final Expression datetime = (Expression) arguments.get(0);
		final boolean isTime = TypeConfiguration.getSqlTemporalType( datetime.getExpressionType() ) == TemporalType.TIME;
		final Format format = (Format) arguments.get(1);

		sqlAppender.appendSql("format(");
		if ( isTime ) {
			sqlAppender.appendSql("cast(");
			datetime.accept( walker );
			sqlAppender.appendSql(" as datetime)");
		}
		else {
			datetime.accept( walker );
		}
		sqlAppender.appendSql(",'");
		dialect.appendDatetimeFormat( sqlAppender, format.getFormat() );
		sqlAppender.appendSql("')");
	}

	@Override
	public String getArgumentListSignature() {
		return "(datetime as pattern)";
	}
}

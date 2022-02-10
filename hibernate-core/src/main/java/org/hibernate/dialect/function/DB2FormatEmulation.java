/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Format;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;
import jakarta.persistence.TemporalType;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.TEMPORAL;

/**
 * DB2's varchar_format() can't handle quoted literal strings in
 * the format pattern. So just split the pattern into bits, call
 * varcharformat() on the odd-numbered bits, and concatenate all
 * the nonempty bits at the end.
 *
 * @author Gavin King
 */
public class DB2FormatEmulation
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	public DB2FormatEmulation(TypeConfiguration typeConfiguration) {
		super(
				"format",
				CommonFunctionFactory.formatValidator(),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, TEMPORAL, STRING )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		final Expression datetime = (Expression) arguments.get(0);
		final boolean isTime = TypeConfiguration.getSqlTemporalType( datetime.getExpressionType() ) == TemporalType.TIME;
		final Format format = (Format) arguments.get(1);

		sqlAppender.appendSql("(");
		String[] bits = OracleDialect.datetimeFormat( format.getFormat(), false, false ).result().split("\"");
		boolean first = true;
		for ( int i=0; i<bits.length; i++ ) {
			String bit = bits[i];
			if ( !bit.isEmpty() ) {
				if ( first ) {
					first = false;
				}
				else {
					sqlAppender.appendSql("||");
				}
				if ( i % 2 == 0 ) {
					sqlAppender.appendSql("varchar_format(");
					// Times need to be wrapped into a timestamp to be able to use formatting
					if ( isTime ) {
						sqlAppender.appendSql( "timestamp(current_date," );
						datetime.accept( walker );
						sqlAppender.appendSql( ")" );
					}
					else {
						datetime.accept( walker );
					}
					sqlAppender.appendSql(",'");
					sqlAppender.appendSql( bit );
					sqlAppender.appendSql("')");
				}
				else {
					sqlAppender.appendSql("'");
					sqlAppender.appendSql( bit );
					sqlAppender.appendSql("'");
				}
			}
		}
		if ( first ) {
			sqlAppender.appendSql("''");
		}
		sqlAppender.appendSql(")");
	}

	@Override
	public String getArgumentListSignature() {
		return "(TEMPORAL datetime as STRING pattern)";
	}
}

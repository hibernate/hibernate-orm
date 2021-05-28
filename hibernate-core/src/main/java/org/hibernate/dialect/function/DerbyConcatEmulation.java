/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.sql.internal.SqmParameterInterpretation;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;

/**
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class DerbyConcatEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	public DerbyConcatEmulation() {
		super(
				"concat",
				StandardArgumentsValidators.min( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.STRING )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		assert arguments.size() > 0;

		boolean hasJdbcParameter = false;
		for (SqlAstNode argument : arguments) {
			if ( argument instanceof SqmParameterInterpretation ) {
				hasJdbcParameter = true;
				break;
			}
		}

		if ( hasJdbcParameter ) {
			sqlAppender.appendSql( "varchar" );
		}
		sqlAppender.appendSql( "( ");
		for ( int i = 0; i < arguments.size(); i++ ) {
			if ( i > 0 ) {
				sqlAppender.appendSql( " || " );
			}
			renderOperand( arguments.get( i ), sqlAppender, walker, hasJdbcParameter );
		}
		sqlAppender.appendSql( " )" );
	}

	private void renderOperand(SqlAstNode operand, SqlAppender sqlAppender, SqlAstTranslator<?> walker, boolean castRequired) {
		if ( castRequired ) {
			sqlAppender.appendSql( "cast" );
		}
		sqlAppender.appendSql( "( " );
		walker.render( operand, SqlAstNodeRenderingMode.DEFAULT );
		if ( castRequired ) {
			sqlAppender.appendSql( " as varchar(32672)" );
		}
		sqlAppender.appendSql( " )" );
	}

	@Override
	public String getArgumentListSignature() {
		return "(string0[ ,string1[ ,string2[...]]])";
	}

}

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
 * A specialized concat() function definition in which:<ol>
 *     <li>we translate to use the concat operator ('||')</li>
 *     <li>wrap dynamic parameters in CASTs to VARCHAR</li>
 * </ol>
 * <p/>
 * This last spec is to deal with a limitation on DB2 and variants (e.g. Derby)
 * where dynamic parameters cannot be used in concatenation unless they are being
 * concatenated with at least one non-dynamic operand.  And even then, the rules
 * are so convoluted as to what is allowed and when the CAST is needed and when
 * it is not that we just go ahead and do the CASTing.
 *
 * @author Steve Ebersole
 * @author Nathan Xu
 */
public class DerbyConcatFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public DerbyConcatFunction() {
		super(
				"concat",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.STRING )
		);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Here's the meat..  The whole reason we have a separate impl for this for Derby is to re-define
	 * this method.  The logic here says that if all incoming args are not dynamic parameters
	 * (i.e. <tt>?</tt>) then we simply use the Derby concat operator (<tt>||</tt>) on the unchanged
	 * arg elements.  However, if any arg is dynamic parameters, then we need to wrap the individual
	 * arg elements in <tt>cast</tt> function calls, use the concatenation operator on the <tt>cast</tt>
	 * returns, and then wrap that whole thing in a call to the Derby <tt>varchar</tt> function.
	 */
	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		final SqlAstNode leftOperand = arguments.get( 0 );
		final SqlAstNode rightOperand = arguments.get( 1 );
		final boolean hasJdbcParameter = leftOperand instanceof SqmParameterInterpretation || rightOperand instanceof SqmParameterInterpretation;
		if ( hasJdbcParameter ) {
			sqlAppender.appendSql( "varchar( ");
			renderOperandInCastingMode( leftOperand, sqlAppender, walker );
			sqlAppender.appendSql( " || " );
			renderOperandInCastingMode( rightOperand, sqlAppender, walker );
			sqlAppender.appendSql( " )" );
		}
		else {
			sqlAppender.appendSql( "( " );
			walker.render( leftOperand, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.appendSql( " || " );
			walker.render( rightOperand, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
			sqlAppender.appendSql( " )" );
		}
	}

	private void renderOperandInCastingMode(SqlAstNode operand, SqlAppender sqlAppender, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "cast( " );
		walker.render( operand, operand instanceof SqmParameterInterpretation ? SqlAstNodeRenderingMode.DEFAULT : SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.appendSql( " as varchar(32672) )" );
	}

	@Override
	public String getArgumentListSignature() {
		return "(string, string)";
	}
}

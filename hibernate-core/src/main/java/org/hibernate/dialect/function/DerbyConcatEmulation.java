/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderingSupport;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;

import java.util.List;

/**
 * Casts query parameters using the Derby varchar() function
 * before concatenating them using the || operator.
 *
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DerbyConcatEmulation
		extends AbstractSqmSelfRenderingFunctionDescriptor
		implements FunctionRenderingSupport {

	public DerbyConcatEmulation() {
		super(
				"concat",
				StandardArgumentsValidators.min( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.STRING )
		);
	}

	@Override
	public FunctionRenderingSupport getRenderingSupport() {
		return this;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstWalker walker) {
		int numberOfArguments = arguments.size();
		if ( numberOfArguments > 1 ) {
			sqlAppender.appendSql("(");
		}
		for ( int i = 0; i < numberOfArguments; i++ ) {
			SqlAstNode argument = arguments.get( i );
			if ( i > 0 ) {
				sqlAppender.appendSql("||");
			}
			boolean param = false; //TODO: argument instanceof GenericParameter;
			if ( param ) {
				sqlAppender.appendSql("cast(");
			}
			argument.accept(walker);
			if ( param ) {
				sqlAppender.appendSql(" as long varchar)");
			}
		}
		if ( numberOfArguments > 1 ) {
			sqlAppender.appendSql(")");
		}
	}

	@Override
	public String getArgumentListSignature() {
		return "(string0[, string1[, ...]])";
	}
}

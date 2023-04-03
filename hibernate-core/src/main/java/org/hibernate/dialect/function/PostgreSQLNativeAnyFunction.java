/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.JavaObjectType;

/**
 * PostgresQL native any function.
 *
 * @author Yanming Zhou
 */
public class PostgreSQLNativeAnyFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public PostgreSQLNativeAnyFunction() {
		super("any", FunctionKind.AGGREGATE, StandardArgumentsValidators.exactly(1),
				StandardFunctionReturnTypeResolvers.invariant(JavaObjectType.INSTANCE), null);
	}

	@Override
	public void render(SqlAppender sqlAppender, List<? extends SqlAstNode> arguments, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql("any(");
		arguments.get(0).accept(walker);
		sqlAppender.appendSql(")");
	}

	@Override
	public String getArgumentListSignature() {
		return "";
	}

}

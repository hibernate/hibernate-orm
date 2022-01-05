/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.BasicType;

import java.util.List;

/**
 * @author Gavin King
 */
public class CurrentFunction
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final String sql;

	public CurrentFunction(String name, String sql, BasicType<?> type) {
		super(
				name,
				StandardArgumentsValidators.NO_ARGS,
				StandardFunctionReturnTypeResolvers.invariant( type )
		);
		this.sql = sql;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( sql );
	}

	@Override
	public String getArgumentListSignature() {
		return "";
	}

	@Override
	public boolean alwaysIncludesParentheses() {
		return sql.indexOf( '(' ) != -1;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingSqmFunction;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Jan Schatteman
 */
public class EveryAnyEmulation extends AbstractSqmSelfRenderingFunctionDescriptor {

	private final boolean every;

	public EveryAnyEmulation(boolean every) {
		super(
				every ? "every" : "any",
				true,
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( StandardBasicTypes.BOOLEAN )
		);
		this.every = every;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> sqlAstArguments,
			Predicate filter,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "(sum(case when " );
		if ( filter != null ) {
			filter.accept( walker );
			sqlAppender.appendSql( " then case when " );
			sqlAstArguments.get( 0 ).accept( walker );
			if ( every ) {
				sqlAppender.appendSql( " then 0 else 1 end else null end)=0)" );
			}
			else {
				sqlAppender.appendSql( " then 1 else 0 end else null end)>0)" );
			}
		}
		else {
			sqlAstArguments.get( 0 ).accept( walker );
			if ( every ) {
				sqlAppender.appendSql( " then 0 else 1 end)=0)" );
			}
			else {
				sqlAppender.appendSql( " then 1 else 0 end)>0)" );
			}
		}
	}

	@Override
	public void render(
			SqlAppender sqlAppender, List<SqlAstNode> sqlAstArguments, SqlAstTranslator<?> walker) {
		this.render( sqlAppender, sqlAstArguments, null, walker );
	}
}

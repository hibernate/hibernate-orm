/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import java.util.List;

import org.hibernate.dialect.function.CastFunction;
import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Oracle json_object function.
 */
public class OracleJsonObjectFunction extends JsonObjectFunction {

	private final CastTarget stringCastTarget;
	private CastFunction castFunction;

	public OracleJsonObjectFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true );
		this.stringCastTarget = new CastTarget( typeConfiguration.getBasicTypeForJavaType( String.class ) );
	}

	@Override
	protected void renderValue(SqlAppender sqlAppender, SqlAstNode value, SqlAstTranslator<?> walker) {
		if ( ExpressionTypeHelper.isNonNativeBoolean( value ) ) {
			CastFunction castFunction = this.castFunction;
			if ( castFunction == null ) {
				castFunction = this.castFunction = (CastFunction) walker.getSessionFactory()
						.getQueryEngine()
						.getSqmFunctionRegistry()
						.findFunctionDescriptor( "cast" );
			}
			castFunction.render(
					sqlAppender,
					List.of( value, stringCastTarget ),
					(ReturnableType<?>) stringCastTarget.getJdbcMapping(),
					walker
			);
			sqlAppender.appendSql( " format json" );
		}
		else {
			value.accept( walker );
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

public class SDOGetGeometryType extends OracleSpatialFunction {
	public SDOGetGeometryType(BasicTypeRegistry typeRegistry) {
		super(
				"GetGeometryType",
				true,
				StandardArgumentsValidators.exactly( 1 ),
				StandardFunctionReturnTypeResolvers.invariant( typeRegistry.resolve( StandardBasicTypes.STRING ) )
		);
	}
	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "CASE " );
		( (Expression) sqlAstArguments.get( 0 ) ).accept( walker );
		sqlAppender.appendSql( ".Get_GType() " );
		sqlAppender.appendSql( " WHEN 1 THEN 'ST_POINT'" );
		sqlAppender.appendSql( " WHEN 2 THEN 'ST_LINESTRING'" );
		sqlAppender.appendSql( " WHEN 3 THEN 'ST_POLYGON'" );
		sqlAppender.appendSql( " WHEN 5 THEN 'ST_MULTIPOINT'" );
		sqlAppender.appendSql( " WHEN 6 THEN 'ST_MULTILINESTRING'" );
		sqlAppender.appendSql( " WHEN 7 THEN 'ST_MULTIPOLYGON'" );
		sqlAppender.appendSql( " END" );
	}
}

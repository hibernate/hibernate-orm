/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.ANY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * DB2's position() function always requires a code unit before version 11.
 */
public class DB2PositionFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public DB2PositionFunction(TypeConfiguration typeConfiguration) {
		super(
				"position",
				new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 3 ), STRING, STRING, ANY ),
				StandardFunctionReturnTypeResolvers.invariant( typeConfiguration.getBasicTypeRegistry().resolve(
						StandardBasicTypes.INTEGER ) ),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, STRING )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final int argumentCount = arguments.size();
		sqlAppender.appendSql( "position(" );
		arguments.get( 0 ).accept( walker );
		for ( int i = 1; i < argumentCount; i++ ) {
			sqlAppender.appendSql( ',' );
			arguments.get( i ).accept( walker );
		}
		if ( argumentCount != 3 ) {
			sqlAppender.appendSql( ",codeunits32" );
		}
		sqlAppender.appendSql( ')' );
	}

	@Override
	public String getSignature(String name) {
		return "(STRING pattern in STRING string[, units]])";
	}
}

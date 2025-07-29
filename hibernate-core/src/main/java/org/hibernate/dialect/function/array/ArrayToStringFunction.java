/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.array;

import java.util.List;

import org.hibernate.query.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionKind;
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
 * @author Christian Beikov
 */
public class ArrayToStringFunction extends AbstractSqmSelfRenderingFunctionDescriptor {

	public ArrayToStringFunction(TypeConfiguration typeConfiguration) {
		super(
				"array_to_string",
				FunctionKind.NORMAL,
				StandardArgumentsValidators.composite(
					new ArgumentTypesValidator( StandardArgumentsValidators.between( 2, 3 ), ANY, STRING, STRING ),
					new ArrayAndElementArgumentValidator( 0, 2 )
				),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.composite(
						new ArrayAndElementArgumentTypeResolver( 0, 2 ),
						StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, ANY, STRING, STRING )
				)
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "array_to_string(" );
		sqlAstArguments.get( 0 ).accept( walker );
		sqlAppender.appendSql( ',' );
		sqlAstArguments.get( 1 ).accept( walker );
		if ( sqlAstArguments.size() > 2 ) {
			sqlAppender.appendSql( ',' );
			sqlAstArguments.get( 2 ).accept( walker );
		}
		sqlAppender.appendSql( ')' );
	}

}

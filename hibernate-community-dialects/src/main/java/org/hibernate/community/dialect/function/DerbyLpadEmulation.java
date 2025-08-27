/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.function;

import java.util.List;

import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentTypesValidator;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/**
 * A derby implementation for lpad.
 *
 * @author Christian Beikov
 */
public class DerbyLpadEmulation
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	public DerbyLpadEmulation(TypeConfiguration typeConfiguration) {
		super(
				"lpad",
				new ArgumentTypesValidator( StandardArgumentsValidators.exactly( 2 ), STRING, INTEGER ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				),
				StandardFunctionArgumentTypeResolvers.invariant( typeConfiguration, STRING, INTEGER )
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final SqlAstNode string = arguments.get( 0 );
		final SqlAstNode length = arguments.get( 1 );
		sqlAppender.appendSql( "case when length(" );
		walker.render( string, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.appendSql( ")<" );
		walker.render( length, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( " then substr(char(''," );
		// The char function for Derby always needs a literal value
		walker.render( length, SqlAstNodeRenderingMode.INLINE_PARAMETERS );
		sqlAppender.appendSql( ")||" );
		walker.render( string, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( ",length(" );
		walker.render( string, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.appendSql( ")+1) else " );
		walker.render( string, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.appendSql( " end" );
	}

	@Override
	public String getArgumentListSignature() {
		return "(STRNG string, INTEGER length)";
	}
}

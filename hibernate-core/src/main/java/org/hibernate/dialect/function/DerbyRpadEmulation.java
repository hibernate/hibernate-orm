/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * A derby implementation for rpad.
 *
 * @author Christian Beikov
 */
public class DerbyRpadEmulation
		extends AbstractSqmSelfRenderingFunctionDescriptor {

	public DerbyRpadEmulation(TypeConfiguration typeConfiguration) {
		super(
				"rpad",
				StandardArgumentsValidators.exactly( 2 ),
				StandardFunctionReturnTypeResolvers.invariant(
						typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
		);
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<SqlAstNode> arguments,
			SqlAstTranslator<?> walker) {
		final SqlAstNode string = arguments.get( 0 );
		final SqlAstNode length = arguments.get( 1 );
		sqlAppender.appendSql( "case when length(" );
		walker.render( string, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		sqlAppender.appendSql( ")<" );
		walker.render( length, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( " then substr(" );
		walker.render( string, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( "||char(''," );
		// The char function for Derby always needs a literal value
		walker.render( length, SqlAstNodeRenderingMode.INLINE_PARAMETERS );
		sqlAppender.appendSql( "),1," );
		walker.render( length, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( ") else " );
		walker.render( string, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( " end" );
	}

	@Override
	public String getArgumentListSignature() {
		return "(string, length)";
	}
}

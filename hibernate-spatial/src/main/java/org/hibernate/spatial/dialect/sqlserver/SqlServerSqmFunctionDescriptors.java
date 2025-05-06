/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.spatial.dialect.sqlserver;

import java.util.List;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.metamodel.model.domain.ReturnableType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionReturnTypeResolver;
import org.hibernate.query.sqm.produce.function.StandardArgumentsValidators;
import org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers;
import org.hibernate.spatial.BaseSqmFunctionDescriptors;
import org.hibernate.spatial.CommonSpatialFunction;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useFirstNonNull;

public class SqlServerSqmFunctionDescriptors extends BaseSqmFunctionDescriptors {
	private final BasicTypeRegistry typeRegistry;

	public SqlServerSqmFunctionDescriptors(FunctionContributions functionContributions) {
		super( functionContributions );
		typeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		//TODO -- after refactoring base class this should be removed
		map.clear();
		map.put(
				CommonSpatialFunction.ST_ASTEXT.getKey(),
				new Method( "STAsText", exactly( 1 ), invariant( StandardBasicTypes.STRING ) )
		);
		map.put(
				CommonSpatialFunction.ST_GEOMETRYTYPE.getKey(),
				new Method( "STGeometryType", exactly( 1 ), invariant( StandardBasicTypes.STRING ) )
		);
		map.put(
				CommonSpatialFunction.ST_DIMENSION.getKey(),
				new Method( "STDimension", exactly( 1 ), invariant( StandardBasicTypes.INTEGER ) )
		);

		map.put(
				CommonSpatialFunction.ST_ENVELOPE.getKey(),
				new Method( "STEnvelope", exactly( 1 ), useFirstNonNull() )
		);

		map.put(
				CommonSpatialFunction.ST_SRID.getKey(),
				new Property( "STSrid", exactly( 1 ), invariant( StandardBasicTypes.INTEGER ) )
		);

		map.put(
				CommonSpatialFunction.ST_ASBINARY.getKey(),
				new Method( "STAsBinary", exactly( 1 ), invariant( StandardBasicTypes.BINARY ) )
		);

		map.put(
				CommonSpatialFunction.ST_ISEMPTY.getKey(),
				new Method( "STIsEmpty", exactly( 1 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);

		map.put(
				CommonSpatialFunction.ST_ISSIMPLE.getKey(),
				new Method( "STIsSimple", exactly( 1 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);

		map.put(
				CommonSpatialFunction.ST_BOUNDARY.getKey(),
				new Method( "STBoundary", exactly( 1 ), useFirstNonNull() )
		);
		map.put(
				CommonSpatialFunction.ST_OVERLAPS.getKey(),
				new Method( "STOverlaps", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);
		map.put(
				CommonSpatialFunction.ST_INTERSECTS.getKey(),
				new Method( "STIntersects", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);
		map.put(
				CommonSpatialFunction.ST_CROSSES.getKey(),
				new Method( "STCrosses", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);
		map.put(
				CommonSpatialFunction.ST_CONTAINS.getKey(),
				new Method( "STContains", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);
		map.put(
				CommonSpatialFunction.ST_DISJOINT.getKey(),
				new Method( "STDisjoint", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);

		map.put(
				CommonSpatialFunction.ST_EQUALS.getKey(),
				new Method( "STEquals", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);

		map.put(
				CommonSpatialFunction.ST_TOUCHES.getKey(),
				new Method( "STTouches", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);

		map.put(
				CommonSpatialFunction.ST_WITHIN.getKey(),
				new Method( "STWithin", exactly( 2 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);

		map.put(
				CommonSpatialFunction.ST_DISTANCE.getKey(),
				new Method( "STDistance", exactly( 2 ), invariant( StandardBasicTypes.DOUBLE ) )
		);

		map.put(
				CommonSpatialFunction.ST_BUFFER.getKey(),
				new Method( "STBuffer", exactly( 2 ),
							useFirstNonNull()
				)
		);

		map.put(
				CommonSpatialFunction.ST_CONVEXHULL.getKey(),
				new Method( "STConvexHull", exactly( 1 ), useFirstNonNull() )
		);
		map.put(
				CommonSpatialFunction.ST_DIFFERENCE.getKey(),
				new Method( "STDifference", exactly( 2 ), useFirstNonNull() )
		);
		map.put(
				CommonSpatialFunction.ST_INTERSECTION.getKey(),
				new Method( "STIntersection", exactly( 2 ), useFirstNonNull() )
		);
		map.put(
				CommonSpatialFunction.ST_SYMDIFFERENCE.getKey(),
				new Method( "STSymDifference", exactly( 2 ), useFirstNonNull() )
		);
		map.put( CommonSpatialFunction.ST_UNION.getKey(), new Method( "STUnion", exactly( 2 ), useFirstNonNull() ) );
		map.put(
				CommonSpatialFunction.ST_RELATE.getKey(),
				new Method( "STRelate", exactly( 3 ), invariant( StandardBasicTypes.BOOLEAN ) )
		);

	}


	ArgumentsValidator exactly(int i) {
		return StandardArgumentsValidators.exactly( i );
	}

	FunctionReturnTypeResolver invariant(BasicTypeReference<?> tpe) {
		return StandardFunctionReturnTypeResolvers.invariant( typeRegistry.resolve( tpe ) );
	}

}

class Property extends NamedSqmFunctionDescriptor {
	Property(
			String name, ArgumentsValidator argValidator, FunctionReturnTypeResolver returnTypeResolver) {
		super( name, false, argValidator, returnTypeResolver );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final Expression arg1 = (Expression) sqlAstArguments.get( 0 );
		walker.render( arg1, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( "." );
		sqlAppender.appendSql( getName() );
	}
}

class Method extends NamedSqmFunctionDescriptor {

	Method(
			String name, ArgumentsValidator argValidator, FunctionReturnTypeResolver returnTypeResolver) {
		super( name, true, argValidator, returnTypeResolver );
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {

		final Expression arg1 = (Expression) sqlAstArguments.get( 0 );
		walker.render( arg1, SqlAstNodeRenderingMode.DEFAULT );
		sqlAppender.appendSql( "." );
		sqlAppender.appendSql( getName() );
		sqlAppender.appendSql( '(' );
		for ( int i = 1; i < sqlAstArguments.size(); i++ ) {
			Expression argN = (Expression) sqlAstArguments.get( i );
			walker.render( argN, SqlAstNodeRenderingMode.DEFAULT );
			if ( i < sqlAstArguments.size() - 1 ) {
				sqlAppender.appendSql( " ," );
			}
		}
		sqlAppender.appendSql( ')' );
	}

}

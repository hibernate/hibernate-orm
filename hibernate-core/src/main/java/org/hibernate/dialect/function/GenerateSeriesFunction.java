/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingSetReturningFunctionDescriptor;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.time.Duration;
import java.util.List;

/**
 * Standard generate_series function.
 */
public class GenerateSeriesFunction extends AbstractSqmSelfRenderingSetReturningFunctionDescriptor {

	protected final boolean coerceToTimestamp;

	public GenerateSeriesFunction(@Nullable String defaultValueColumnName, String defaultIndexSelectionExpression, boolean coerceToTimestamp, TypeConfiguration typeConfiguration) {
		this(
				new GenerateSeriesSetReturningFunctionTypeResolver(
						defaultValueColumnName,
						defaultIndexSelectionExpression
				),
				// Treat durations like intervals to avoid conversions
				typeConfiguration.getBasicTypeRegistry().resolve( java.time.Duration.class, SqlTypes.INTERVAL_SECOND ),
				coerceToTimestamp
		);
	}

	protected GenerateSeriesFunction(SetReturningFunctionTypeResolver setReturningFunctionTypeResolver, BasicType<Duration> durationType) {
		this( setReturningFunctionTypeResolver, durationType, false );
	}

	protected GenerateSeriesFunction(SetReturningFunctionTypeResolver setReturningFunctionTypeResolver, BasicType<Duration> durationType, boolean coerceToTimestamp) {
		super(
				"generate_series",
				new GenerateSeriesArgumentValidator(),
				setReturningFunctionTypeResolver,
				new GenerateSeriesArgumentTypeResolver( durationType )
		);
		this.coerceToTimestamp = coerceToTimestamp;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final Expression start = (Expression) sqlAstArguments.get( 0 );
		final Expression stop = (Expression) sqlAstArguments.get( 1 );
		final Expression step = sqlAstArguments.size() > 2 ? (Expression) sqlAstArguments.get( 2 ) : null;
		renderGenerateSeries( sqlAppender, start, stop, step, tupleType, tableIdentifierVariable, walker );
	}

	protected void renderGenerateSeries(
			SqlAppender sqlAppender,
			Expression start,
			Expression stop,
			@Nullable Expression step,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		final JdbcType boundType = start.getExpressionType().getSingleJdbcMapping().getJdbcType();
		final boolean castTimestamp = coerceToTimestamp
				&& (boundType.getDdlTypeCode() == SqlTypes.DATE || boundType.getDdlTypeCode() == SqlTypes.TIME);
		sqlAppender.appendSql( "generate_series(" );
		if ( castTimestamp ) {
			sqlAppender.appendSql( "cast(" );
			start.accept( walker );
			sqlAppender.appendSql( " as timestamp),cast(" );
			stop.accept( walker );
			sqlAppender.appendSql( " as timestamp)" );
		}
		else {
			start.accept( walker );
			sqlAppender.appendSql( ',' );
			stop.accept( walker );
		}
		if ( step != null ) {
			sqlAppender.appendSql( ',' );
			step.accept( walker );
		}
		sqlAppender.appendSql( ')' );
		if ( tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null ) != null ) {
			sqlAppender.append( " with ordinality" );
		}
	}
}

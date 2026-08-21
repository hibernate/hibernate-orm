/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.array;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CteGenerateSeriesFunction;
import org.hibernate.dialect.function.json.DB2JsonTableFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.function.SelfRenderingSqmSetReturningFunction;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;

import java.util.List;

/**
 * DB2 unnest function.
 * Unnesting JSON arrays requires more effort since DB2 doesn't support arrays in {@code json_table()}.
 * See {@link org.hibernate.dialect.function.json.DB2JsonTableFunction} for more details.
 *
 * @see org.hibernate.dialect.function.json.DB2JsonTableFunction
 */
public class DB2UnnestFunction extends UnnestFunction {

	private final int maximumArraySize;

	public DB2UnnestFunction(int maximumArraySize) {
		super( "v", "i" );
		this.maximumArraySize = maximumArraySize;
	}

	@Override
	protected <T> SelfRenderingSqmSetReturningFunction<T> generateSqmSetReturningFunctionExpression(List<? extends SqmTypedNode<?>> arguments, QueryEngine queryEngine) {
		return new SelfRenderingSqmSetReturningFunction<>(
				this,
				this,
				arguments,
				getArgumentsValidator(),
				getSetReturningTypeResolver(),
				queryEngine.getCriteriaBuilder(),
				getName()
		) {
			@Override
			public TableGroup convertToSqlAst(NavigablePath navigablePath, String identifierVariable, boolean lateral, boolean canUseInnerJoins, boolean withOrdinality, SqmToSqlAstConverter walker) {
				walker.registerQueryTransformer( new DB2JsonTableFunction.SeriesQueryTransformer( maximumArraySize ) );
				return super.convertToSqlAst( navigablePath, identifierVariable, lateral, canUseInnerJoins, withOrdinality, walker );
			}
		};
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			Expression array,
			BasicPluralType<?, ?> pluralType,
			@Nullable SqlTypedMapping sqlTypedMapping,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "lateral(select " );
		final ModelPart elementPart = tupleType.findSubPart( CollectionPart.Nature.ELEMENT.getName(), null );
		if ( elementPart == null ) {
			sqlAppender.append( "t.*" );
		}
		else {
			final BasicValuedModelPart elementMapping = elementPart.asBasicValuedModelPart();
			final boolean isBoolean = elementMapping.getSingleJdbcMapping().getJdbcType().isBoolean();
			if ( isBoolean ) {
				sqlAppender.appendSql( "decode(" );
			}
			sqlAppender.appendSql( "json_value('{\"a\":'||" );
			array.accept( walker );
			sqlAppender.appendSql( "||'}','$.a['||(i.i-1)||']'" );
			if ( isBoolean ) {
				sqlAppender.appendSql( ')' );
				final JdbcMapping type = elementMapping.getSingleJdbcMapping();
				//noinspection unchecked
				final JdbcLiteralFormatter<Object> jdbcLiteralFormatter = type.getJdbcLiteralFormatter();
				final SessionFactoryImplementor sessionFactory = walker.getSessionFactory();
				final Dialect dialect = sessionFactory.getJdbcServices().getDialect();
				final WrapperOptions wrapperOptions = sessionFactory.getWrapperOptions();
				final Object trueValue = type.convertToRelationalValue( true );
				final Object falseValue = type.convertToRelationalValue( false );
				sqlAppender.append( ",'true'," );
				jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, trueValue, dialect, wrapperOptions );
				sqlAppender.append( ",'false'," );
				jdbcLiteralFormatter.appendJdbcLiteral( sqlAppender, falseValue, dialect, wrapperOptions );
				sqlAppender.append( ") " );
			}
			else {
				sqlAppender.appendSql( " returning " );
				sqlAppender.append( getDdlType( elementMapping, SqlTypes.JSON_ARRAY, walker ) );
				sqlAppender.append( ") " );
			}

			sqlAppender.append( elementMapping.getSelectionExpression() );
		}
		final ModelPart indexPart = tupleType.findSubPart( CollectionPart.Nature.INDEX.getName(), null );
		if ( indexPart != null ) {
			sqlAppender.appendSql( ",i.i " );
			sqlAppender.append( indexPart.asBasicValuedModelPart().getSelectionExpression() );
		}

		sqlAppender.appendSql( " from " );
		sqlAppender.appendSql( CteGenerateSeriesFunction.CteGenerateSeriesQueryTransformer.NAME );
		sqlAppender.appendSql( " i" );

		if ( elementPart == null ) {
			sqlAppender.appendSql( " join json_table(json_query('{\"a\":'||" );
			array.accept( walker );
			sqlAppender.appendSql( "||'}','$.a['||(i.i-1)||']'),'strict $' columns(" );
			tupleType.forEachSelectable( 0, (selectionIndex, selectableMapping) -> {
				if ( !CollectionPart.Nature.INDEX.getName().equals( selectableMapping.getSelectableName() ) ) {
					if ( selectionIndex == 0 ) {
						sqlAppender.append( ' ' );
					}
					else {
						sqlAppender.append( ',' );
					}
					sqlAppender.append( selectableMapping.getSelectionExpression() );
					sqlAppender.append( ' ' );
					sqlAppender.append( getDdlType( selectableMapping, SqlTypes.JSON_ARRAY, walker ) );
					sqlAppender.appendSql( " path '$." );
					sqlAppender.append( selectableMapping.getSelectableName() );
					sqlAppender.appendSql( "' error on error" );
				}
			} );
			sqlAppender.appendSql( ") error on error) t on json_exists('{\"a\":'||" );
			array.accept( walker );
			sqlAppender.appendSql( "||'}','$.a['||(i.i-1)||']'))" );
		}
		else {
			sqlAppender.appendSql( " where json_exists('{\"a\":'||" );
			array.accept( walker );
			sqlAppender.appendSql( "||'}','$.a['||(i.i-1)||']'))" );
		}

	}
}

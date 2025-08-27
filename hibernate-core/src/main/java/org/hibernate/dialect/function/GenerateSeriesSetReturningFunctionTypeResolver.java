/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.internal.util.NullnessHelper;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 *
 * @since 7.0
 */
public class GenerateSeriesSetReturningFunctionTypeResolver implements SetReturningFunctionTypeResolver {

	protected final @Nullable String defaultValueColumnName;
	protected final String defaultIndexSelectionExpression;

	public GenerateSeriesSetReturningFunctionTypeResolver(@Nullable String defaultValueColumnName, String defaultIndexSelectionExpression) {
		this.defaultValueColumnName = defaultValueColumnName;
		this.defaultIndexSelectionExpression = defaultIndexSelectionExpression;
	}

	@Override
	public AnonymousTupleType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
		final SqmTypedNode<?> start = arguments.get( 0 );
		final SqmTypedNode<?> stop = arguments.get( 1 );
		final SqmExpressible<?> startExpressible = start.getExpressible();
		final SqmExpressible<?> stopExpressible = stop.getExpressible();
		final SqmDomainType<?> type = NullnessHelper.coalesce(
				startExpressible == null ? null : startExpressible.getSqmType(),
				stopExpressible == null ? null : stopExpressible.getSqmType()
		);
		if ( type == null ) {
			throw new IllegalArgumentException( "Couldn't determine types of arguments to function 'generate_series'" );
		}

		final SqmBindableType<?>[] componentTypes = new SqmBindableType<?>[]{ type, typeConfiguration.getBasicTypeForJavaType( Long.class ) };
		final String[] componentNames = new String[]{ CollectionPart.Nature.ELEMENT.getName(), CollectionPart.Nature.INDEX.getName() };
		return new AnonymousTupleType<>( componentTypes, componentNames );
	}

	@Override
	public SelectableMapping[] resolveFunctionReturnType(
			List<? extends SqlAstNode> arguments,
			String tableIdentifierVariable,
			boolean lateral,
			boolean withOrdinality,
			SqmToSqlAstConverter converter) {
		final Expression start = (Expression) arguments.get( 0 );
		final Expression stop = (Expression) arguments.get( 0 );
		final JdbcMappingContainer expressionType = NullnessHelper.coalesce(
				start.getExpressionType(),
				stop.getExpressionType()
		);
		final JdbcMapping type = expressionType.getSingleJdbcMapping();
		if ( type == null ) {
			throw new IllegalArgumentException( "Couldn't determine types of arguments to function 'generate_series'" );
		}

		final SelectableMapping indexMapping = withOrdinality ? new SelectableMappingImpl(
				"",
				defaultIndexSelectionExpression,
				new SelectablePath( CollectionPart.Nature.INDEX.getName() ),
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				false,
				false,
				false,
				false,
				false,
				false,
				converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Long.class )
		) : null;

		final String elementSelectionExpression = defaultValueColumnName == null
				? tableIdentifierVariable
				: defaultValueColumnName;
		final SelectableMapping elementMapping;
		if ( expressionType instanceof SqlTypedMapping typedMapping ) {
			elementMapping = new SelectableMappingImpl(
					"",
					elementSelectionExpression,
					new SelectablePath( CollectionPart.Nature.ELEMENT.getName() ),
					null,
					null,
					typedMapping.getColumnDefinition(),
					typedMapping.getLength(),
					typedMapping.getPrecision(),
					typedMapping.getScale(),
					typedMapping.getTemporalPrecision(),
					typedMapping.isLob(),
					true,
					false,
					false,
					false,
					false,
					type
			);
		}
		else {
			elementMapping = new SelectableMappingImpl(
					"",
					elementSelectionExpression,
					new SelectablePath( CollectionPart.Nature.ELEMENT.getName() ),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					false,
					true,
					false,
					false,
					false,
					false,
					type
			);
		}
		final SelectableMapping[] returnType;
		if ( indexMapping == null ) {
			returnType = new SelectableMapping[]{ elementMapping };
		}
		else {
			returnType = new SelectableMapping[] {elementMapping, indexMapping};
		}
		return returnType;
	}
}

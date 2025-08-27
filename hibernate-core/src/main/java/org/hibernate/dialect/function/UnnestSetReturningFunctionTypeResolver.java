/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function;

import java.util.ArrayList;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 *
 * @since 7.0
 */
public class UnnestSetReturningFunctionTypeResolver implements SetReturningFunctionTypeResolver {

	protected final @Nullable String defaultBasicArrayColumnName;
	protected final String defaultIndexSelectionExpression;

	public UnnestSetReturningFunctionTypeResolver(@Nullable String defaultBasicArrayColumnName, String defaultIndexSelectionExpression) {
		this.defaultBasicArrayColumnName = defaultBasicArrayColumnName;
		this.defaultIndexSelectionExpression = defaultIndexSelectionExpression;
	}

	@Override
	public AnonymousTupleType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
		final SqmTypedNode<?> arrayArgument = arguments.get( 0 );
		final SqmExpressible<?> expressible = arrayArgument.getExpressible();
		if ( expressible == null ) {
			throw new IllegalArgumentException( "Couldn't determine array type of argument to function 'unnest'" );
		}
		if ( !( expressible.getSqmType() instanceof BasicPluralType<?,?> pluralType ) ) {
			throw new IllegalArgumentException( "Argument passed to function 'unnest' is not a BasicPluralType. Found: " + expressible );
		}

		final BasicType<?> elementType = pluralType.getElementType();
		final SqmBindableType<?>[] componentTypes;
		final String[] componentNames;
		if ( elementType.getJdbcType() instanceof AggregateJdbcType aggregateJdbcType
			&& aggregateJdbcType.getEmbeddableMappingType() != null ) {
			final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
			componentTypes = determineComponentTypes( embeddableMappingType );
			componentNames = new String[componentTypes.length];
			final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
			int index = 0;
			for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
				final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( i );
				if ( attributeMapping.getMappedType() instanceof SqmExpressible<?> ) {
					componentNames[index++] = attributeMapping.getAttributeName();
				}
			}
			assert index == componentNames.length - 1;
			componentTypes[index] = typeConfiguration.getBasicTypeForJavaType( Long.class );
			componentNames[index] = CollectionPart.Nature.INDEX.getName();
		}
		else {
			componentTypes = new SqmBindableType<?>[]{ elementType, typeConfiguration.getBasicTypeForJavaType( Long.class ) };
			componentNames = new String[]{ CollectionPart.Nature.ELEMENT.getName(), CollectionPart.Nature.INDEX.getName() };
		}
		return new AnonymousTupleType<>( componentTypes, componentNames );
	}

	@Override
	public SelectableMapping[] resolveFunctionReturnType(
			List<? extends SqlAstNode> arguments,
			String tableIdentifierVariable,
			boolean lateral,
			boolean withOrdinality,
			SqmToSqlAstConverter converter) {
		final Expression expression = (Expression) arguments.get( 0 );
		final JdbcMappingContainer expressionType = expression.getExpressionType();
		if ( expressionType == null ) {
			throw new IllegalArgumentException( "Couldn't determine array type of argument to function 'unnest'" );
		}
		if ( !( expressionType.getSingleJdbcMapping() instanceof BasicPluralType<?,?> pluralType ) ) {
			throw new IllegalArgumentException( "Argument passed to function 'unnest' is not a BasicPluralType. Found: " + expressionType );
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

		final BasicType<?> elementType = pluralType.getElementType();
		final SelectableMapping[] returnType;
		if ( elementType.getJdbcType() instanceof AggregateJdbcType aggregateJdbcType
				&& aggregateJdbcType.getEmbeddableMappingType() != null ) {
			final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
			final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
			returnType = new SelectableMapping[jdbcValueCount + (indexMapping == null ? 0 : 1)];
			for ( int i = 0; i < jdbcValueCount; i++ ) {
				final SelectableMapping selectableMapping = embeddableMappingType.getJdbcValueSelectable( i );
				final String selectableName = selectableMapping.getSelectableName();
				returnType[i] = new SelectableMappingImpl(
						selectableMapping.getContainingTableExpression(),
						selectableName,
						new SelectablePath( selectableName ),
						null,
						null,
						selectableMapping.getColumnDefinition(),
						selectableMapping.getLength(),
						selectableMapping.getPrecision(),
						selectableMapping.getScale(),
						selectableMapping.getTemporalPrecision(),
						selectableMapping.isLob(),
						true,
						false,
						false,
						false,
						selectableMapping.isFormula(),
						selectableMapping.getJdbcMapping()
				);
				if ( indexMapping != null ) {
					returnType[jdbcValueCount] = indexMapping;
				}
			}
		}
		else {
			final String elementSelectionExpression = defaultBasicArrayColumnName == null
					? tableIdentifierVariable
					: defaultBasicArrayColumnName;
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
						elementType
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
						elementType
				);
			}
			if ( indexMapping == null ) {
				returnType = new SelectableMapping[]{ elementMapping };
			}
			else {
				returnType = new SelectableMapping[] {elementMapping, indexMapping};
			}
		}
		return returnType;
	}

	private static SqmBindableType<?>[] determineComponentTypes(EmbeddableMappingType embeddableMappingType) {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final ArrayList<SqmBindableType<?>> expressibles = new ArrayList<>( numberOfAttributeMappings + 1 );

		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( i );
			final MappingType mappedType = attributeMapping.getMappedType();
			if ( mappedType instanceof SqmBindableType<?> sqmExpressible ) {
				expressibles.add( sqmExpressible );
			}
		}
		return expressibles.toArray( new SqmBindableType<?>[expressibles.size() + 1] );
	}
}

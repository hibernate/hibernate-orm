/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function.internal;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeReference;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @since 7.0
 */
@Incubating
public class SetReturningFunctionTypeResolverBuilder implements SetReturningFunctionTypeResolver.Builder {

	private final LinkedHashMap<String, TypeResolver> typeResolvers = new LinkedHashMap<>();

	@Override
	public SetReturningFunctionTypeResolver.Builder invariant(String component, BasicTypeReference<?> invariantType) {
		return invariant( component, invariantType, component );
	}

	@Override
	public SetReturningFunctionTypeResolverBuilder invariant(String component, BasicType<?> invariantType) {
		return invariant( component, invariantType, component );
	}

	@Override
	public SetReturningFunctionTypeResolverBuilder useArgType(String component, int argPosition) {
		return useArgType( component, argPosition, component );
	}

	@Override
	public SetReturningFunctionTypeResolver.Builder invariant(String component, BasicTypeReference<?> invariantType, String selectionExpression) {
		if ( invariantType == null ) {
			throw new IllegalArgumentException( "Passed `invariantType` for function return cannot be null" );
		}
		if ( selectionExpression == null ) {
			throw new IllegalArgumentException( "Passed `selectionExpression` for function return cannot be null" );
		}
		return withComponent( component, new BasicTypeReferenceTypeResolver( component, selectionExpression, invariantType ) );
	}

	@Override
	public SetReturningFunctionTypeResolverBuilder invariant(String component, BasicType<?> invariantType, String selectionExpression) {
		if ( invariantType == null ) {
			throw new IllegalArgumentException( "Passed `invariantType` for function return cannot be null" );
		}
		if ( selectionExpression == null ) {
			throw new IllegalArgumentException( "Passed `selectionExpression` for function return cannot be null" );
		}
		return withComponent( component, new BasicTypeTypeResolver( component, selectionExpression, invariantType ) );
	}

	@Override
	public SetReturningFunctionTypeResolverBuilder useArgType(String component, int argPosition, String selectionExpression) {
		if ( selectionExpression == null ) {
			throw new IllegalArgumentException( "Passed `selectionExpression` for function return cannot be null" );
		}
		return withComponent( component, new ArgTypeTypeResolver( component, selectionExpression, argPosition ) );
	}

	private SetReturningFunctionTypeResolverBuilder withComponent(String component, TypeResolver resolver) {
		if ( component == null ) {
			throw new IllegalArgumentException( "Passed `component` for function return cannot be null" );
		}
		typeResolvers.put( component, resolver );
		return this;
	}

	@Override
	public SetReturningFunctionTypeResolver build() {
		return new SetReturningFunctionTypeResolverImpl( this );
	}

	private static class SetReturningFunctionTypeResolverImpl implements SetReturningFunctionTypeResolver {

		private final TypeResolver[] typeResolvers;

		public SetReturningFunctionTypeResolverImpl(SetReturningFunctionTypeResolverBuilder builder) {
			this.typeResolvers = builder.typeResolvers.values().toArray( new TypeResolver[0] );
		}

		@Override
		public AnonymousTupleType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
			final SqmBindableType<?>[] componentTypes = new SqmBindableType[typeResolvers.length + 1];
			final String[] componentNames = new String[typeResolvers.length + 1];
			int i = 0;
			for ( TypeResolver typeResolver : typeResolvers ) {
				componentNames[i] = typeResolver.componentName();
				componentTypes[i] = typeResolver.resolveTupleType( arguments, typeConfiguration );
				i++;
			}
			componentTypes[i] = typeConfiguration.getBasicTypeForJavaType( Long.class );
			componentNames[i] = CollectionPart.Nature.INDEX.getName();
			return new AnonymousTupleType<>( componentTypes, componentNames );
		}

		@Override
		public SelectableMapping[] resolveFunctionReturnType(
				List<? extends SqlAstNode> arguments,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			final SelectableMapping[] selectableMappings = new SelectableMapping[typeResolvers.length + (withOrdinality ? 1 : 0)];
			int i = 0;
			for ( TypeResolver typeResolver : typeResolvers ) {
				final JdbcMapping jdbcMapping = typeResolver.resolveFunctionReturnType( arguments, converter );
				selectableMappings[i] = new SelectableMappingImpl(
						"",
						typeResolver.selectionExpression(),
						new SelectablePath( typeResolver.componentName() ),
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
						jdbcMapping
				);
				i++;
			}
			if ( withOrdinality ) {
				selectableMappings[i] = new SelectableMappingImpl(
						"",
						determineIndexSelectionExpression( selectableMappings, tableIdentifierVariable, converter ),
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
				);
			}
			return selectableMappings;
		}

		private String determineIndexSelectionExpression(SelectableMapping[] selectableMappings, String tableIdentifierVariable, SqmToSqlAstConverter walker) {
			final String defaultOrdinalityColumnName =
					walker.getCreationContext().getDialect()
							.getDefaultOrdinalityColumnName();
			String name = defaultOrdinalityColumnName == null ? "i" : defaultOrdinalityColumnName;
			OUTER: for ( int i = 0; i < selectableMappings.length; i++ ) {
				for ( SelectableMapping selectableMapping : selectableMappings ) {
					if ( selectableMapping != null ) {
						if ( selectableMapping.getSelectionExpression().equals( name ) ) {
							name += '_';
							continue OUTER;
						}
					}
				}
				break;
			}
			return name;
		}
	}

	private interface TypeResolver {

		String componentName();

		String selectionExpression();

		SqmBindableType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration);

		JdbcMapping resolveFunctionReturnType(List<? extends SqlAstNode> arguments, SqmToSqlAstConverter walker);
	}

	private record BasicTypeReferenceTypeResolver(
			String componentName,
			String selectionExpression,
			BasicTypeReference<?> basicTypeReference
	) implements TypeResolver {

		@Override
			public SqmBindableType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
				return typeConfiguration.getBasicTypeRegistry().resolve( basicTypeReference );
			}

			@Override
			public JdbcMapping resolveFunctionReturnType(List<? extends SqlAstNode> arguments, SqmToSqlAstConverter walker) {
				return walker.getCreationContext().getTypeConfiguration().getBasicTypeRegistry().resolve( basicTypeReference );
			}
		}

	private record BasicTypeTypeResolver(
			String componentName,
			String selectionExpression,
			BasicType<?> basicType
	) implements TypeResolver {

		@Override
			public SqmBindableType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
				return basicType;
			}

			@Override
			public JdbcMapping resolveFunctionReturnType(List<? extends SqlAstNode> arguments, SqmToSqlAstConverter walker) {
				return basicType;
			}
		}

	private record ArgTypeTypeResolver(
			String componentName,
			String selectionExpression,
			int argPosition
	) implements TypeResolver {

		@Override
			public SqmBindableType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
				return arguments.get( argPosition ).getExpressible();
			}

			@Override
			public JdbcMapping resolveFunctionReturnType(List<? extends SqlAstNode> arguments, SqmToSqlAstConverter walker) {
				return ((Expression) arguments.get( argPosition )).getExpressionType().getSingleJdbcMapping();
			}
		}
}

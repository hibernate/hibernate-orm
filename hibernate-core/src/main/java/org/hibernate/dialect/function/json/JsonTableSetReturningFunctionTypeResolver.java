/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmJsonTableFunction;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.JsonTableExistsColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableNestedColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.JsonTableValueColumnDefinition;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @since 7.0
 */
public class JsonTableSetReturningFunctionTypeResolver implements SetReturningFunctionTypeResolver {

	@Override
	public AnonymousTupleType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
		final SqmJsonTableFunction.Columns columns = (SqmJsonTableFunction.Columns) arguments.get( arguments.size() - 1 );
		return columns.createTupleType();
	}

	@Override
	public SelectableMapping[] resolveFunctionReturnType(
			List<? extends SqlAstNode> arguments,
			String tableIdentifierVariable,
			boolean lateral,
			boolean withOrdinality,
			SqmToSqlAstConverter converter) {
		JsonTableColumnsClause columnsClause = null;
		for ( SqlAstNode argument : arguments ) {
			if ( argument instanceof JsonTableColumnsClause jsonTableColumnsClause ) {
				columnsClause = jsonTableColumnsClause;
				break;
			}
		}
		assert columnsClause != null;

		final List<JsonTableColumnDefinition> columnDefinitions = columnsClause.getColumnDefinitions();
		final List<SelectableMapping> selectableMappings = new ArrayList<>( columnDefinitions.size() );
		addSelectableMappings( selectableMappings, columnsClause, converter );
		return selectableMappings.toArray( new SelectableMapping[0] );
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableNestedColumnDefinition columnDefinition, SqmToSqlAstConverter converter) {
		addSelectableMappings( selectableMappings, columnDefinition.columns(), converter );
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableColumnsClause columnsClause, SqmToSqlAstConverter converter) {
		for ( JsonTableColumnDefinition columnDefinition : columnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof JsonTableExistsColumnDefinition definition ) {
				addSelectableMappings( selectableMappings, definition, converter );
			}
			else if ( columnDefinition instanceof JsonTableQueryColumnDefinition definition ) {
				addSelectableMappings( selectableMappings, definition, converter );
			}
			else if ( columnDefinition instanceof JsonTableValueColumnDefinition definition ) {
				addSelectableMappings( selectableMappings, definition, converter );
			}
			else if ( columnDefinition instanceof JsonTableOrdinalityColumnDefinition definition ) {
				addSelectableMappings( selectableMappings, definition, converter );
			}
			else {
				addSelectableMappings( selectableMappings, (JsonTableNestedColumnDefinition) columnDefinition, converter );
			}
		}
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableOrdinalityColumnDefinition definition, SqmToSqlAstConverter converter) {
		addSelectableMapping(
				selectableMappings,
				definition.name(),
				converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Long.class ),
				converter
		);
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableValueColumnDefinition definition, SqmToSqlAstConverter converter) {
		addSelectableMapping(
				selectableMappings,
				definition.name(),
				definition.type().getJdbcMapping(),
				converter
		);
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableQueryColumnDefinition definition, SqmToSqlAstConverter converter) {
		addSelectableMapping(
				selectableMappings,
				definition.name(),
				converter.getCreationContext().getTypeConfiguration().getBasicTypeRegistry()
						.resolve( String.class, SqlTypes.JSON ),
				converter
		);
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableExistsColumnDefinition definition, SqmToSqlAstConverter converter) {
		addSelectableMapping(
				selectableMappings,
				definition.name(),
				converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Boolean.class ),
				converter
		);
	}

	protected void addSelectableMapping(List<SelectableMapping> selectableMappings, String name, JdbcMapping type, SqmToSqlAstConverter converter) {
		selectableMappings.add( new SelectableMappingImpl(
				"",
				name,
				new SelectablePath( name ),
				null,
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
				type
		));
	}
}

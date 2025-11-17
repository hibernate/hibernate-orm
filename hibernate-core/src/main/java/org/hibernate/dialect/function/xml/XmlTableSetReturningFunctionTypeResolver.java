/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleType;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.query.sqm.tree.expression.SqmXmlTableFunction;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnsClause;
import org.hibernate.sql.ast.tree.expression.XmlTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableValueColumnDefinition;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @since 7.0
 */
public class XmlTableSetReturningFunctionTypeResolver implements SetReturningFunctionTypeResolver {

	@Override
	public AnonymousTupleType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments, TypeConfiguration typeConfiguration) {
		final SqmXmlTableFunction.Columns columns = (SqmXmlTableFunction.Columns) arguments.get( arguments.size() - 1 );
		return columns.createTupleType();
	}

	@Override
	public SelectableMapping[] resolveFunctionReturnType(
			List<? extends SqlAstNode> arguments,
			String tableIdentifierVariable,
			boolean lateral,
			boolean withOrdinality,
			SqmToSqlAstConverter converter) {
		XmlTableColumnsClause columnsClause = null;
		for ( SqlAstNode argument : arguments ) {
			if ( argument instanceof XmlTableColumnsClause tableColumnsClause ) {
				columnsClause = tableColumnsClause;
				break;
			}
		}
		assert columnsClause != null;

		final List<XmlTableColumnDefinition> columnDefinitions = columnsClause.getColumnDefinitions();
		final List<SelectableMapping> selectableMappings = new ArrayList<>( columnDefinitions.size() );
		addSelectableMappings( selectableMappings, columnsClause, converter );
		return selectableMappings.toArray( new SelectableMapping[0] );
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, XmlTableColumnsClause columnsClause, SqmToSqlAstConverter converter) {
		for ( XmlTableColumnDefinition columnDefinition : columnsClause.getColumnDefinitions() ) {
			if ( columnDefinition instanceof XmlTableQueryColumnDefinition definition ) {
				addSelectableMappings( selectableMappings, definition, converter );
			}
			else if ( columnDefinition instanceof XmlTableValueColumnDefinition definition ) {
				addSelectableMappings( selectableMappings, definition, converter );
			}
			else {
				final XmlTableOrdinalityColumnDefinition definition
						= (XmlTableOrdinalityColumnDefinition) columnDefinition;
				addSelectableMappings( selectableMappings, definition, converter );
			}
		}
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, XmlTableOrdinalityColumnDefinition definition, SqmToSqlAstConverter converter) {
		addSelectableMapping(
				selectableMappings,
				definition.name(),
				converter.getCreationContext().getTypeConfiguration().getBasicTypeForJavaType( Long.class ),
				converter );
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, XmlTableValueColumnDefinition definition, SqmToSqlAstConverter converter) {
		addSelectableMapping(
				selectableMappings,
				definition.name(),
				definition.type().getJdbcMapping(),
				converter );
	}

	protected void addSelectableMappings(List<SelectableMapping> selectableMappings, XmlTableQueryColumnDefinition definition, SqmToSqlAstConverter converter) {
		addSelectableMapping(
				selectableMappings,
				definition.name(),
				converter.getCreationContext().getTypeConfiguration().getBasicTypeRegistry()
						.resolve( String.class, SqlTypes.SQLXML ),
				converter );
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

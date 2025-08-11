/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.json;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.sql.ast.tree.expression.JsonTableErrorBehavior;
import org.hibernate.sql.ast.tree.expression.JsonTableQueryColumnDefinition;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonAsStringJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

import static org.hibernate.dialect.function.json.OracleJsonValueFunction.isEncodedBoolean;

/**
 * Oracle json_table function.
 */
public class OracleJsonTableFunction extends JsonTableFunction {

	public OracleJsonTableFunction(TypeConfiguration typeConfiguration) {
		super( new OracleJsonTableSetReturningFunctionTypeResolver(), typeConfiguration );
	}

	@Override
	protected void renderJsonTable(
			SqlAppender sqlAppender,
			JsonTableArguments arguments,
			AnonymousTupleTableGroupProducer tupleType,
			String tableIdentifierVariable,
			SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "json_table(" );
		arguments.jsonDocument().accept( walker );
		if ( arguments.jsonPath() != null ) {
			sqlAppender.appendSql( ',' );
			final JsonPathPassingClause passingClause = arguments.passingClause();
			if ( passingClause != null ) {
				JsonPathHelper.appendInlinedJsonPathIncludingPassingClause(
						sqlAppender,
						"",
						arguments.jsonPath(),
						passingClause,
						walker
				);
			}
			else {
				arguments.jsonPath().accept( walker );
			}
		}
		// Default behavior is NULL ON ERROR
		if ( arguments.errorBehavior() == JsonTableErrorBehavior.ERROR ) {
			sqlAppender.appendSql( " error on error" );
		}
		renderColumns( sqlAppender, arguments.columnsClause(), 0, walker );
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		final String typeName = super.determineColumnType( castTarget, walker );
		// The various float types are not supported in json_table() on all versions of Oracle,
		// but luckily, one can use "number" to "parse" arbitrary precision numbers
		return switch ( typeName ) {
			case "float", "binary_float", "binary_double" -> "number";
			case "number(1,0)" -> isEncodedBoolean( castTarget.getJdbcMapping() ) ? "varchar2(5)" : typeName;
			// Prefer clob over blob for JSON types for backwards compatibility
			case "blob" -> isJson( castTarget.getJdbcMapping() ) ? "clob" : typeName;
			default -> typeName;
		};
	}

	private boolean isJson(JdbcMapping jdbcMapping) {
		return jdbcMapping.getJdbcType().isJson();
	}

	private static class OracleJsonTableSetReturningFunctionTypeResolver extends JsonTableSetReturningFunctionTypeResolver {
		@Override
		protected void addSelectableMappings(List<SelectableMapping> selectableMappings, JsonTableQueryColumnDefinition definition, SqmToSqlAstConverter converter) {
			//
			final TypeConfiguration typeConfiguration = converter.getCreationContext().getTypeConfiguration();
			final JdbcType jsonType = typeConfiguration.getJdbcTypeRegistry().getDescriptor( SqlTypes.JSON );
			if ( jsonType.getDdlTypeCode() == SqlTypes.BLOB ) {
				// Blob is not supported on all DB versions as return type for json_table(), so we have to use clob
				addSelectableMapping(
						selectableMappings,
						definition.name(),
						typeConfiguration.getBasicTypeRegistry().resolve(
								typeConfiguration.getJavaTypeRegistry().getDescriptor( String.class ),
								JsonAsStringJdbcType.CLOB_INSTANCE
						),
						converter
				);
			}
			else {
				super.addSelectableMappings( selectableMappings, definition, converter );
			}
		}

		@Override
		protected void addSelectableMapping(List<SelectableMapping> selectableMappings, String name, JdbcMapping type, SqmToSqlAstConverter converter) {
			if ( isEncodedBoolean( type ) ) {
				//noinspection unchecked
				final JdbcLiteralFormatter<Object> jdbcLiteralFormatter = type.getJdbcLiteralFormatter();
				final Dialect dialect = converter.getCreationContext().getDialect();
				final WrapperOptions wrapperOptions = converter.getCreationContext().getWrapperOptions();
				final Object trueValue = type.convertToRelationalValue( true );
				final Object falseValue = type.convertToRelationalValue( false );
				final String trueFragment = jdbcLiteralFormatter.toJdbcLiteral( trueValue, dialect, wrapperOptions );
				final String falseFragment = jdbcLiteralFormatter.toJdbcLiteral( falseValue, dialect, wrapperOptions );
				selectableMappings.add( new SelectableMappingImpl(
						"",
						name,
						new SelectablePath( name ),
						"decode(" + Template.TEMPLATE + "." + name + ",'true'," + trueFragment + ",'false'," + falseFragment + ")",
						null,
						"varchar2(5)",
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
			else {
				super.addSelectableMapping( selectableMappings, name, type, converter );
			}
		}
	}
}

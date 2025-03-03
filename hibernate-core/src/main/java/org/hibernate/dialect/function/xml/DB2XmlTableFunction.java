/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.internal.SelectableMappingImpl;
import org.hibernate.query.sqm.tuple.internal.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.Template;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.XmlTableValueColumnDefinition;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.List;

/**
 * DB2 xmltable function.
 */
public class DB2XmlTableFunction extends XmlTableFunction {

	public DB2XmlTableFunction(TypeConfiguration typeConfiguration) {
		super( false, new DB2XmlTableSetReturningFunctionTypeResolver(), typeConfiguration );
	}

	@Override
	protected void renderXmlTable(SqlAppender sqlAppender, XmlTableArguments arguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmltable(" );
		// DB2 doesn't like parameters for the xpath expression
		final String xpath = walker.getLiteralValue( arguments.xpath() );
		sqlAppender.appendSingleQuoteEscapedString( "$d" + xpath );
		sqlAppender.appendSql( " passing " );
		if ( !arguments.isXmlType() ) {
			sqlAppender.appendSql( "xmlparse(document " );
		}
		// DB2 needs parameters to be casted here
		walker.render( arguments.xmlDocument(), SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		if ( !arguments.isXmlType() ) {
			sqlAppender.appendSql( ')' );
		}
		sqlAppender.appendSql( " as \"d\"" );
		renderColumns( sqlAppender, arguments.columnsClause(), walker );
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		final String typeName = super.determineColumnType( castTarget, walker );
		return isBoolean( castTarget.getJdbcMapping() ) ? "varchar(5)" : typeName;
	}

	@Override
	protected void renderXmlValueColumnDefinition(SqlAppender sqlAppender, XmlTableValueColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( definition.type(), walker ) );

		// DB2 wants the default before the path
		renderDefaultExpression( definition.defaultExpression(), sqlAppender, walker );
		renderColumnPath( definition.name(), definition.xpath(), sqlAppender, walker );
	}

	static boolean isBoolean(JdbcMapping type) {
		return switch ( type.getCastType() ) {
			case BOOLEAN, TF_BOOLEAN, YN_BOOLEAN, INTEGER_BOOLEAN -> true;
			default -> false;
		};
	}

	private static class DB2XmlTableSetReturningFunctionTypeResolver extends XmlTableSetReturningFunctionTypeResolver {
		@Override
		protected void addSelectableMapping(List<SelectableMapping> selectableMappings, String name, JdbcMapping type, SqmToSqlAstConverter converter) {
			if ( isBoolean( type ) ) {
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
						"varchar(5)",
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

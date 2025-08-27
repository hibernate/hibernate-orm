/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.function.xml;

import org.hibernate.QueryException;
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
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.CastTarget;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.sql.ast.tree.expression.XmlTableColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableOrdinalityColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableQueryColumnDefinition;
import org.hibernate.sql.ast.tree.expression.XmlTableValueColumnDefinition;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.ArrayList;
import java.util.List;


/**
 * Sybase ASE xmltable function.
 */
public class SybaseASEXmlTableFunction extends XmlTableFunction {

	public SybaseASEXmlTableFunction(TypeConfiguration typeConfiguration) {
		super( false, new SybaseASEXmlTableSetReturningFunctionTypeResolver(), typeConfiguration );
	}

	@Override
	protected void renderXmlTable(SqlAppender sqlAppender, XmlTableArguments arguments, AnonymousTupleTableGroupProducer tupleType, String tableIdentifierVariable, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( "xmltable(" );
		walker.render( arguments.xpath(), SqlAstNodeRenderingMode.INLINE_PARAMETERS );
		sqlAppender.appendSql( " passing " );
		walker.render( arguments.xmlDocument(), SqlAstNodeRenderingMode.INLINE_PARAMETERS );
		renderColumns( sqlAppender, arguments.columnsClause(), walker );
		sqlAppender.appendSql( ')' );
	}

	@Override
	protected String determineColumnType(CastTarget castTarget, SqlAstTranslator<?> walker) {
		if ( isBoolean( castTarget.getJdbcMapping() ) ) {
			return "varchar(5)";
		}
		else {
			return super.determineColumnType( castTarget, walker );
		}
	}

	@Override
	protected void renderXmlQueryColumnDefinition(SqlAppender sqlAppender, XmlTableQueryColumnDefinition definition, SqlAstTranslator<?> walker) {
		// Queries don't really work, so we have to extract the ordinality instead and extract the value through a read expression
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " int for ordinality" );
	}

	@Override
	protected void renderXmlValueColumnDefinition(SqlAppender sqlAppender, XmlTableValueColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( ' ' );
		sqlAppender.appendSql( determineColumnType( definition.type(), walker ) );

		// Sybase ASE wants the default before the path
		renderDefaultExpression( definition.defaultExpression(), sqlAppender, walker );
		renderColumnPath( definition.name(), definition.xpath(), sqlAppender, walker );
	}

	@Override
	protected void renderXmlOrdinalityColumnDefinition(SqlAppender sqlAppender, XmlTableOrdinalityColumnDefinition definition, SqlAstTranslator<?> walker) {
		sqlAppender.appendSql( definition.name() );
		sqlAppender.appendSql( " bigint for ordinality" );
	}

	private static class SybaseASEXmlTableSetReturningFunctionTypeResolver extends XmlTableSetReturningFunctionTypeResolver {

		@Override
		public SelectableMapping[] resolveFunctionReturnType(
				List<? extends SqlAstNode> sqlAstNodes,
				String tableIdentifierVariable,
				boolean lateral,
				boolean withOrdinality,
				SqmToSqlAstConverter converter) {
			final XmlTableArguments arguments = XmlTableArguments.extract( sqlAstNodes );
			final List<SelectableMapping> selectableMappings = new ArrayList<>( arguments.columnsClause().getColumnDefinitions().size() );
			addSelectableMappings( selectableMappings, arguments, converter );
			return selectableMappings.toArray( new SelectableMapping[0] );
		}

		protected void addSelectableMappings(List<SelectableMapping> selectableMappings, XmlTableArguments arguments, SqmToSqlAstConverter converter) {
			for ( XmlTableColumnDefinition columnDefinition : arguments.columnsClause().getColumnDefinitions() ) {
				if ( columnDefinition instanceof XmlTableQueryColumnDefinition definition ) {
					addSelectableMappings( selectableMappings, definition, arguments, converter );
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

		protected void addSelectableMappings(List<SelectableMapping> selectableMappings, XmlTableQueryColumnDefinition definition, XmlTableArguments arguments, SqmToSqlAstConverter converter) {
			// Sybase ASE can't extract XML nodes via xmltable, so we select the ordinality instead and extract
			// the XML nodes via xmlextract in select item. Unfortunately, this limits XPaths to literals
			// and documents to columns or literals, since that is the only form that can be encoded in read expressions
			final String documentFragment;
			if ( arguments.xmlDocument() instanceof Literal documentLiteral ) {
				documentFragment = documentLiteral.getJdbcMapping().getJdbcLiteralFormatter().toJdbcLiteral(
						documentLiteral.getLiteralValue(),
						converter.getCreationContext().getDialect(),
						converter.getCreationContext().getWrapperOptions()
				);
			}
			else if ( arguments.xmlDocument() instanceof ColumnReference columnReference ) {
				documentFragment = columnReference.getExpressionText();
			}
			else {
				throw new QueryException( "Sybase ASE only supports passing a literal or column reference as XML document for xmltable() when using query columns, but got: " + arguments.xmlDocument() );
			}
			if ( !( arguments.xpath() instanceof Literal literal)) {
				throw new QueryException( "Sybase ASE only supports passing an XPath literal to xmltable() when using query columns, but got: " + arguments.xpath() );
			}
			final String xpathString = (String) literal.getLiteralValue();
			final String definitionPath = definition.xpath() == null ? definition.name() : definition.xpath();

			selectableMappings.add( new SelectableMappingImpl(
					"",
					definition.name(),
					new SelectablePath( definition.name() ),
					"xmlextract('" + xpathString + "['||cast(" + Template.TEMPLATE + "." + definition.name() + " as varchar)||']/" + definitionPath + "'," + documentFragment + ")",
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
					converter.getCreationContext().getTypeConfiguration().getBasicTypeRegistry()
							.resolve( String.class, SqlTypes.SQLXML )
			));
		}

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
						"case " + Template.TEMPLATE + "." + name + " when 'true' then " + trueFragment + " when 'false' then " + falseFragment + " end",
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

	public static boolean isBoolean(JdbcMapping type) {
		return switch ( type.getCastType() ) {
			case BOOLEAN, TF_BOOLEAN, YN_BOOLEAN, INTEGER_BOOLEAN -> true;
			default -> false;
		};
	}
}

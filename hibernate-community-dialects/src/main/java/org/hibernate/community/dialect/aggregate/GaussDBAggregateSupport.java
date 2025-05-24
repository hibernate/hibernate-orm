/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.aggregate;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.AggregateSupportImpl;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.STRUCT_ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT_TABLE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

/**
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLAggregateSupport.
 */
public class GaussDBAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport INSTANCE = new GaussDBAggregateSupport();

	private static final String XML_EXTRACT_START = "xmlelement(name \"" + XmlHelper.ROOT_TAG + "\",(select xmlagg(t.v) from xmltable(";
	private static final String XML_EXTRACT_SEPARATOR = "/*' passing ";
	private static final String XML_EXTRACT_END = " columns v xml path '.')t))";
	private static final String XML_QUERY_START = "(select xmlagg(t.v) from xmltable(";
	private static final String XML_QUERY_SEPARATOR = "' passing ";
	private static final String XML_QUERY_END = " columns v xml path '.')t)";

	public static AggregateSupport valueOf(Dialect dialect) {
		return GaussDBAggregateSupport.INSTANCE;
	}

	@Override
	public String aggregateComponentCustomReadExpression(
			String template,
			String placeholder,
			String aggregateParentReadExpression,
			String columnExpression,
			int aggregateColumnTypeCode,
			SqlTypedMapping column,
			TypeConfiguration typeConfiguration) {
		switch ( aggregateColumnTypeCode ) {
			case JSON_ARRAY:
			case JSON:
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case JSON:
					case JSON_ARRAY:
						return template.replace(
								placeholder,
								aggregateParentReadExpression + "->'" + columnExpression + "'"
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"decode(" + aggregateParentReadExpression + "->>'" + columnExpression + "','hex')"
						);
					case ARRAY:
						final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) column.getJdbcMapping();
						switch ( pluralType.getElementType().getJdbcType().getDefaultSqlTypeCode() ) {
							case BOOLEAN:
							case TINYINT:
							case SMALLINT:
							case INTEGER:
							case BIGINT:
							case FLOAT:
							case DOUBLE:
								// For types that are natively supported in jsonb we can use jsonb_array_elements,
								// but note that we can't use that for string types,
								// because casting a jsonb[] to text[] will not omit the quotes of the jsonb text values
								return template.replace(
										placeholder,
										"cast(array(select jsonb_array_elements(" + aggregateParentReadExpression + "->'" + columnExpression + "')) as " + column.getColumnDefinition() + ')'
								);
							case BINARY:
							case VARBINARY:
							case LONG32VARBINARY:
								// We encode binary data as hex, so we have to decode here
								return template.replace(
										placeholder,
										"array(select decode(jsonb_array_elements_text(" + aggregateParentReadExpression + "->'" + columnExpression + "'),'hex'))"
								);
							default:
								return template.replace(
										placeholder,
										"cast(array(select jsonb_array_elements_text(" + aggregateParentReadExpression + "->'" + columnExpression + "')) as " + column.getColumnDefinition() + ')'
								);
						}
					default:
						return template.replace(
								placeholder,
								"cast(" + aggregateParentReadExpression + "->>'" + columnExpression + "' as " + column.getColumnDefinition() + ')'
						);
				}
			case XML_ARRAY:
			case SQLXML:
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case SQLXML:
						return template.replace(
								placeholder,
								XML_EXTRACT_START + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/*" ) + XML_EXTRACT_END
						);
					case XML_ARRAY:
						if ( typeConfiguration.getCurrentBaseSqlTypeIndicators().isXmlFormatMapperLegacyFormatEnabled() ) {
							throw new IllegalArgumentException( "XML array '" + columnExpression + "' in '" + aggregateParentReadExpression + "' is not supported with legacy format enabled." );
						}
						else {
							return template.replace(
									placeholder,
									"xmlelement(name \"Collection\",(select xmlagg(t.v order by t.i) from xmltable(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/*" ) + " columns v xml path '.', i for ordinality)t))"
							);
						}
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"decode((select t.v from xmltable(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression )+ " columns v text path '.') t),'hex')"
						);
					case ARRAY:
						throw new UnsupportedOperationException( "Transforming XML_ARRAY to native arrays is not supported on GaussDB!" );
					default:
						return template.replace(
								placeholder,
								"(select t.v from xmltable(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression ) + " columns v " + column.getColumnDefinition() + " path '.') t)"
						);
				}
			case STRUCT:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				return template.replace( placeholder, '(' + aggregateParentReadExpression + ")." + columnExpression );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static String xmlExtractArguments(String aggregateParentReadExpression, String xpathFragment) {
		final String extractArguments;
		int separatorIndex;
		if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_START )
				&& aggregateParentReadExpression.endsWith( XML_EXTRACT_END )
				&& (separatorIndex = aggregateParentReadExpression.indexOf( XML_EXTRACT_SEPARATOR )) != -1 ) {
			final StringBuilder sb = new StringBuilder( aggregateParentReadExpression.length() - XML_EXTRACT_START.length() + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, XML_EXTRACT_START.length(), separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex + 2, aggregateParentReadExpression.length() - XML_EXTRACT_END.length() );
			extractArguments = sb.toString();
		}
		else if ( aggregateParentReadExpression.startsWith( XML_QUERY_START )
				&& aggregateParentReadExpression.endsWith( XML_QUERY_END )
				&& (separatorIndex = aggregateParentReadExpression.indexOf( XML_QUERY_SEPARATOR )) != -1 ) {
			final StringBuilder sb = new StringBuilder( aggregateParentReadExpression.length() - XML_QUERY_START.length() + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, XML_QUERY_START.length(), separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex, aggregateParentReadExpression.length() - XML_QUERY_END.length() );
			extractArguments = sb.toString();
		}
		else {
			extractArguments = "'/" + XmlHelper.ROOT_TAG + "/" + xpathFragment + "' passing " + aggregateParentReadExpression;
		}
		return extractArguments;
	}

	private static String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				// We encode binary data as hex
				return "to_jsonb(encode(" + customWriteExpression + ",'hex'))";
			case ARRAY:
				final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) jdbcMapping;
				switch ( pluralType.getElementType().getJdbcType().getDefaultSqlTypeCode() ) {
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						// We encode binary data as hex
						return "to_jsonb(array(select encode(unnest(" + customWriteExpression + "),'hex')))";
					default:
						return "to_jsonb(" + customWriteExpression + ")";
				}
			default:
				return "to_jsonb(" + customWriteExpression + ")";
		}
	}

	private static String xmlCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				// We encode binary data as hex
				return "encode(" + customWriteExpression + ",'hex')";
//			case ARRAY:
//				final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) jdbcMapping;
//				switch ( pluralType.getElementType().getJdbcType().getDefaultSqlTypeCode() ) {
//					case BINARY:
//					case VARBINARY:
//					case LONG32VARBINARY:
//						// We encode binary data as hex
//						return "to_jsonb(array(select encode(unnest(" + customWriteExpression + "),'hex')))";
//					default:
//						return "to_jsonb(" + customWriteExpression + ")";
//				}
			default:
				return customWriteExpression;
		}
	}

	@Override
	public String aggregateComponentAssignmentExpression(
			String aggregateParentAssignmentExpression,
			String columnExpression,
			int aggregateColumnTypeCode,
			Column column) {
		switch ( aggregateColumnTypeCode ) {
			case JSON:
			case JSON_ARRAY:
			case SQLXML:
			case XML_ARRAY:
				// For JSON/XML we always have to replace the whole object
				return aggregateParentAssignmentExpression;
			case STRUCT:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				return aggregateParentAssignmentExpression + "." + columnExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		switch ( aggregateSqlTypeCode ) {
			case JSON:
			case SQLXML:
				return true;
		}
		return false;
	}

	@Override
	public boolean preferSelectAggregateMapping(int aggregateSqlTypeCode) {
		// The JDBC driver does not support selecting java.sql.Struct, so return false to select individual parts
		return aggregateSqlTypeCode != STRUCT;
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		switch ( aggregateSqlTypeCode ) {
			case JSON:
				return new RootJsonWriteExpression( aggregateColumn, columnsToUpdate );
			case SQLXML:
				return new RootXmlWriteExpression( aggregateColumn, columnsToUpdate );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
	}

	interface JsonWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}
	private static class AggregateJsonWriteExpression implements JsonWriteExpression {
		private final LinkedHashMap<String, JsonWriteExpression> subExpressions = new LinkedHashMap<>();

		protected void initializeSubExpressions(SelectableMapping[] columns) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath selectablePath = column.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				AggregateJsonWriteExpression currentAggregate = this;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					currentAggregate = (AggregateJsonWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new AggregateJsonWriteExpression()
					);
				}
				final String customWriteExpression = column.getWriteExpression();
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new BasicJsonWriteExpression(
								column,
								jsonCustomWriteExpression( customWriteExpression, column.getJdbcMapping() )
						)
				);
			}
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( "||jsonb_build_object" );
			char separator = '(';
			for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final JsonWriteExpression value = entry.getValue();
				final String subPath = path + "->'" + column + "'";
				sb.append( separator );
				if ( value instanceof AggregateJsonWriteExpression ) {
					sb.append( '\'' );
					sb.append( column );
					sb.append( "',coalesce(" );
					sb.append( subPath );
					sb.append( ",'{}')" );
					value.append( sb, subPath, translator, expression );
				}
				else {
					value.append( sb, subPath, translator, expression );
				}
				separator = ',';
			}
			sb.append( ')' );
		}
	}

	private static class RootJsonWriteExpression extends AggregateJsonWriteExpression
			implements WriteExpressionRenderer {
		private final boolean nullable;
		private final String path;

		RootJsonWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			this.nullable = aggregateColumn.isNullable();
			this.path = aggregateColumn.getSelectionExpression();
			initializeSubExpressions( columns );
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression aggregateColumnWriteExpression,
				String qualifier) {
			final String basePath;
			if ( qualifier == null || qualifier.isBlank() ) {
				basePath = path;
			}
			else {
				basePath = qualifier + "." + path;
			}
			if ( nullable ) {
				sqlAppender.append( "coalesce(" );
				sqlAppender.append( basePath );
				sqlAppender.append( ",'{}')" );
			}
			else {
				sqlAppender.append( basePath );
			}
			append( sqlAppender, basePath, translator, aggregateColumnWriteExpression );
		}
	}
	private static class BasicJsonWriteExpression implements JsonWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String customWriteExpressionStart;
		private final String customWriteExpressionEnd;

		BasicJsonWriteExpression(SelectableMapping selectableMapping, String customWriteExpression) {
			this.selectableMapping = selectableMapping;
			if ( customWriteExpression.equals( "?" ) ) {
				this.customWriteExpressionStart = "";
				this.customWriteExpressionEnd = "";
			}
			else {
				final String[] parts = StringHelper.split( "?", customWriteExpression );
				assert parts.length == 2;
				this.customWriteExpressionStart = parts[0];
				this.customWriteExpressionEnd = parts[1];
			}
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( '\'' );
			sb.append( selectableMapping.getSelectableName() );
			sb.append( "'," );
			sb.append( customWriteExpressionStart );
			// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
			// since we don't know how the custom write expression looks like where this is embedded,
			// so we have to be pessimistic and avoid ambiguities
			translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
			sb.append( customWriteExpressionEnd );
		}
	}

	interface XmlWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}
	private static class AggregateXmlWriteExpression implements XmlWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String columnDefinition;
		private final LinkedHashMap<String, XmlWriteExpression> subExpressions = new LinkedHashMap<>();

		private AggregateXmlWriteExpression(SelectableMapping selectableMapping, String columnDefinition) {
			this.selectableMapping = selectableMapping;
			this.columnDefinition = columnDefinition;
		}

		protected void initializeSubExpressions(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath selectablePath = column.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				AggregateXmlWriteExpression currentAggregate = this;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) currentAggregate.selectableMapping.getJdbcMapping().getJdbcType();
					final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
					final int selectableIndex = embeddableMappingType.getSelectableIndex( parts[i].getSelectableName() );
					currentAggregate = (AggregateXmlWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new AggregateXmlWriteExpression( embeddableMappingType.getJdbcValueSelectable( selectableIndex ), columnDefinition )
					);
				}
				final String customWriteExpression = column.getWriteExpression();
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new BasicXmlWriteExpression(
								column,
								xmlCustomWriteExpression( customWriteExpression, column.getJdbcMapping() )
						)
				);
			}
			passThroughUnsetSubExpressions( aggregateColumn );
		}

		protected void passThroughUnsetSubExpressions(SelectableMapping aggregateColumn) {
			final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) aggregateColumn.getJdbcMapping().getJdbcType();
			final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
			final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
			for ( int i = 0; i < jdbcValueCount; i++ ) {
				final SelectableMapping selectableMapping = embeddableMappingType.getJdbcValueSelectable( i );

				final XmlWriteExpression xmlWriteExpression = subExpressions.get( selectableMapping.getSelectableName() );
				if ( xmlWriteExpression == null ) {
					subExpressions.put(
							selectableMapping.getSelectableName(),
							new PassThroughXmlWriteExpression( selectableMapping )
					);
				}
				else if ( xmlWriteExpression instanceof AggregateXmlWriteExpression writeExpression ) {
					writeExpression.passThroughUnsetSubExpressions( selectableMapping );
				}
			}
		}

		protected String getTagName() {
			return selectableMapping.getSelectableName();
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( "xmlelement(name " );
			sb.appendDoubleQuoteEscapedString( getTagName() );
			sb.append( ",xmlconcat" );
			char separator = '(';
			for ( Map.Entry<String, XmlWriteExpression> entry : subExpressions.entrySet() ) {
				sb.append( separator );

				final XmlWriteExpression value = entry.getValue();
				if ( value instanceof AggregateXmlWriteExpression ) {
					final String subPath = XML_QUERY_START + xmlExtractArguments( path, entry.getKey() ) + XML_QUERY_END;
					value.append( sb, subPath, translator, expression );
				}
				else {
					value.append( sb, path, translator, expression );
				}
				separator = ',';
			}
			sb.append( "))" );
		}
	}

	private static class RootXmlWriteExpression extends AggregateXmlWriteExpression
			implements WriteExpressionRenderer {
		private final String path;

		RootXmlWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			super( aggregateColumn, aggregateColumn.getColumnDefinition() );
			path = aggregateColumn.getSelectionExpression();
			initializeSubExpressions( aggregateColumn, columns );
		}

		@Override
		protected String getTagName() {
			return XmlHelper.ROOT_TAG;
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression aggregateColumnWriteExpression,
				String qualifier) {
			final String basePath;
			if ( qualifier == null || qualifier.isBlank() ) {
				basePath = path;
			}
			else {
				basePath = qualifier + "." + path;
			}
			append( sqlAppender, XML_QUERY_START + "'/" + getTagName() + "' passing " + basePath + XML_QUERY_END, translator, aggregateColumnWriteExpression );
		}
	}
	private static class BasicXmlWriteExpression implements XmlWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String[] customWriteExpressionParts;

		BasicXmlWriteExpression(SelectableMapping selectableMapping, String customWriteExpression) {
			this.selectableMapping = selectableMapping;
			if ( customWriteExpression.equals( "?" ) ) {
				this.customWriteExpressionParts = new String[]{ "", "" };
			}
			else {
				assert !customWriteExpression.startsWith( "?" );
				final String[] parts = StringHelper.split( "?", customWriteExpression );
				assert parts.length == 2 || (parts.length & 1) == 1;
				this.customWriteExpressionParts = parts;
			}
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			final JdbcType jdbcType = selectableMapping.getJdbcMapping().getJdbcType();
			final boolean isArray = jdbcType.getDefaultSqlTypeCode() == XML_ARRAY;
			sb.append( "xmlelement(name " );
			sb.appendDoubleQuoteEscapedString( selectableMapping.getSelectableName() );
			sb.append( ',' );
			if ( isArray ) {
				// Remove the <Collection> tag to wrap the value into the selectable specific tag
				sb.append( "(select xmlagg(t.v order by t.i) from xmltable('/Collection/*' passing " );
			}
			sb.append( customWriteExpressionParts[0] );
			for ( int i = 1; i < customWriteExpressionParts.length; i++ ) {
				// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
				// since we don't know how the custom write expression looks like where this is embedded,
				// so we have to be pessimistic and avoid ambiguities
				translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
				sb.append( customWriteExpressionParts[i] );
			}
			if ( isArray ) {
				sb.append( " columns v xml path '.', i for ordinality)t)" );
			}
			sb.append( ')' );
		}
	}

	private static class PassThroughXmlWriteExpression implements XmlWriteExpression {

		private final SelectableMapping selectableMapping;

		PassThroughXmlWriteExpression(SelectableMapping selectableMapping) {
			this.selectableMapping = selectableMapping;
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( XML_QUERY_START );
			sb.append( xmlExtractArguments( path, selectableMapping.getSelectableName() ) );
			sb.append( XML_QUERY_END );
		}
	}

}

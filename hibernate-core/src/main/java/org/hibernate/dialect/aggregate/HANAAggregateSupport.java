/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.dialect.function.json.HANAJsonValueFunction.jsonValueReturningType;
import static org.hibernate.dialect.function.xml.HANAXmlTableFunction.xmlValueReturningType;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

public class HANAAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport INSTANCE = new HANAAggregateSupport();

	private static final String JSON_QUERY_START = "json_query(";
	private static final String JSON_QUERY_END = "' error on error)";
	private static final String XML_EXTRACT_START = "xmlextract(";
	private static final String XML_EXTRACT_END = "')";
	private static final String XML_EXTRACT_READ_START = "case when ";
	private static final String XML_EXTRACT_READ_NULL_CHECK = " is null then null else ";
	private static final String XML_EXTRACT_READ_INVOCATION_START = "'<" + XmlHelper.ROOT_TAG + ">'||xmlextract(";
	private static final String XML_EXTRACT_READ_END = "/*')||'</" + XmlHelper.ROOT_TAG + ">' end";

	private HANAAggregateSupport() {
	}

	public static AggregateSupport valueOf(Dialect dialect) {
		return dialect.getVersion().isSameOrAfter( 2, 0, 40 ) ? INSTANCE : AggregateSupportImpl.INSTANCE;
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
			case JSON:
			case JSON_ARRAY:
				final String jsonParentPartExpression = determineJsonParentPartExpression( aggregateParentReadExpression );
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case BOOLEAN:
						if ( SqlTypes.isNumericType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									"case json_value(" + jsonParentPartExpression + columnExpression + "') when 'true' then 1 when 'false' then 0 end"
							);
						}
						else {
							return template.replace(
									placeholder,
									"case json_value(" + jsonParentPartExpression + columnExpression + "') when 'true' then true when 'false' then false end"
							);
						}
					case DATE:
					case TIME:
					case TIMESTAMP:
					case TIMESTAMP_UTC:
						return template.replace(
								placeholder,
								"cast(json_value(" + jsonParentPartExpression + columnExpression + "') as " + column.getColumnDefinition() + ")"
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
					case BLOB:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"hextobin(json_value(" + jsonParentPartExpression + columnExpression + "' error on error))"
						);
					case JSON:
					case JSON_ARRAY:
						return template.replace(
								placeholder,
								"json_query(" + jsonParentPartExpression + columnExpression + "' error on error)"
						);
					case UUID:
						if ( SqlTypes.isBinaryType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									"hextobin(replace(json_value(" + jsonParentPartExpression + columnExpression + "'),'-',''))"
							);
						}
						// Fall-through intended
					default:
						return template.replace(
								placeholder,
								"json_value(" + jsonParentPartExpression + columnExpression + "' returning " + jsonValueReturningType( column ) + " error on error)"
						);
				}
			case SQLXML:
			case XML_ARRAY:
				final String xmlParentPartExpression;
				final int patternIdx;
				final String caseExpression;
				if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_READ_START )
					&& aggregateParentReadExpression.endsWith( XML_EXTRACT_READ_END )
					&& (patternIdx = aggregateParentReadExpression.indexOf( XML_EXTRACT_READ_NULL_CHECK )) != -1
					&& aggregateParentReadExpression.regionMatches( patternIdx + XML_EXTRACT_READ_NULL_CHECK.length(),
						XML_EXTRACT_READ_INVOCATION_START, 0, XML_EXTRACT_READ_INVOCATION_START.length() )) {
					caseExpression = aggregateParentReadExpression.substring( 0, patternIdx + XML_EXTRACT_READ_NULL_CHECK.length() );
					xmlParentPartExpression = aggregateParentReadExpression.substring(
							patternIdx + XML_EXTRACT_READ_NULL_CHECK.length() + XML_EXTRACT_READ_INVOCATION_START.length(),
							aggregateParentReadExpression.length() - XML_EXTRACT_READ_END.length()
					) + "/";
				}
				else {
					caseExpression = XML_EXTRACT_READ_START + aggregateParentReadExpression + XML_EXTRACT_READ_NULL_CHECK;
					xmlParentPartExpression = aggregateParentReadExpression + ",'/" + XmlHelper.ROOT_TAG + "/";
				}
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
					case BLOB:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								caseExpression + "hextobin(xmlextractvalue(" + xmlParentPartExpression + columnExpression + "')) end"
						);
					case DATE:
					case TIME:
					case TIMESTAMP:
					case TIMESTAMP_UTC:
						// Cast from clob to varchar first
						return template.replace(
								placeholder,
								caseExpression + "cast(cast(xmlextractvalue(" + xmlParentPartExpression + columnExpression + "') as varchar(36)) as " + xmlValueReturningType( column, column.getColumnDefinition() ) + ") end"
						);
					case SQLXML:
						return template.replace(
								placeholder,
								caseExpression + XML_EXTRACT_READ_INVOCATION_START + xmlParentPartExpression + columnExpression + XML_EXTRACT_READ_END
						);
					case XML_ARRAY:
						if ( typeConfiguration.getCurrentBaseSqlTypeIndicators().isXmlFormatMapperLegacyFormatEnabled() ) {
							throw new IllegalArgumentException( "XML array '" + columnExpression + "' in '" + aggregateParentReadExpression + "' is not supported with legacy format enabled." );
						}
						else {
							return template.replace(
									placeholder,
									caseExpression + "'<Collection>'||xmlextract(" + xmlParentPartExpression + columnExpression + "/*')||'</Collection>' end"
							);
						}
					case UUID:
						if ( SqlTypes.isBinaryType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									caseExpression + "hextobin(replace(xmlextractvalue(" + xmlParentPartExpression + columnExpression + "'),'-','')) end"
							);
						}
						// Fall-through intended
					default:
						return template.replace(
								placeholder,
								caseExpression + "cast(xmlextractvalue(" + xmlParentPartExpression + columnExpression + "') as " + xmlValueReturningType( column, column.getColumnDefinition() ) + ") end"
						);
				}
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static String determineJsonParentPartExpression(String aggregateParentReadExpression) {
		final String parentPartExpression;
		if ( aggregateParentReadExpression.startsWith( JSON_QUERY_START ) && aggregateParentReadExpression.endsWith( JSON_QUERY_END ) ) {
			parentPartExpression = aggregateParentReadExpression.substring( JSON_QUERY_START.length(), aggregateParentReadExpression.length() - JSON_QUERY_END.length() ) + ".";
		}
		else {
			parentPartExpression = aggregateParentReadExpression + ",'$.";
		}
		return parentPartExpression;
	}

	private static String determineXmlParentPartExpression(String aggregateParentReadExpression) {
		final String parentPartExpression;
		if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_START ) && aggregateParentReadExpression.endsWith( XML_EXTRACT_END ) ) {
			parentPartExpression = aggregateParentReadExpression.substring( XML_EXTRACT_START.length(), aggregateParentReadExpression.length() - XML_EXTRACT_END.length() ) + "/";
		}
		else {
			parentPartExpression = aggregateParentReadExpression + ",'/" + XmlHelper.ROOT_TAG + "/";
		}
		return parentPartExpression;
	}

	private static String customWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case UUID:
				return "replace_regexpr('^(.{8})(.{4})(.{4})(.{4})(.{12})$' in lower(bintohex(" + customWriteExpression + ")) with '\\1-\\2-\\3-\\4-\\5')";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				// We encode binary data as hex
				return "bintohex(" + customWriteExpression + ")";
			case TIMESTAMP:
				return "to_varchar(" + customWriteExpression + ",'YYYY-MM-DD\"T\"HH24:MI:SS.FF9')";
			case TIMESTAMP_UTC:
				return "to_varchar(" + customWriteExpression + ",'YYYY-MM-DD\"T\"HH24:MI:SS.FF9\"Z\"')";
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
				// For JSON and XML we always have to replace the whole object
				return aggregateParentAssignmentExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	@Override
	public String aggregateCustomWriteExpression(
			AggregateColumn aggregateColumn,
			List<Column> aggregatedColumns) {
		// We need to know what array this is STRUCT_ARRAY/JSON_ARRAY/XML_ARRAY,
		// which we can easily get from the type code of the aggregate column
		final int sqlTypeCode = aggregateColumn.getType().getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode == SqlTypes.ARRAY ? aggregateColumn.getTypeCode() : sqlTypeCode ) {
			case JSON:
			case JSON_ARRAY:
			case SQLXML:
			case XML_ARRAY:
				return null;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		return aggregateSqlTypeCode == JSON || aggregateSqlTypeCode == SQLXML;
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
		boolean isAggregate();
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}
	private static class AggregateJsonWriteExpression implements JsonWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String columnDefinition;
		private final LinkedHashMap<String, JsonWriteExpression> subExpressions = new LinkedHashMap<>();

		private AggregateJsonWriteExpression(SelectableMapping selectableMapping, String columnDefinition) {
			this.selectableMapping = selectableMapping;
			this.columnDefinition = columnDefinition;
		}

		@Override
		public boolean isAggregate() {
			return true;
		}

		protected void initializeSubExpressions(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath selectablePath = column.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				AggregateJsonWriteExpression currentAggregate = this;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) currentAggregate.selectableMapping.getJdbcMapping().getJdbcType();
					final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
					final int selectableIndex = embeddableMappingType.getSelectableIndex( parts[i].getSelectableName() );
					currentAggregate = (AggregateJsonWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new AggregateJsonWriteExpression( embeddableMappingType.getJdbcValueSelectable( selectableIndex ), columnDefinition )
					);
				}
				final String customWriteExpression = column.getWriteExpression();
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new BasicJsonWriteExpression(
								column,
								customWriteExpression( customWriteExpression, column.getJdbcMapping() )
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

				final JsonWriteExpression jsonWriteExpression = subExpressions.get( selectableMapping.getSelectableName() );
				if ( jsonWriteExpression == null ) {
					subExpressions.put(
							selectableMapping.getSelectableName(),
							new PassThroughJsonWriteExpression( selectableMapping )
					);
				}
				else if ( jsonWriteExpression instanceof AggregateJsonWriteExpression writeExpression ) {
					writeExpression.passThroughUnsetSubExpressions( selectableMapping );
				}
			}
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			final int aggregateCount = determineAggregateCount();
			if ( aggregateCount != 0 ) {
				sb.append( "(trim(trailing '}' from " );
			}

			sb.append( "(select" );
			if ( aggregateCount != subExpressions.size() ) {
				char separator = ' ';
				for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
					final String column = entry.getKey();
					final JsonWriteExpression value = entry.getValue();
					if ( !value.isAggregate() ) {
						sb.append( separator );
						value.append( sb, path, translator, expression );
						sb.append( ' ' );
						sb.appendDoubleQuoteEscapedString( column );
						separator = ',';
					}
				}
				sb.append( " from sys.dummy for json('arraywrap'='no','omitnull'='no')" );
				sb.append( " returns " );
				sb.append( columnDefinition );
			}
			else {
				sb.append( " cast('{}' as " );
				sb.append( columnDefinition );
				sb.append( ") jsonresult from sys.dummy" );
			}
			sb.append( ')' );
			if ( aggregateCount != 0 ) {
				sb.append( ')' );
				final String parentPartExpression = determineJsonParentPartExpression( path );
				String separator = aggregateCount == subExpressions.size() ? " " : ",";
				for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
					final String column = entry.getKey();
					final JsonWriteExpression value = entry.getValue();
					if ( value.isAggregate() ) {
						sb.append( "||'" );
						sb.append( separator );
						sb.append( '"' );
						sb.append( column );
						sb.append( "\":'||" );
						if ( value instanceof AggregateJsonWriteExpression ) {
							final String subPath = "json_query(" + parentPartExpression + column + "' error on error)";
							value.append( sb, subPath, translator, expression );
						}
						else {
							sb.append( "coalesce(" );
							value.append( sb, path, translator, expression );
							sb.append( ",'null')" );
						}
						separator = ",";
					}
				}
				sb.append( "||'}')" );
			}
		}

		private int determineAggregateCount() {
			int count = 0;
			for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
				if ( entry.getValue().isAggregate() ) {
					count++;
				}
			}
			return count;
		}
	}

	private static class RootJsonWriteExpression extends AggregateJsonWriteExpression
			implements WriteExpressionRenderer {
		private final String path;

		RootJsonWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			super( aggregateColumn, aggregateColumn.getColumnDefinition() );
			path = aggregateColumn.getSelectionExpression();
			initializeSubExpressions( aggregateColumn, columns );
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
		public boolean isAggregate() {
			return selectableMapping.getJdbcMapping().getJdbcType().isJson();
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( customWriteExpressionStart );
			// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
			// since we don't know how the custom write expression looks like where this is embedded,
			// so we have to be pessimistic and avoid ambiguities
			translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
			sb.append( customWriteExpressionEnd );
		}
	}

	private static class PassThroughJsonWriteExpression implements JsonWriteExpression {

		private final SelectableMapping selectableMapping;

		PassThroughJsonWriteExpression(SelectableMapping selectableMapping) {
			this.selectableMapping = selectableMapping;
		}

		@Override
		public boolean isAggregate() {
			return selectableMapping.getJdbcMapping().getJdbcType().isJson();
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			final String parentPartExpression = determineJsonParentPartExpression( path );
			switch ( selectableMapping.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
				case BOOLEAN:
					sb.append( "case json_value(" );
					sb.append( parentPartExpression );
					sb.append( selectableMapping.getSelectableName() );
					if ( SqlTypes.isNumericType( selectableMapping.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
						sb.append( "') when 'true' then 1 when 'false' then 0 end" );
					}
					else {
						sb.append( "') when 'true' then true when 'false' then false end" );
					}
					break;
				case TINYINT:
				case SMALLINT:
				case INTEGER:
				case BIGINT:
				case FLOAT:
				case REAL:
				case DOUBLE:
				case DECIMAL:
				case NUMERIC:
					sb.append( "json_value(" );
					sb.append( parentPartExpression );
					sb.append( selectableMapping.getSelectableName() );
					sb.append( "' returning " );
					sb.append( jsonValueReturningType( selectableMapping ) );
					sb.append( " error on error)" );
					break;
				case JSON:
				case JSON_ARRAY:
					sb.append( "json_query(" );
					sb.append( parentPartExpression );
					sb.append( selectableMapping.getSelectableName() );
					sb.append( "' error on error)" );
					break;
				default:
					sb.append( "json_value(" );
					sb.append( parentPartExpression );
					sb.append( selectableMapping.getSelectableName() );
					sb.append( "' error on error)" );
					break;
			}
		}
	}

	interface XmlWriteExpression {
		boolean isAggregate();
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

		@Override
		public boolean isAggregate() {
			return true;
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
								customWriteExpression( customWriteExpression, column.getJdbcMapping() )
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
			final int aggregateCount = determineAggregateCount();
			if ( aggregateCount != 0 ) {
				sb.append( "(replace_regexpr('^(.*)</" );
				sb.append( getTagName() );
				sb.append( ">$' flag 's' in " );
			}

			sb.append( "(select" );
			if ( aggregateCount != subExpressions.size() ) {
				char separator = ' ';
				for ( Map.Entry<String, XmlWriteExpression> entry : subExpressions.entrySet() ) {
					final String column = entry.getKey();
					final XmlWriteExpression value = entry.getValue();
					if ( !value.isAggregate() ) {
						sb.append( separator );
						value.append( sb, path, translator, expression );
						sb.append( ' ' );
						sb.appendDoubleQuoteEscapedString( column );
						separator = ',';
					}
				}
				sb.append( " from sys.dummy for xml('root'='no','rowname'='" );
				sb.append( getTagName() );
				sb.append( "','format'='no','nullstyle'='attribute') returns " );
				sb.append( columnDefinition );
			}
			else {
				sb.append( " cast('<" );
				sb.append( getTagName() );
				sb.append( "></" );
				sb.append( getTagName() );
				sb.append( ">' as " );
				sb.append( columnDefinition );
				sb.append( ") xmlresult from sys.dummy" );
			}
			sb.append( ')' );
			if ( aggregateCount != 0 ) {
				sb.append( " with '\\1')" );
				final String parentPartExpression = determineXmlParentPartExpression( path );
				for ( Map.Entry<String, XmlWriteExpression> entry : subExpressions.entrySet() ) {
					final String column = entry.getKey();
					final XmlWriteExpression value = entry.getValue();
					if ( value.isAggregate() ) {
						sb.append( "||case when " );
						sb.append( path );
						sb.append( " is null then null else " );
						if ( value instanceof AggregateXmlWriteExpression ) {
							final String subPath = "xmlextract(" + parentPartExpression + column + "')";
							value.append( sb, subPath, translator, expression );
						}
						else {
							value.append( sb, path, translator, expression );
						}
						sb.append( " end" );
					}
				}
				sb.append( "||'</" );
				sb.append( getTagName() );
				sb.append( ">')" );
			}
		}

		private int determineAggregateCount() {
			int count = 0;
			for ( Map.Entry<String, XmlWriteExpression> entry : subExpressions.entrySet() ) {
				if ( entry.getValue().isAggregate() ) {
					count++;
				}
			}
			return count;
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
			append( sqlAppender, basePath, translator, aggregateColumnWriteExpression );
		}
	}
	private static class BasicXmlWriteExpression implements XmlWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String customWriteExpressionStart;
		private final String customWriteExpressionEnd;

		BasicXmlWriteExpression(SelectableMapping selectableMapping, String customWriteExpression) {
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
		public boolean isAggregate() {
			return selectableMapping.getJdbcMapping().getJdbcType().isXml();
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			final boolean isArray = selectableMapping.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() == XML_ARRAY;
			if ( isAggregate() ) {
				sb.append( "coalesce(" );
			}
			if ( isArray ) {
				sb.append( "'<" );
				sb.append( selectableMapping.getSelectableName() );
				sb.append( ">'||case when " );
				sb.append( customWriteExpressionStart );
				// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
				// since we don't know how the custom write expression looks like where this is embedded,
				// so we have to be pessimistic and avoid ambiguities
				translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
				sb.append( customWriteExpressionEnd );
				sb.append( " is null then null else xmlextract(" );
			}
			sb.append( customWriteExpressionStart );
			// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
			// since we don't know how the custom write expression looks like where this is embedded,
			// so we have to be pessimistic and avoid ambiguities
			translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
			sb.append( customWriteExpressionEnd );
			if ( isArray ) {
				sb.append( ",'/*/node()') end||'</" );
				sb.append( selectableMapping.getSelectableName() );
				sb.append( ">'" );
			}
			// Since xmlextractvalue throws an error if a xpath expression doesn't resolve to a node,
			// insert special null nodes
			if ( isAggregate() ) {
				sb.append( ",'<" );
				sb.append( selectableMapping.getSelectableName() );
				if ( selectableMapping.getJdbcMapping().getJdbcType() instanceof AggregateJdbcType ) {
					sb.append( ">" );
					appendNullTags( sb, selectableMapping );
					sb.append( "</" );
					sb.append( selectableMapping.getSelectableName() );
					sb.append( ">')" );
				}
				else {
					sb.append( "/>')" );
				}
			}
		}

		private void appendNullTags(SqlAppender sb, SelectableMapping parentMapping) {
			final AggregateJdbcType jdbcType = (AggregateJdbcType) parentMapping.getJdbcMapping().getJdbcType();
			final EmbeddableMappingType embeddableMappingType = jdbcType.getEmbeddableMappingType();
			final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
			for ( int i = 0; i < jdbcValueCount; i++ ) {
				final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable( i );
				sb.append( "<" );
				if ( selectable.getJdbcMapping().getJdbcType() instanceof AggregateJdbcType ) {
					sb.append( selectable.getSelectableName() );
					sb.append( ">" );
					appendNullTags( sb, selectable );
					sb.append( "</" );
					sb.append( selectable.getSelectableName() );
					sb.append( ">" );
				}
				else {
					sb.append( selectable.getSelectableName() );
					sb.append( "/>" );
				}
			}
		}
	}

	private static class PassThroughXmlWriteExpression implements XmlWriteExpression {

		private final SelectableMapping selectableMapping;

		PassThroughXmlWriteExpression(SelectableMapping selectableMapping) {
			this.selectableMapping = selectableMapping;
		}

		@Override
		public boolean isAggregate() {
			return selectableMapping.getJdbcMapping().getJdbcType().isXml();
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			final String parentPartExpression = determineXmlParentPartExpression( path );
			switch ( selectableMapping.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
				case SQLXML:
				case XML_ARRAY:
					sb.append( "xmlextract(" );
					sb.append( parentPartExpression );
					sb.append( selectableMapping.getSelectableName() );
					sb.append( "')" );
					break;
				default:
					sb.append( "xmlextractvalue(" );
					sb.append( parentPartExpression );
					sb.append( selectableMapping.getSelectableName() );
					sb.append( "')" );
					break;
			}
		}
	}


}

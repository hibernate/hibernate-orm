/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.json.HANAJsonValueFunction;
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

import static org.hibernate.type.SqlTypes.ARRAY;
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
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;

public class HANAAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport INSTANCE = new HANAAggregateSupport();

	private static final String JSON_QUERY_START = "json_query(";
	private static final String JSON_QUERY_JSON_END = "' error on error)";

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
			SqlTypedMapping column) {
		switch ( aggregateColumnTypeCode ) {
			case JSON:
			case JSON_ARRAY:
				final String parentPartExpression = determineParentPartExpression( aggregateParentReadExpression );
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case BOOLEAN:
						if ( SqlTypes.isNumericType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									"case json_value(" + parentPartExpression + columnExpression + "') when 'true' then 1 when 'false' then 0 end"
							);
						}
						else {
							return template.replace(
									placeholder,
									"case json_value(" + parentPartExpression + columnExpression + "') when 'true' then true when 'false' then false end"
							);
						}
					case DATE:
					case TIME:
					case TIMESTAMP:
					case TIMESTAMP_UTC:
						return template.replace(
								placeholder,
								"cast(json_value(" + parentPartExpression + columnExpression + "') as " + column.getColumnDefinition() + ")"
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
					case BLOB:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"hextobin(json_value(" + parentPartExpression + columnExpression + "' error on error))"
						);
					case JSON:
					case JSON_ARRAY:
						return template.replace(
								placeholder,
								"json_query(" + parentPartExpression + columnExpression + "' error on error)"
						);
					case UUID:
						if ( SqlTypes.isBinaryType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									"hextobin(json_value(" + parentPartExpression + columnExpression + "'))"
							);
						}
						// Fall-through intended
					default:
						return template.replace(
								placeholder,
								"json_value(" + parentPartExpression + columnExpression + "' returning " + HANAJsonValueFunction.jsonValueReturningType(
										column ) + " error on error)"
						);
				}
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static String determineParentPartExpression(String aggregateParentReadExpression) {
		final String parentPartExpression;
		if ( aggregateParentReadExpression.startsWith( JSON_QUERY_START ) && aggregateParentReadExpression.endsWith( JSON_QUERY_JSON_END ) ) {
			parentPartExpression = aggregateParentReadExpression.substring( JSON_QUERY_START.length(), aggregateParentReadExpression.length() - JSON_QUERY_JSON_END.length() ) + ".";
		}
		else {
			parentPartExpression = aggregateParentReadExpression + ",'$.";
		}
		return parentPartExpression;
	}

	private static String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case UUID:
				if ( !SqlTypes.isBinaryType( jdbcMapping.getJdbcType().getDdlTypeCode() ) ) {
					return customWriteExpression;
				}
				// Fall-through intended
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
				// For JSON we always have to replace the whole object
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
				return null;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
	}

	@Override
	public int aggregateComponentSqlTypeCode(int aggregateColumnSqlTypeCode, int columnSqlTypeCode) {
		if ( aggregateColumnSqlTypeCode == JSON ) {
			return columnSqlTypeCode == ARRAY ? JSON_ARRAY : columnSqlTypeCode;
		}
		else {
			return columnSqlTypeCode;
		}
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		return aggregateSqlTypeCode == JSON;
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		switch ( aggregateSqlTypeCode ) {
			case JSON:
				return jsonAggregateColumnWriter( aggregateColumn, columnsToUpdate );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
	}

	private WriteExpressionRenderer jsonAggregateColumnWriter(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columns) {
		return new RootJsonWriteExpression( aggregateColumn, columns );
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
							k -> new AggregateJsonWriteExpression( embeddableMappingType.getSelectable( selectableIndex ), columnDefinition )
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
							new PassThroughExpression( selectableMapping )
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
				final String parentPartExpression = determineParentPartExpression( path );
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

	private static class PassThroughExpression implements JsonWriteExpression {

		private final SelectableMapping selectableMapping;

		PassThroughExpression(SelectableMapping selectableMapping) {
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
			final String parentPartExpression = determineParentPartExpression( path );
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
					sb.append( HANAJsonValueFunction.jsonValueReturningType( selectableMapping ) );
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

}

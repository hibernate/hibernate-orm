/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import org.hibernate.dialect.Dialect;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
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
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hibernate.type.SqlTypes.*;

public class SQLServerAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport JSON_INSTANCE = new SQLServerAggregateSupport( true );
	private static final AggregateSupport LEGACY_INSTANCE = new SQLServerAggregateSupport( false );

	private static final String JSON_QUERY_START = "json_query(";
	private static final String JSON_QUERY_JSON_END = "')";
	private static final int JSON_VALUE_MAX_LENGTH = 4000;
	private static final String XML_QUERY_START = "cast('<e>'+cast(";
	private static final String XML_QUERY_SEPARATOR = ".query('";
	private static final String XML_QUERY_END = "/*') as nvarchar(max))+'</e>' as xml)";

	private final boolean supportsJson;

	private SQLServerAggregateSupport(boolean supportsJson) {
		this.supportsJson = supportsJson;
	}

	public static AggregateSupport valueOf(Dialect dialect) {
		return dialect.getVersion().isSameOrAfter( 13 )
				? SQLServerAggregateSupport.JSON_INSTANCE
				: SQLServerAggregateSupport.LEGACY_INSTANCE;
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
				if ( !supportsJson ) {
					break;
				}
				final String parentJsonPartExpression;
				if ( aggregateParentReadExpression.startsWith( JSON_QUERY_START )
						&& aggregateParentReadExpression.endsWith( JSON_QUERY_JSON_END ) ) {
					parentJsonPartExpression = aggregateParentReadExpression.substring( JSON_QUERY_START.length(), aggregateParentReadExpression.length() - JSON_QUERY_JSON_END.length() ) + ".";
				}
				else {
					parentJsonPartExpression = aggregateParentReadExpression + ",'$.";
				}
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case JSON:
					case JSON_ARRAY:
						return template.replace(
								placeholder,
								"json_query(" + parentJsonPartExpression + columnExpression + "')"
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
					case BLOB:
						// We encode binary data as hex, so we have to decode here
						if ( determineLength( column ) * 2 > JSON_VALUE_MAX_LENGTH ) {
							// Since data is HEX encoded, multiply the max length by 2 since we need 2 hex chars per byte
							return template.replace(
									placeholder,
									"(select convert(" + column.getColumnDefinition() + ",v,2) from openjson(" + aggregateParentReadExpression + ") with (v varchar(max) '$." + columnExpression + "'))"
							);
						}
						else {
							return template.replace(
									placeholder,
									"convert(" + column.getColumnDefinition() + ",json_value(" + parentJsonPartExpression + columnExpression + "'),2)"
							);
						}
					case CHAR:
					case NCHAR:
					case VARCHAR:
					case NVARCHAR:
					case LONG32VARCHAR:
					case LONG32NVARCHAR:
					case CLOB:
					case NCLOB:
						if ( determineLength( column ) > JSON_VALUE_MAX_LENGTH ) {
							return template.replace(
									placeholder,
									"(select * from openjson(" + aggregateParentReadExpression + ") with (v " + column.getColumnDefinition() + " '$." + columnExpression + "'))"
							);
						}
						// Fall-through intended
					case BIT:
					case TINYINT:
					case SMALLINT:
					case INTEGER:
					case BIGINT:
					case REAL:
					case FLOAT:
					case DOUBLE:
					case NUMERIC:
					case DECIMAL:
					case TIME:
					case TIME_UTC:
					case TIME_WITH_TIMEZONE:
					case DATE:
					case TIMESTAMP:
					case TIMESTAMP_UTC:
					case TIMESTAMP_WITH_TIMEZONE:
						return template.replace(
								placeholder,
								"cast(json_value(" + parentJsonPartExpression + columnExpression + "') as " + column.getColumnDefinition() + ")"
						);
					default:
						return template.replace(
								placeholder,
								"(select * from openjson(" + aggregateParentReadExpression + ") with (v " + column.getColumnDefinition() + " '$." + columnExpression + "'))"
						);
				}
			case SQLXML:
			case XML_ARRAY:
				final String xmlColumn;
				final String parentXmlPartExpression;
				final int queryIndex;
				if ( aggregateParentReadExpression.startsWith( XML_QUERY_START )
					&& aggregateParentReadExpression.endsWith( XML_QUERY_END )
					&& (queryIndex = aggregateParentReadExpression.indexOf( XML_QUERY_SEPARATOR )) != -1 ) {
					xmlColumn = aggregateParentReadExpression.substring( XML_QUERY_START.length(), queryIndex );
					parentXmlPartExpression = aggregateParentReadExpression.substring( queryIndex + XML_QUERY_SEPARATOR.length(), aggregateParentReadExpression.length() - XML_QUERY_END.length() );
				}
				else {
					xmlColumn = aggregateParentReadExpression;
					parentXmlPartExpression = "/" + XmlHelper.ROOT_TAG;
				}
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case SQLXML:
						return template.replace(
								placeholder,
								XML_QUERY_START + xmlColumn + XML_QUERY_SEPARATOR + parentXmlPartExpression + "/" + columnExpression + XML_QUERY_END
						);
					case XML_ARRAY:
						return template.replace(
								placeholder,
								"cast('<Collection>'+cast(" + xmlColumn + XML_QUERY_SEPARATOR + parentXmlPartExpression + "/" + columnExpression + "/*') as nvarchar(max))+'</Collection>' as xml)"
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
					case BLOB:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"convert(" + column.getColumnDefinition() + "," + xmlColumn + ".value('(" + parentXmlPartExpression + "/" + columnExpression + "/text())[1]','nvarchar(max)'),2)"
						);
					default:
						return template.replace(
								placeholder,
								xmlColumn + ".value('(" + parentXmlPartExpression + "/" + columnExpression + "/text())[1]','" + column.getColumnDefinition() + "')"
						);
				}
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static Long determineLength(SqlTypedMapping column) {
		final Long length = column.getLength();
		if ( length != null ) {
			return length;
		}
		else {
			final String columnDefinition = column.getColumnDefinition();
			assert columnDefinition != null;
			final int parenthesisIndex = columnDefinition.indexOf( '(' );
			if ( parenthesisIndex != -1 ) {
				int end;
				for ( end = parenthesisIndex + 1; end < columnDefinition.length(); end++ ) {
					if ( !Character.isDigit( columnDefinition.charAt( end ) ) ) {
						break;
					}
				}
				return Long.parseLong( columnDefinition.substring( parenthesisIndex + 1, end ) );
			}
			// Default to the max varchar length
			return 8000L;
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
				if ( !supportsJson ) {
					break;
				}
			case SQLXML:
			case XML_ARRAY:
				// For JSON/XML we always have to replace the whole object
				return aggregateParentAssignmentExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				return "convert(nvarchar(max)," + customWriteExpression + ",2)";
			case TIME:
				return "left(" + customWriteExpression + ",8)";
			case DATE:
				return "format(" + customWriteExpression + ",'yyyy-MM-dd')";
			case TIMESTAMP:
				return "format(" + customWriteExpression + ",'yyyy-MM-ddTHH:mm:ss.fffffff')";
			case TIMESTAMP_UTC:
			case TIMESTAMP_WITH_TIMEZONE:
				return "format(" + customWriteExpression + ",'yyyy-MM-ddTHH:mm:ss.fffffffzzz')";
			case UUID:
				return "cast(" + customWriteExpression + " as nvarchar(36))";
			case JSON:
			case JSON_ARRAY:
				return "json_query(" + customWriteExpression + ")";
			default:
				return customWriteExpression;
		}
	}

	private static String xmlCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case BOOLEAN:
			case BIT:
				return "case " + customWriteExpression + " when 1 then 'true' when 0 then 'false' end";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				return "convert(nvarchar(max)," + customWriteExpression + ",2)";
			case TIME:
				return "left(" + customWriteExpression + ",8)";
			case DATE:
				return "format(" + customWriteExpression + ",'yyyy-MM-dd')";
			case TIMESTAMP:
				return "format(" + customWriteExpression + ",'yyyy-MM-ddTHH:mm:ss.fffffff')";
			case TIMESTAMP_UTC:
			case TIMESTAMP_WITH_TIMEZONE:
				return "format(" + customWriteExpression + ",'yyyy-MM-ddTHH:mm:ss.fffffffzzz')";
			case UUID:
				return "cast(" + customWriteExpression + " as nvarchar(36))";
			default:
				return customWriteExpression;
		}
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
				if ( !supportsJson ) {
					break;
				}
				return new RootJsonWriteExpression( aggregateColumn, columnsToUpdate, this, typeConfiguration );
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
		protected final EmbeddableMappingType embeddableMappingType;

		public AggregateJsonWriteExpression(SelectableMapping selectableMapping, SQLServerAggregateSupport aggregateSupport) {
			this.embeddableMappingType = ( (AggregateJdbcType) selectableMapping.getJdbcMapping().getJdbcType() )
					.getEmbeddableMappingType();
		}

		protected void initializeSubExpressions(
				SelectableMapping[] columns,
				SQLServerAggregateSupport aggregateSupport,
				TypeConfiguration typeConfiguration) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath selectablePath = column.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				AggregateJsonWriteExpression currentAggregate = this;
				EmbeddableMappingType currentMappingType = embeddableMappingType;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					final SelectableMapping selectableMapping = currentMappingType.getJdbcValueSelectable(
							currentMappingType.getSelectableIndex( parts[i].getSelectableName() )
					);
					currentAggregate = (AggregateJsonWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new AggregateJsonWriteExpression( selectableMapping, aggregateSupport )
					);
					currentMappingType = currentAggregate.embeddableMappingType;
				}
				final String customWriteExpression = column.getWriteExpression();
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new BasicJsonWriteExpression(
								column,
								jsonCustomWriteExpression(
										customWriteExpression,
										column.getJdbcMapping()
								)
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
			for ( int i = 0; i < subExpressions.size() - 1; i++ ) {
				sb.append( "json_modify(" );
			}
			sb.append( "json_modify(" );
			sb.append( path );
			for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final JsonWriteExpression value = entry.getValue();
				final String subPath = "json_query(" + path + ",'$." + column + "')";
				sb.append( ",'$." );
				sb.append( column );
				sb.append( "'," );
				if ( value instanceof AggregateJsonWriteExpression ) {
					value.append( sb, subPath, translator, expression );
				}
				else {
					value.append( sb, subPath, translator, expression );
				}
				sb.append( ')' );
			}
		}
	}

	private static class RootJsonWriteExpression extends AggregateJsonWriteExpression
			implements WriteExpressionRenderer {
		private final boolean nullable;
		private final String path;

		RootJsonWriteExpression(
				SelectableMapping aggregateColumn,
				SelectableMapping[] columns,
				SQLServerAggregateSupport aggregateSupport,
				TypeConfiguration typeConfiguration) {
			super( aggregateColumn, aggregateSupport );
			this.nullable = aggregateColumn.isNullable();
			this.path = aggregateColumn.getSelectionExpression();
			initializeSubExpressions( columns, aggregateSupport, typeConfiguration );
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
			append(
					sqlAppender,
					nullable ? "coalesce(" + basePath + ",'{}')" : basePath,
					translator,
					aggregateColumnWriteExpression
			);
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
			sb.append( "(select" );
			char separator = ' ';
			for ( Map.Entry<String, XmlWriteExpression> entry : subExpressions.entrySet() ) {
				sb.append( separator );

				final XmlWriteExpression value = entry.getValue();
				if ( value instanceof AggregateXmlWriteExpression ) {
					final String queryPrefix = path.substring( 0, path.length() - "')".length() );
					final String subPath = queryPrefix + "/" + entry.getKey() + "')";
					value.append( sb, subPath, translator, expression );
				}
				else {
					value.append( sb, path, translator, expression );
				}
				separator = ',';
			}
			sb.append( " for xml path(" );
			sb.appendSingleQuoteEscapedString( getTagName() );
			sb.append( "),type)" );
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
			append( sqlAppender, basePath + ".query('/" + getTagName() + "')", translator, aggregateColumnWriteExpression );
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
			if ( isArray ) {
				sb.append( '(' );
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
				// Remove the <Collection> tag to wrap the value into the selectable specific tag
				sb.append( ").query('/Collection/*')" );
			}
			sb.append( ' ' );
			sb.appendDoubleQuoteEscapedString( selectableMapping.getSelectableName() );
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
			assert path.endsWith( "')" );
			sb.append( path, 0, path.length() - 2 );
			sb.append( '/' );
			sb.append( selectableMapping.getSelectableName() );
			sb.append( "')" );
		}
	}

}

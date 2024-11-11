/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.Size;
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
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hibernate.type.SqlTypes.*;

public class SQLServerAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport INSTANCE = new SQLServerAggregateSupport();

	private static final String JSON_QUERY_START = "json_query(";
	private static final String JSON_QUERY_JSON_END = "')";
	private static final int JSON_VALUE_MAX_LENGTH = 4000;

	private SQLServerAggregateSupport() {
	}

	public static AggregateSupport valueOf(Dialect dialect) {
		return dialect.getVersion().isSameOrAfter( 13 )
				? SQLServerAggregateSupport.INSTANCE
				: AggregateSupportImpl.INSTANCE;
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
				final String parentPartExpression;
				if ( aggregateParentReadExpression.startsWith( JSON_QUERY_START )
						&& aggregateParentReadExpression.endsWith( JSON_QUERY_JSON_END ) ) {
					parentPartExpression = aggregateParentReadExpression.substring( JSON_QUERY_START.length(), aggregateParentReadExpression.length() - JSON_QUERY_JSON_END.length() ) + ".";
				}
				else {
					parentPartExpression = aggregateParentReadExpression + ",'$.";
				}
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case JSON:
					case JSON_ARRAY:
						return template.replace(
								placeholder,
								"json_query(" + parentPartExpression + columnExpression + "')"
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
									"convert(" + column.getColumnDefinition() + ",json_value(" + parentPartExpression + columnExpression + "'),2)"
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
								"cast(json_value(" + parentPartExpression + columnExpression + "') as " + column.getColumnDefinition() + ")"
						);
					default:
						return template.replace(
								placeholder,
								"(select * from openjson(" + aggregateParentReadExpression + ") with (v " + column.getColumnDefinition() + " '$." + columnExpression + "'))"
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
				// For JSON we always have to replace the whole object
				return aggregateParentAssignmentExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private String jsonCustomWriteExpression(
			String customWriteExpression,
			JdbcMapping jdbcMapping,
			SelectableMapping column,
			TypeConfiguration typeConfiguration) {
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

	private static String determineElementTypeName(
			Size castTargetSize,
			BasicPluralType<?, ?> pluralType,
			TypeConfiguration typeConfiguration) {
		final DdlTypeRegistry ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
		final BasicType<?> expressionType = pluralType.getElementType();
		DdlType ddlType = ddlTypeRegistry.getDescriptor( expressionType.getJdbcType().getDdlTypeCode() );
		if ( ddlType == null ) {
			// this may happen when selecting a null value like `SELECT null from ...`
			// some dbs need the value to be cast so not knowing the real type we fall back to INTEGER
			ddlType = ddlTypeRegistry.getDescriptor( SqlTypes.INTEGER );
		}

		return ddlType.getTypeName( castTargetSize, expressionType, ddlTypeRegistry );
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
				return jsonAggregateColumnWriter( aggregateColumn, columnsToUpdate, typeConfiguration );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
	}

	private WriteExpressionRenderer jsonAggregateColumnWriter(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columns,
			TypeConfiguration typeConfiguration) {
		return new RootJsonWriteExpression( aggregateColumn, columns, this, typeConfiguration );
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
								aggregateSupport.jsonCustomWriteExpression(
										customWriteExpression,
										column.getJdbcMapping(),
										column,
										typeConfiguration
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

}

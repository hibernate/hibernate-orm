/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.ENUM;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;

public class MySQLAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport JSON_INSTANCE = new MySQLAggregateSupport( true, false );
	private static final AggregateSupport JSON_WITH_UUID_INSTANCE = new MySQLAggregateSupport( true, true );
	private static final AggregateSupport LONGTEXT_INSTANCE = new MySQLAggregateSupport( false, false );

	private final boolean jsonType;
	private final boolean uuidFunctions;

	private MySQLAggregateSupport(boolean jsonType, boolean uuidFunctions) {
		this.jsonType = jsonType;
		this.uuidFunctions = uuidFunctions;
	}

	public static AggregateSupport forMySQL(Dialect dialect) {
		return dialect.getVersion().isSameOrAfter( 8 )
				? JSON_WITH_UUID_INSTANCE
				: dialect.getVersion().isSameOrAfter( 5, 7 )
						? JSON_INSTANCE
						: AggregateSupportImpl.INSTANCE;
	}

	public static AggregateSupport forTiDB(Dialect dialect) {
		return JSON_WITH_UUID_INSTANCE;
	}

	public static AggregateSupport forMariaDB(Dialect dialect) {
		return LONGTEXT_INSTANCE;
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
								queryExpression( aggregateParentReadExpression, columnExpression )
						);
					case BOOLEAN:
						return template.replace(
								placeholder,
								jsonType
										? "case " + queryExpression( aggregateParentReadExpression, columnExpression ) + " when cast('true' as json) then true when cast('false' as json) then false end"
										: "case " + queryExpression( aggregateParentReadExpression, columnExpression ) + " when 'true' then true when 'false' then false end"
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"unhex(json_unquote(" + queryExpression( aggregateParentReadExpression, columnExpression ) + "))"
						);
					case UUID:
						if ( column.getJdbcMapping().getJdbcType().isBinary() ) {
							if ( uuidFunctions ) {
								return template.replace(
										placeholder,
										"uuid_to_bin(json_unquote(" + queryExpression( aggregateParentReadExpression, columnExpression ) + "))"
								);
							}
							else {
								return template.replace(
										placeholder,
										"unhex(replace(json_unquote(" + queryExpression( aggregateParentReadExpression,
												columnExpression ) + "),'-',''))"
								);
							}
						}
						// Fall-through intended
					default:
						return template.replace(
								placeholder,
								valueExpression( aggregateParentReadExpression, columnExpression, columnCastType( column ) )
						);
				}
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private String columnCastType(SqlTypedMapping column) {
		return switch (column.getJdbcMapping().getJdbcType().getDdlTypeCode()) {
			// special case for casting to Boolean
			case BOOLEAN, BIT -> "unsigned";
			// MySQL doesn't let you cast to INTEGER/BIGINT/TINYINT
			case TINYINT, SMALLINT, INTEGER, BIGINT -> "signed";
			case REAL -> "float";
			case DOUBLE -> "double";
			case FLOAT -> jsonType
					// In newer versions of MySQL, casting to float/double is supported
					? column.getColumnDefinition()
					: column.getPrecision() == null || column.getPrecision() == 53 ? "double" : "float";
			// MySQL doesn't let you cast to TEXT/LONGTEXT
			case CHAR, VARCHAR, LONG32VARCHAR, CLOB, ENUM -> "char";
			case NCHAR, NVARCHAR, LONG32NVARCHAR, NCLOB -> "char character set utf8mb4";
			// MySQL doesn't let you cast to BLOB/TINYBLOB/LONGBLOB
			case BINARY, VARBINARY, LONG32VARBINARY, BLOB -> "binary";
			default -> column.getColumnDefinition();
		};
	}

	private String valueExpression(String aggregateParentReadExpression, String columnExpression, String columnType) {
		return "cast(json_unquote(" + queryExpression( aggregateParentReadExpression, columnExpression ) + ") as " + columnType + ')';
	}

	private String queryExpression(String aggregateParentReadExpression, String columnExpression) {
		if ( jsonType ) {
			return "nullif(json_extract(" + aggregateParentReadExpression + ",'$." + columnExpression + "'),cast('null' as json))";
		}
		else {
			return "nullif(json_extract(" + aggregateParentReadExpression + ",'$." + columnExpression + "'),'null')";
		}
	}

	private String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				// We encode binary data as hex
				return "hex(" + customWriteExpression + ")";
			case BOOLEAN:
				return "(" + customWriteExpression + ")=true";
			case TIMESTAMP:
				return "date_format(" + customWriteExpression + ",'%Y-%m-%dT%T.%f')";
			case TIMESTAMP_UTC:
				return "date_format(" + customWriteExpression + ",'%Y-%m-%dT%T.%fZ')";
			case UUID:
				if ( jdbcMapping.getJdbcType().isBinary() ) {
					if ( uuidFunctions ) {
						return "bin_to_uuid(" + customWriteExpression + ")";
					}
					else {
						return "regexp_replace(lower(hex(" + customWriteExpression + ")),'^(.{8})(.{4})(.{4})(.{4})(.{12})$','$1-$2-$3-$4-$5')";
					}
				}
				// Fall-through intended
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
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		switch ( aggregateSqlTypeCode ) {
			case JSON:
				return true;
		}
		return false;
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
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}
	private class AggregateJsonWriteExpression implements JsonWriteExpression {
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
			for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final JsonWriteExpression value = entry.getValue();
				final String subPath = queryExpression( path, column );
				sb.append( ',' );
				if ( value instanceof AggregateJsonWriteExpression ) {
					sb.append( "'$." );
					sb.append( column );
					sb.append( "',json_set(coalesce(" );
					sb.append( subPath );
					sb.append( ",json_object())" );
					value.append( sb, subPath, translator, expression );
					sb.append( ')' );
				}
				else {
					value.append( sb, subPath, translator, expression );
				}
			}
		}
	}

	private class RootJsonWriteExpression extends AggregateJsonWriteExpression
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
			sqlAppender.appendSql( "json_set(" );
			if ( nullable ) {
				sqlAppender.append( "coalesce(" );
				sqlAppender.append( basePath );
				sqlAppender.append( ",json_object())" );
			}
			else {
				sqlAppender.append( basePath );
			}
			append( sqlAppender, basePath, translator, aggregateColumnWriteExpression );
			sqlAppender.append( ')' );
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
			sb.append( "'$." );
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

}

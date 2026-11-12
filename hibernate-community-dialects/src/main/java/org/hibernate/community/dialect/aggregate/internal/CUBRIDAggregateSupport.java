package org.hibernate.community.dialect.aggregate.internal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.aggregate.spi.AggregateColumnWriteExpression;
import org.hibernate.dialect.aggregate.spi.AggregateComponentAssignmentRequest;
import org.hibernate.dialect.aggregate.spi.AggregateComponentReadRequest;
import org.hibernate.dialect.aggregate.spi.AggregateWriteExpressionRenderer;
import org.hibernate.dialect.aggregate.spi.AggregateWriteRendererRequest;
import org.hibernate.dialect.aggregate.spi.StandardAggregateSupport;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.spi.StringBuilderSqlAppender;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.dialect.function.array.DdlTypeHelper.getCastTypeName;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DURATION;
import static org.hibernate.type.SqlTypes.ENUM;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;

/// CUBRID aggregate support for embeddables stored as `json`.
public final class CUBRIDAggregateSupport extends StandardAggregateSupport {

	public static final CUBRIDAggregateSupport INSTANCE = new CUBRIDAggregateSupport();

	private CUBRIDAggregateSupport() {
	}

	@Override
	public String aggregateComponentCustomReadExpression(AggregateComponentReadRequest request) {
		switch ( request.aggregateColumnTypeCode() ) {
			case JSON_ARRAY:
			case JSON:
				final String parent = request.aggregateParentReadExpression();
				final String column = request.columnExpression();
				final String expression = switch ( request.column().getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case JSON, JSON_ARRAY -> queryExpression( parent, column );
					case BOOLEAN -> "case " + queryExpression( parent, column )
							+ " when cast('true' as json) then true when cast('false' as json) then false end";
					// binary data is written as hex
					case BINARY, VARBINARY, LONG32VARBINARY ->
							"cast(json_unquote(" + queryExpression( parent, column ) + ") as bit varying)";
					// casting text with a 'T' separator to datetime silently drops the time
					case TIMESTAMP, TIMESTAMP_UTC -> "cast(replace(json_unquote(" + queryExpression( parent, column )
							+ "),'T',' ') as " + getCastTypeName( request.column(), request.typeConfiguration() ) + ')';
					default -> "cast(json_unquote(" + queryExpression( parent, column ) + ") as "
							+ columnCastType( request.column(), request.typeConfiguration() ) + ')';
				};
				return request.template().replace( request.placeholder(), expression );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + request.aggregateColumnTypeCode() );
	}

	private static String columnCastType(SqlTypedMapping column, TypeConfiguration typeConfiguration) {
		return switch ( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) {
			// a cast to char keeps only the first character
			case CHAR, NCHAR, VARCHAR, NVARCHAR, LONG32VARCHAR, LONG32NVARCHAR, CLOB, NCLOB, ENUM -> "varchar";
			default -> getCastTypeName( column, typeConfiguration );
		};
	}

	private static String queryExpression(String aggregateParentReadExpression, String columnExpression) {
		return "nullif(json_extract(" + aggregateParentReadExpression + ",'$." + columnExpression + "'),cast('null' as json))";
	}

	/// Renders a value so that CUBRID's JSON functions accept it.
	public void appendJsonWriteExpression(SqlAppender sqlAppender, Runnable renderFunction, JdbcMapping jdbcMapping) {
		// CUBRID's JSON functions reject temporal, LOB and binary values, and true is just 1
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case BINARY, VARBINARY, LONG32VARBINARY -> {
				sqlAppender.appendSql( "hex(" );
				renderFunction.run();
				sqlAppender.appendSql( ')' );
			}
			case BOOLEAN -> {
				sqlAppender.appendSql( "case (" );
				renderFunction.run();
				sqlAppender.appendSql( ") when true then cast('true' as json) when false then cast('false' as json) end" );
			}
			case DATE -> {
				sqlAppender.appendSql( "date_format(" );
				renderFunction.run();
				sqlAppender.appendSql( ",'%Y-%m-%d')" );
			}
			case TIME -> {
				sqlAppender.appendSql( "time_format(" );
				renderFunction.run();
				sqlAppender.appendSql( ",'%H:%i:%s')" );
			}
			case TIMESTAMP -> {
				sqlAppender.appendSql( "date_format(" );
				renderFunction.run();
				sqlAppender.appendSql( ",'%Y-%m-%dT%T.%f')" );
			}
			case TIMESTAMP_UTC -> {
				sqlAppender.appendSql( "date_format(" );
				renderFunction.run();
				sqlAppender.appendSql( ",'%Y-%m-%dT%T.%fZ')" );
			}
			case CLOB, NCLOB -> {
				sqlAppender.appendSql( "clob_to_char(" );
				renderFunction.run();
				sqlAppender.appendSql( ')' );
			}
			// a numeric becomes a double in JSON, so parse its text to keep integers exact
			case NUMERIC, DECIMAL, DURATION -> {
				sqlAppender.appendSql( "cast(cast(" );
				renderFunction.run();
				sqlAppender.appendSql( " as varchar) as json)" );
			}
			default -> renderFunction.run();
		}
	}

	private String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final StringBuilderSqlAppender sqlAppender = new StringBuilderSqlAppender();
		appendJsonWriteExpression( sqlAppender, () -> sqlAppender.appendSql( customWriteExpression ), jdbcMapping );
		return sqlAppender.toString();
	}

	@Override
	public String aggregateComponentAssignmentExpression(AggregateComponentAssignmentRequest request) {
		switch ( request.aggregateColumnTypeCode() ) {
			case JSON:
			case JSON_ARRAY:
				// For JSON we always have to replace the whole object
				return request.aggregateParentAssignmentExpression();
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + request.aggregateColumnTypeCode() );
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int code) {
		return code == JSON;
	}

	@Override
	public AggregateWriteExpressionRenderer aggregateCustomWriteExpressionRenderer(AggregateWriteRendererRequest request) {
		final int aggregateSqlTypeCode = request.aggregateColumn().getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		if ( aggregateSqlTypeCode == JSON ) {
			return new RootJsonWriteExpression( request.aggregateColumn(), request.columnsToUpdate() );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
	}

	private interface JsonWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}

	private class AggregateJsonWriteExpression implements JsonWriteExpression {
		private final LinkedHashMap<String, JsonWriteExpression> subExpressions = new LinkedHashMap<>();

		protected void initializeSubExpressions(List<SelectableMapping> columns) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath[] parts = column.getSelectablePath().getParts();
				AggregateJsonWriteExpression currentAggregate = this;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					currentAggregate = (AggregateJsonWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new AggregateJsonWriteExpression()
					);
				}
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new BasicJsonWriteExpression(
								column,
								jsonCustomWriteExpression( column.getWriteExpression(), column.getJdbcMapping() )
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
			implements AggregateWriteExpressionRenderer {
		private final boolean nullable;
		private final String path;

		RootJsonWriteExpression(SelectableMapping aggregateColumn, List<SelectableMapping> columns) {
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
			final String basePath = qualifier == null || qualifier.isBlank() ? path : qualifier + "." + path;
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

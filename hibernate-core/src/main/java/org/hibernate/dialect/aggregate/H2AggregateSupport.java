/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;
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
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.VARBINARY;

public class H2AggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport INSTANCE = new H2AggregateSupport();

	public static @Nullable AggregateSupport valueOf(Dialect dialect) {
		return dialect.getVersion().isSameOrAfter( 2, 2, 220 )
				? H2AggregateSupport.INSTANCE
				: AggregateSupportImpl.INSTANCE;
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
								"(" + aggregateParentReadExpression + ").\"" + columnExpression + "\""
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								hexDecodeExpression( queryExpression( "(" + aggregateParentReadExpression + ").\"" + columnExpression + "\"" ), column.getColumnDefinition() )
						);
					case ARRAY:
						final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) column.getJdbcMapping();
						final String elementTypeName = getElementTypeName( column.getColumnDefinition() );
						switch ( pluralType.getElementType().getJdbcType().getDefaultSqlTypeCode() ) {
							case BINARY:
							case VARBINARY:
							case LONG32VARBINARY:
								// We encode binary data as hex, so we have to decode here
								return template.replace(
										placeholder,
										"(select array_agg(" + hexDecodeExpression( queryExpression( "(" + aggregateParentReadExpression + ").\"" + columnExpression + "\"[i.x]" ), elementTypeName ) + ") from system_range(1,10000) i where i.x<=coalesce(array_length((" + aggregateParentReadExpression + ").\"" + columnExpression + "\"),0))"
								);
							default:
								return template.replace(
										placeholder,
										"(select array_agg(" + valueExpression( "(" + aggregateParentReadExpression + ").\"" + columnExpression + "\"[i.x]", elementTypeName ) + ") from system_range(1,10000) i where i.x<=coalesce(array_length((" + aggregateParentReadExpression + ").\"" + columnExpression + "\"),0))"
								);
						}
					default:
						return template.replace(
								placeholder,
								columnExpression( aggregateParentReadExpression, columnExpression, column.getColumnDefinition() )
						);
				}
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static String getElementTypeName(String arrayTypeName) {
		final String elementTypeName = arrayTypeName.substring( 0, arrayTypeName.lastIndexOf( " array" ) );
		// Doing array_agg on clob produces funky results
		return elementTypeName.equals( "clob" ) ? "varchar" : elementTypeName;
	}

	private static String columnExpression(String aggregateParentReadExpression, String columnExpression, String columnType) {
		return valueExpression( "(" + aggregateParentReadExpression + ").\"" + columnExpression + "\"", columnType );
	}

	private static String hexDecodeExpression(String valueExpression, String columnType) {
		return "cast(hextoraw(regexp_replace(" + valueExpression + ",'([0-9a-f][0-9a-f])','00$1')) as " + columnType + ")";
	}

	private static String valueExpression(String valueExpression, String columnType) {
		return "cast(" + queryExpression( valueExpression ) + " as " + columnType + ')';
	}

	private static String queryExpression(String valueExpression) {
		// First we produce a SQL null if we see a JSON null
		// Next, we replace quotes that surround the value
		// Finally, we undo escaping that was done to a string
		return "stringdecode(regexp_replace(nullif(" + valueExpression + ",JSON'null'),'^\"(.*)\"$','$1'))";
	}

	private static String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				// We encode binary data as hex
				return "rawtohex(" + customWriteExpression + ")";
			case ARRAY:
				final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) jdbcMapping;
				switch ( pluralType.getElementType().getJdbcType().getDefaultSqlTypeCode() ) {
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
						// We encode binary data as hex
						return "(select array_agg(rawtohex(t.c1)) from unnest(" + customWriteExpression + ") t)";
					default:
						return customWriteExpression;
				}
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
	private static class AggregateJsonWriteExpression implements JsonWriteExpression {
		private final LinkedHashMap<String, JsonWriteExpression> subExpressions = new LinkedHashMap<>();

		protected void initializeSubExpressions(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
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
			sb.append( "json_object" );
			char separator = '(';
			for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final JsonWriteExpression value = entry.getValue();
				final String subPath = path + ".\"" + column + "\"";
				sb.append( separator );
				if ( value instanceof AggregateJsonWriteExpression ) {
					sb.append( '\'' );
					sb.append( column );
					sb.append( "':coalesce(" );
					value.append( sb, subPath, translator, expression );
					sb.append( ",json_object())" );
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
		private final String path;

		RootJsonWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			this.path = aggregateColumn.getSelectionExpression();
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
			append( sqlAppender, "(" + basePath + ")", translator, aggregateColumnWriteExpression );
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
			sb.append( "':" );
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
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( '\'' );
			sb.append( selectableMapping.getSelectableName() );
			sb.append( "':" );
			sb.append( path );
		}
	}

}

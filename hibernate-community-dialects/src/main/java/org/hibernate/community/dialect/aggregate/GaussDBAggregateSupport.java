/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect.aggregate;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.AggregateSupport.AggregateColumnWriteExpression;
import org.hibernate.dialect.aggregate.AggregateSupport.WriteExpressionRenderer;
import org.hibernate.dialect.aggregate.PostgreSQLAggregateSupport;
import org.hibernate.internal.util.StringHelper;
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

import static org.hibernate.dialect.function.array.DdlTypeHelper.getCastTypeName;
import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INSTANT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.LOCAL_DATE;
import static org.hibernate.type.SqlTypes.LOCAL_DATE_TIME;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.OFFSET_DATE_TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.ZONED_DATE_TIME;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

/**
 * Aggregate support for {@link org.hibernate.community.dialect.GaussDBDialect}.
 * <p>
 * GaussDB in MySQL-compatibility (M) mode does not support the {@code jsonb} type at all
 * (creating a {@code jsonb} table fails with a syntax error), so the dialect uses the
 * {@code json} type instead. The PostgreSQL {@code ->}/{@code ->>} operators <em>do</em>
 * work on {@code json} in M mode and behave like PostgreSQL's (they return SQL NULL for a
 * JSON null value, whereas {@code JSON_UNQUOTE(JSON_EXTRACT(...))} returns the literal
 * string {@code "null"}), so the scalar/JSON read expressions reuse them unchanged. What
 * M mode lacks is the {@code jsonb || jsonb} concatenation operator ({@code ||} is logical
 * OR in M mode), so only the write side needs rewriting:
 * <ul>
 *   <li>write: {@code json_set(json, '$.path', value, ...)} instead of
 *       {@code jsonb || jsonb_build_object(...)}. {@code json_set} has the same overwrite
 *       semantics as the PostgreSQL {@code ||} operator (right side wins, other keys
 *       preserved). Values are passed raw (not wrapped in {@code to_jsonb}), because
 *       {@code json_set} converts them to JSON itself and would double-quote a
 *       {@code jsonb} argument.</li>
 * </ul>
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on PostgreSQLAggregateSupport.
 */
public class GaussDBAggregateSupport extends PostgreSQLAggregateSupport {

	private static final AggregateSupport INSTANCE = new GaussDBAggregateSupport();

	// XML aggregate rendering. GaussDB M mode's XMLTABLE requires the PASSING argument to be
	// `xmltype`, but the aggregate column is `xml` and `cast(xml as xmltype)` is rejected, so the
	// passing column is routed through text: `cast(cast(<col> as text) as xmltype)` (see
	// xmlExtractArguments and GaussDBRootXmlWriteExpression). The COLUMNS type declarations
	// (v xml / v text / scalars) are accepted unchanged in M mode.
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
						// `->` works on json in M mode and returns SQL NULL for a JSON null,
						// unlike JSON_EXTRACT which returns a json null value. Parenthesize the
						// extraction: M mode binds `is null` tighter than `->`, so without parens
						// a sub-aggregate null check `parent->'col' is null` parses as
						// `parent->('col' is null)` (type json) and is rejected in CHECK constraints.
						return template.replace(
								placeholder,
								"(" + aggregateParentReadExpression + "->'" + columnExpression + "')"
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
								return template.replace(
										placeholder,
										"cast(array(select jsonb_array_elements(JSON_EXTRACT(" + aggregateParentReadExpression + ",'$." + columnExpression + "'))) as " + getCastTypeName( column, typeConfiguration ) + ')'
								);
							case BINARY:
							case VARBINARY:
							case LONG32VARBINARY:
								// We encode binary data as hex, so we have to decode here
								return template.replace(
										placeholder,
										"array(select decode(jsonb_array_elements_text(JSON_EXTRACT(" + aggregateParentReadExpression + ",'$." + columnExpression + "')),'hex'))"
								);
							default:
								return template.replace(
										placeholder,
										"cast(array(select jsonb_array_elements_text(JSON_EXTRACT(" + aggregateParentReadExpression + ",'$." + columnExpression + "'))) as " + getCastTypeName( column, typeConfiguration ) + ')'
								);
						}
					default:
						// `->>` returns SQL NULL for a JSON null value, matching PostgreSQL.
						// JSON_UNQUOTE(JSON_EXTRACT(...)) instead returns the literal string
						// "null", which breaks CHECK constraints (cast to integer errors out)
						// and reads (a null field read back as the string "null").
						if ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() == DATE ) {
							// M mode `cast(text as date)` rejects ISO dates ("invalid value ... for MON"),
							// but `cast(text as timestamp)` accepts them; cast via timestamp, then date.
							// Still SQL-NULL-safe: cast(null as timestamp) -> null -> cast(null as date) -> null.
							return template.replace(
									placeholder,
									"cast(cast(" + aggregateParentReadExpression + "->>'" + columnExpression + "' as timestamp) as " + getCastTypeName( column, typeConfiguration ) + ')'
							);
						}
						return template.replace(
								placeholder,
								"cast(" + aggregateParentReadExpression + "->>'" + columnExpression + "' as " + getCastTypeName( column, typeConfiguration ) + ')'
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
						throw new UnsupportedOperationException( "Transforming XML_ARRAY to native arrays is not supported!" );
					default:
						return template.replace(
								placeholder,
								"(select t.v from xmltable(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression ) + " columns v " + getCastTypeName( column, typeConfiguration ) + " path '.') t)"
						);
				}
		}
		// XML_ARRAY, SQLXML and STRUCT reuse the PostgreSQL implementation unchanged
		return super.aggregateComponentCustomReadExpression(
				template,
				placeholder,
				aggregateParentReadExpression,
				columnExpression,
				aggregateColumnTypeCode,
				column,
				typeConfiguration
		);
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		if ( aggregateSqlTypeCode == JSON ) {
			return new GaussDBRootJsonWriteExpression( aggregateColumn, columnsToUpdate );
		}
		if ( aggregateSqlTypeCode == SQLXML ) {
			return new GaussDBRootXmlWriteExpression( aggregateColumn, columnsToUpdate );
		}
		// XML and other aggregate types reuse the PostgreSQL implementation unchanged
		return super.aggregateCustomWriteExpressionRenderer( aggregateColumn, columnsToUpdate, typeConfiguration );
	}

	private static String xmlExtractArguments(String aggregateParentReadExpression, String xpathFragment) {
		final String extractArguments;
		int separatorIndex;
		if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_START )
			&& aggregateParentReadExpression.endsWith( XML_EXTRACT_END )
			&& (separatorIndex = aggregateParentReadExpression.indexOf( XML_EXTRACT_SEPARATOR )) != -1 ) {
			final var sb = new StringBuilder( aggregateParentReadExpression.length() - XML_EXTRACT_START.length() + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, XML_EXTRACT_START.length(), separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex + 2, aggregateParentReadExpression.length() - XML_EXTRACT_END.length() );
			extractArguments = sb.toString();
		}
		else if ( aggregateParentReadExpression.startsWith( XML_QUERY_START )
				&& aggregateParentReadExpression.endsWith( XML_QUERY_END )
				&& (separatorIndex = aggregateParentReadExpression.indexOf( XML_QUERY_SEPARATOR )) != -1 ) {
			final var sb = new StringBuilder( aggregateParentReadExpression.length() - XML_QUERY_START.length() + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, XML_QUERY_START.length(), separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex, aggregateParentReadExpression.length() - XML_QUERY_END.length() );
			extractArguments = sb.toString();
		}
		else {
			// Top-level: the parent is the bare `xml` aggregate column. M mode XMLTABLE requires
			// `xmltype`; cast(xml as xmltype) is rejected, so route through text.
			extractArguments = "'/" + XmlHelper.ROOT_TAG + "/" + xpathFragment + "' passing cast(cast(" + aggregateParentReadExpression + " as text) as xmltype)";
		}
		return extractArguments;
	}

	private static String xmlCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				// We encode binary data as hex
				return "encode(" + customWriteExpression + ",'hex')";
			default:
				return customWriteExpression;
		}
	}

	private static String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				// We encode binary data as hex; json_set embeds the hex string as a JSON string
				return "encode(" + customWriteExpression + ",'hex')";
			case DATE:
			case LOCAL_DATE:
				// M mode serializes a date as "2000-01-01 00:00:00 AD" (with a time component and era),
				// which JdbcDateJavaType.fromEncodedString can't parse when the whole JSON document is
				// read back (e.g. after an UPDATE via json_set). Emit a clean ISO date string instead,
				// matching what the persist path (JsonGeneratingVisitor) writes. NULL-safe: to_char(null,..)=null.
				return "to_char(" + customWriteExpression + ",'YYYY-MM-DD')";
			case TIMESTAMP:
			case LOCAL_DATE_TIME:
				// M mode separates date and time with a space; ISO_LOCAL_DATE_TIME (used by
				// JdbcTimestampJavaType / LocalDateTimeJavaType on read-back) requires a 'T'.
				// .MS preserves fractional seconds. NULL-safe.
				return "to_char(" + customWriteExpression + ",'YYYY-MM-DD\"T\"HH24:MI:SS.MS')";
			case TIMESTAMP_WITH_TIMEZONE:
			case TIMESTAMP_UTC:
			case INSTANT:
			case OFFSET_DATE_TIME:
			case ZONED_DATE_TIME:
				// M mode serializes a timestamptz as "2000-01-01 00:00:00+00" (space separator,
				// colon-less offset, expressed in the session timezone), which the ISO formatters used
				// on read-back (OffsetDateTime/Instant/ZonedDateTime) can't parse. Normalize to UTC and
				// emit a trailing "Z". The "Z" is embedded in the to_char format rather than concatenated
				// with ||, because || is logical-OR (not string concat) in M mode. NULL-safe.
				return "to_char(" + customWriteExpression + " at time zone 'UTC','YYYY-MM-DD\"T\"HH24:MI:SS\"Z\"')";
			default:
				// Raw value: json_set converts it to JSON itself. Do not wrap in to_jsonb,
				// because json_set would double-quote a jsonb argument.
				return customWriteExpression;
		}
	}

	interface GaussDBJsonWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}

	private static class GaussDBAggregateJsonWriteExpression implements GaussDBJsonWriteExpression {
		private final LinkedHashMap<String, GaussDBJsonWriteExpression> subExpressions = new LinkedHashMap<>();

		protected void initializeSubExpressions(SelectableMapping[] columns) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath selectablePath = column.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				GaussDBAggregateJsonWriteExpression currentAggregate = this;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					currentAggregate = (GaussDBAggregateJsonWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new GaussDBAggregateJsonWriteExpression()
					);
				}
				final String customWriteExpression = column.getWriteExpression();
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new GaussDBBasicJsonWriteExpression(
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
			// Emit `json_set` path-value pairs: `, '$.column', value`.
			// The caller (`render` or a parent aggregate) opens the `json_set(...)`
			// and supplies the target expression; we only append the path-value pairs.
			for ( Map.Entry<String, GaussDBJsonWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final GaussDBJsonWriteExpression value = entry.getValue();
				sb.append( ", '$." );
				sb.append( column );
				sb.append( "'," );
				if ( value instanceof GaussDBAggregateJsonWriteExpression ) {
					// Nested aggregate: merge the existing sub-object with the new fields.
					// `jsonb1 || jsonb_build_object(col, ...)` becomes
					// `json_set(jsonb1, '$.col', json_set(jsonb1->'col', ...))`
					final String subPath = "JSON_EXTRACT(" + path + ",'$." + column + "')";
					sb.append( "json_set(coalesce(" );
					sb.append( subPath );
					sb.append( ",'{}')" );
					value.append( sb, subPath, translator, expression );
					sb.append( ")" );
				}
				else {
					value.append( sb, path, translator, expression );
				}
			}
		}
	}

	private static class GaussDBRootJsonWriteExpression extends GaussDBAggregateJsonWriteExpression
			implements WriteExpressionRenderer {
		private final boolean nullable;
		private final String path;

		GaussDBRootJsonWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
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
			// `coalesce(base,'{}') || jsonb_build_object(...)` becomes
			// `json_set(coalesce(base,'{}'), '$.field', value, ...)`
			sqlAppender.append( "json_set(" );
			if ( nullable ) {
				sqlAppender.append( "coalesce(" );
				sqlAppender.append( basePath );
				sqlAppender.append( ",'{}')" );
			}
			else {
				sqlAppender.append( basePath );
			}
			append( sqlAppender, basePath, translator, aggregateColumnWriteExpression );
			sqlAppender.append( ")" );
		}
	}

	private static class GaussDBBasicJsonWriteExpression implements GaussDBJsonWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String customWriteExpressionStart;
		private final String customWriteExpressionEnd;

		GaussDBBasicJsonWriteExpression(SelectableMapping selectableMapping, String customWriteExpression) {
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
			// Only the value; the `$.field` path is emitted by the parent aggregate.
			sb.append( customWriteExpressionStart );
			// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
			// since we don't know how the custom write expression looks like where this is embedded,
			// so we have to be pessimistic and avoid ambiguities
			translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
			sb.append( customWriteExpressionEnd );
		}
	}

	interface GaussDBXmlWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}

	private static class GaussDBAggregateXmlWriteExpression implements GaussDBXmlWriteExpression {

		private final SelectableMapping selectableMapping;
		private final LinkedHashMap<String, GaussDBXmlWriteExpression> subExpressions = new LinkedHashMap<>();

		private GaussDBAggregateXmlWriteExpression(SelectableMapping selectableMapping) {
			this.selectableMapping = selectableMapping;
		}

		protected void initializeSubExpressions(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath selectablePath = column.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				GaussDBAggregateXmlWriteExpression currentAggregate = this;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) currentAggregate.selectableMapping.getJdbcMapping().getJdbcType();
					final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
					final int selectableIndex = embeddableMappingType.getSelectableIndex( parts[i].getSelectableName() );
					currentAggregate = (GaussDBAggregateXmlWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new GaussDBAggregateXmlWriteExpression( embeddableMappingType.getJdbcValueSelectable( selectableIndex ) )
					);
				}
				final String customWriteExpression = column.getWriteExpression();
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new GaussDBBasicXmlWriteExpression(
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

				final GaussDBXmlWriteExpression xmlWriteExpression = subExpressions.get( selectableMapping.getSelectableName() );
				if ( xmlWriteExpression == null ) {
					subExpressions.put(
							selectableMapping.getSelectableName(),
							new GaussDBPassThroughXmlWriteExpression( selectableMapping )
					);
				}
				else if ( xmlWriteExpression instanceof GaussDBAggregateXmlWriteExpression writeExpression ) {
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
			for ( Map.Entry<String, GaussDBXmlWriteExpression> entry : subExpressions.entrySet() ) {
				sb.append( separator );

				final GaussDBXmlWriteExpression value = entry.getValue();
				if ( value instanceof GaussDBAggregateXmlWriteExpression ) {
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

	private static class GaussDBRootXmlWriteExpression extends GaussDBAggregateXmlWriteExpression
			implements WriteExpressionRenderer {
		private final String path;

		GaussDBRootXmlWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			super( aggregateColumn );
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
			// M mode XMLTABLE requires the PASSING argument to be `xmltype`; the `xml` aggregate
			// column can't be cast directly to xmltype, so route through text. Sub-aggregates and
			// pass-through fields reuse this passing via xmlExtractArguments unchanged.
			append( sqlAppender, XML_QUERY_START + "'/" + getTagName() + "' passing cast(cast(" + basePath + " as text) as xmltype)" + XML_QUERY_END, translator, aggregateColumnWriteExpression );
		}
	}

	private static class GaussDBBasicXmlWriteExpression implements GaussDBXmlWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String[] customWriteExpressionParts;

		GaussDBBasicXmlWriteExpression(SelectableMapping selectableMapping, String customWriteExpression) {
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
				// Remove the <Collection> tag to wrap the value into the selectable specific tag.
				// M mode XMLTABLE requires `xmltype`; the xml value is routed through text. A NULL array
				// value would pass NULL to XMLTABLE, which M mode rejects ("NULL is passed when the
				// detoast operation is executed"); coalesce to an empty <Collection/> (0 rows) instead.
				sb.append( "(select xmlagg(t.v order by t.i) from xmltable('/Collection/*' passing coalesce(cast(cast(" );
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
				sb.append( " as text) as xmltype),cast('<Collection/>' as xmltype)) columns v xml path '.', i for ordinality)t)" );
			}
			sb.append( ')' );
		}
	}

	private static class GaussDBPassThroughXmlWriteExpression implements GaussDBXmlWriteExpression {

		private final SelectableMapping selectableMapping;

		GaussDBPassThroughXmlWriteExpression(SelectableMapping selectableMapping) {
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

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
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.SQLXML;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

/**
 * Sybase ASE supports the {@code xmlextract()} function which is used to implement aggregate support.
 * One notable quirk of Sybase ASE that causes a lot of ugly code is that a SQL {@code NULL} in string concatenation
 * is treated as empty string and hence does not produce {@code NULL}.
 * So in order to get correct results, it is necessary to wrap concatenation expressions with case expressions,
 * that ensure {@code NULL} is produced if any of the concatenation expressions is {@code NULL}.
 */
public class SybaseASEAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport INSTANCE = new SybaseASEAggregateSupport();

	private static final String XML_EXTRACT_START = "xmlextract(";
	private static final String XML_EXTRACT_SEPARATOR = "/*',";
	private static final String XML_EXTRACT_SIMPLE_SEPARATOR = "',";
	private static final String XML_EXTRACT_END = ")";
	private static final String XML_EXTRACT_READ_START = "case when ";
	private static final String XML_EXTRACT_READ_NULL_CHECK = " is null then null else ";
	private static final String XML_EXTRACT_READ_INVOCATION_START = "'<" + XmlHelper.ROOT_TAG + ">'+xmlextract(";
	private static final String XML_EXTRACT_READ_END = " returns varchar(16384))+'</" + XmlHelper.ROOT_TAG + ">' end";

	private SybaseASEAggregateSupport() {
	}

	public static AggregateSupport valueOf(Dialect dialect) {
		return INSTANCE;
	}

	@Override
	public boolean supportsComponentCheckConstraints() {
		return false;
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
			case SQLXML:
			case XML_ARRAY:
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
					case BLOB:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"strtobin(xmlextract(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + " returns varchar(16384)))"
						);
					case DATE:
					case TIME:
					case TIMESTAMP:
					case TIMESTAMP_UTC:
						// Cast from clob to varchar first
						return template.replace(
								placeholder,
								"cast(str_replace(xmlextract(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + " returns varchar(36)),'Z','') as " + column.getColumnDefinition() + ")"
						);
					case SQLXML:
						return template.replace(
								placeholder,
								xmlExtractForConcat( "'<" + XmlHelper.ROOT_TAG + ">'+", aggregateParentReadExpression, columnExpression + "/*", "+'</" + XmlHelper.ROOT_TAG + ">'" )
						);
					case XML_ARRAY:
						if ( typeConfiguration.getCurrentBaseSqlTypeIndicators().isXmlFormatMapperLegacyFormatEnabled() ) {
							throw new IllegalArgumentException( "XML array '" + columnExpression + "' in '" + aggregateParentReadExpression + "' is not supported with legacy format enabled." );
						}
						else {
							return template.replace(
									placeholder,
									xmlExtractForConcat( "'<Collection>'+", aggregateParentReadExpression, columnExpression + "/*", "+'</Collection>'" )
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
						// xmlextract() unfortunately does not resolve XML entities, so we use str_replace to do that
						return template.replace(
								placeholder,
								"cast(str_replace(str_replace(str_replace(str_replace(xmlextract(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + " returns varchar(16384)),'&lt;','<'),'&gt;','>'),'&quot;','\"'),'&amp;','&') as " + column.getColumnDefinition() + ")"
						);
					case UUID:
						if ( SqlTypes.isBinaryType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									"strtobin(str_replace(xmlextract(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + " returns varchar(36)),'-',null))"
							);
						}
						// Fall-through intended
					default:
						return template.replace(
								placeholder,
								"cast(xmlextract(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + " returns varchar(16384)) as " + column.getColumnDefinition() + ")"
						);
				}
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static String xmlExtractArguments(String aggregateParentReadExpression, String xpathFragment) {
		final String extractArguments;
		final int separatorIndex;
		final int patternIdx;
		if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_READ_START )
			&& aggregateParentReadExpression.endsWith( XML_EXTRACT_READ_END )
			&& (patternIdx = aggregateParentReadExpression.indexOf( XML_EXTRACT_READ_NULL_CHECK )) != -1
			&& aggregateParentReadExpression.regionMatches( patternIdx + XML_EXTRACT_READ_NULL_CHECK.length(),
				XML_EXTRACT_READ_INVOCATION_START, 0, XML_EXTRACT_READ_INVOCATION_START.length() )) {
			final int argumentsStartIndex = patternIdx + XML_EXTRACT_READ_NULL_CHECK.length() + XML_EXTRACT_READ_INVOCATION_START.length();
			separatorIndex = aggregateParentReadExpression.indexOf( XML_EXTRACT_SEPARATOR );
			final StringBuilder sb = new StringBuilder( aggregateParentReadExpression.length() - argumentsStartIndex + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, argumentsStartIndex, separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex + 2, aggregateParentReadExpression.length() - XML_EXTRACT_READ_END.length() );
			extractArguments = sb.toString();
		}
		else if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_START )
			&& aggregateParentReadExpression.endsWith( XML_EXTRACT_END )
			&& (separatorIndex = aggregateParentReadExpression.indexOf( XML_EXTRACT_SIMPLE_SEPARATOR )) != -1 ) {
			final StringBuilder sb = new StringBuilder( aggregateParentReadExpression.length() - XML_EXTRACT_START.length() + xpathFragment.length() );
			final int xpathEnd;
			if ( aggregateParentReadExpression.regionMatches( separatorIndex - 2, XML_EXTRACT_SEPARATOR, 0, XML_EXTRACT_SEPARATOR.length() ) ) {
				xpathEnd = separatorIndex - 2;
			}
			else {
				xpathEnd = separatorIndex;
			}
			sb.append( aggregateParentReadExpression, XML_EXTRACT_START.length(), xpathEnd );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex, aggregateParentReadExpression.length() - 1 );
			extractArguments = sb.toString();
		}
		else {
			extractArguments = "'/" + XmlHelper.ROOT_TAG + "/" + xpathFragment + "'," + aggregateParentReadExpression;
		}
		return extractArguments;
	}

	private static String xmlExtractForConcat(String prefix, String aggregateParentReadExpression, String xpathFragment, String suffix) {
		final String extractArguments;
		final int patternIdx;
		final String caseExpression;
		if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_READ_START )
			&& aggregateParentReadExpression.endsWith( XML_EXTRACT_READ_END )
			&& (patternIdx = aggregateParentReadExpression.indexOf( XML_EXTRACT_READ_NULL_CHECK )) != -1
			&& aggregateParentReadExpression.regionMatches( patternIdx + XML_EXTRACT_READ_NULL_CHECK.length(),
				XML_EXTRACT_READ_INVOCATION_START, 0, XML_EXTRACT_READ_INVOCATION_START.length() )) {
			caseExpression = aggregateParentReadExpression.substring( 0, patternIdx + XML_EXTRACT_READ_NULL_CHECK.length() );
			final int argumentsStartIndex = patternIdx + XML_EXTRACT_READ_NULL_CHECK.length() + XML_EXTRACT_READ_INVOCATION_START.length();
			final int separatorIndex = aggregateParentReadExpression.indexOf( XML_EXTRACT_SEPARATOR );
			final StringBuilder sb = new StringBuilder( aggregateParentReadExpression.length() - argumentsStartIndex + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, argumentsStartIndex, separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex + 2, aggregateParentReadExpression.length() - XML_EXTRACT_READ_END.length() );
			extractArguments = sb.toString();
		}
		else {
			caseExpression = XML_EXTRACT_READ_START + aggregateParentReadExpression + XML_EXTRACT_READ_NULL_CHECK;
			extractArguments = "'/" + XmlHelper.ROOT_TAG + "/" + xpathFragment + "'," + aggregateParentReadExpression;
		}
		return caseExpression + prefix + XML_EXTRACT_START + extractArguments + " returns varchar(16384)" + XML_EXTRACT_END + suffix + " end";
	}

	private static String customWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case UUID:
				return "stuff(stuff(stuff(stuff(lower(bintostr(" + customWriteExpression + ")),9,0,'-'),14,0,'-'),19,0,'-'),24,0,'-')";
			case BOOLEAN:
				return "case " + customWriteExpression + " when 1 then 'true' when 0 then 'false' end";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				// We encode binary data as hex
				return "bintostr(" + customWriteExpression + ")";
			case DATE:
				return "left(convert(varchar," + customWriteExpression + ",140),10)";
			case TIME:
				return "left(convert(varchar," + customWriteExpression + ",20),8)";
			case TIMESTAMP:
				return "str_replace(convert(varchar," + customWriteExpression + ",140),' ','T')";
			case TIMESTAMP_UTC:
				return "nullif(str_replace(convert(varchar," + customWriteExpression + ",140),' ','T')+'Z','Z')";
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
		// We need to know what array this is XML_ARRAY,
		// which we can easily get from the type code of the aggregate column
		final int sqlTypeCode = aggregateColumn.getType().getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode == SqlTypes.ARRAY ? aggregateColumn.getTypeCode() : sqlTypeCode ) {
			case SQLXML:
			case XML_ARRAY:
				return null;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		return aggregateSqlTypeCode == SQLXML;
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		switch ( aggregateSqlTypeCode ) {
			case SQLXML:
				return new RootXmlWriteExpression( aggregateColumn, columnsToUpdate );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
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
			sb.append( "'<" );
			sb.append( getTagName() );
			sb.append( ">'" );
			for ( Map.Entry<String, XmlWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final XmlWriteExpression value = entry.getValue();
				sb.append( "+coalesce(" );
				if ( value instanceof AggregateXmlWriteExpression ) {
					final String subPath = "xmlextract(" + xmlExtractArguments( path, column ) + ")";
					value.append( sb, subPath, translator, expression );
				}
				else {
					value.append( sb, path, translator, expression );
				}
				sb.append( ",'<" );
				sb.append( column );
				sb.append( "/>')" );
			}
			sb.append( "+'</" );
			sb.append( getTagName() );
			sb.append( ">'" );
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
		public boolean isAggregate() {
			return selectableMapping.getJdbcMapping().getJdbcType().isXml();
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			final JdbcType jdbcType = selectableMapping.getJdbcMapping().getJdbcType();
			final boolean isArray = jdbcType.getDefaultSqlTypeCode() == XML_ARRAY;
			final boolean isString = jdbcType.isStringLike();
			sb.append( "case when " );
			translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
			sb.append( " is null then null else '<" );
			sb.append( selectableMapping.getSelectableName() );
			sb.append( ">'+" );
			if ( isArray ) {
				// Remove the <Collection> tag to wrap the value into the selectable specific tag
				sb.append( "xmlextract('/Collection/*'," );
			}
			else if ( isString ) {
				// Need to escape certain characters in XML content
				sb.append( "str_replace(str_replace(str_replace(" );
				if ( jdbcType.isLobOrLong() ) {
					// str_replace doesn't work with LOBs, so need to cast here
					sb.append( "cast(" );
				}
			}
			else {
				sb.append( "cast(" );
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
				sb.append( " returns varchar(16384))" );
			}
			else if ( isString ) {
				if ( jdbcType.isLobOrLong() ) {
					sb.append( " as varchar(16384))" );
				}
				sb.append( ",'&','&amp;'),'<','&lt;'),'>','&gt;')" );
			}
			else {
				sb.append( " as varchar(16384))" );
			}
			sb.append( "+'</" );
			sb.append( selectableMapping.getSelectableName() );
			sb.append( ">' end" );
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
			sb.append( "xmlextract(" );
			sb.append( xmlExtractArguments( path, selectableMapping.getSelectableName() ) );
			// This expression is always going to be concatenated
			// Since concatenation doesn't support LOBs, ensure a varchar is returned
			sb.append( " returns varchar(16384))" );
		}
	}


}

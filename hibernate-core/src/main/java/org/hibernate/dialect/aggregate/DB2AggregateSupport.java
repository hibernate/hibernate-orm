/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.NamedAuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2StructJdbcType;
import org.hibernate.dialect.XmlHelper;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;

public class DB2AggregateSupport extends AggregateSupportImpl {

	public static final AggregateSupport INSTANCE = new DB2AggregateSupport( false );
	public static final AggregateSupport JSON_INSTANCE = new DB2AggregateSupport( true );
	private static final String JSON_QUERY_START = "json_query(";
	private static final String JSON_QUERY_JSON_END = "')";

	private final boolean jsonSupport;

	public DB2AggregateSupport(boolean jsonSupport) {
		this.jsonSupport = jsonSupport;
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
				if ( !jsonSupport ) {
					break;
				}
				final String parentPartExpression;
				if ( aggregateParentReadExpression.startsWith( JSON_QUERY_START ) && aggregateParentReadExpression.endsWith( JSON_QUERY_JSON_END ) ) {
					parentPartExpression = aggregateParentReadExpression.substring( JSON_QUERY_START.length(), aggregateParentReadExpression.length() - JSON_QUERY_JSON_END.length() ) + ".";
				}
				else {
					parentPartExpression = aggregateParentReadExpression + ",'$.";
				}
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case BOOLEAN:
						if ( SqlTypes.isNumericType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									"decode(json_value(" + parentPartExpression + columnExpression + "'),'true',1,'false',0)"
							);
						}
						else {
							return template.replace(
									placeholder,
									"decode(json_value(" + parentPartExpression + columnExpression + "'),'true',true,'false',false)"
							);
						}
					case TIMESTAMP_WITH_TIMEZONE:
					case TIMESTAMP_UTC:
						return template.replace(
								placeholder,
								"cast(trim(trailing 'Z' from json_value(" + parentPartExpression + columnExpression + "' returning varchar(35))) as " + column.getColumnDefinition() + ")"
						);
					case BINARY:
					case VARBINARY:
					case LONG32VARBINARY:
					case BLOB:
						// We encode binary data as hex, so we have to decode here
						return template.replace(
								placeholder,
								"hextoraw(json_value(" + parentPartExpression + columnExpression + "'))"
						);
					case UUID:
						return template.replace(
								placeholder,
								"hextoraw(replace(json_value(" + parentPartExpression + columnExpression + "'),'-',''))"
						);
					case JSON:
					case JSON_ARRAY:
						return template.replace(
								placeholder,
								"json_query(" + parentPartExpression + columnExpression + "')"
						);
					default:
						return template.replace(
								placeholder,
								"json_value(" + parentPartExpression + columnExpression + "' returning " + column.getColumnDefinition() + ")"
						);
				}
			case STRUCT:
				return template.replace( placeholder, aggregateParentReadExpression + ".." + columnExpression );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
			case BLOB:
				// We encode binary data as hex
				return "hex(" + customWriteExpression + ")";
			case UUID:
				return "regexp_replace(lower(hex(" + customWriteExpression + ")),'^(.{8})(.{4})(.{4})(.{4})(.{12})$','$1-$2-$3-$4-$5')";
			case ARRAY:
			case JSON_ARRAY:
				return "(" + customWriteExpression + ") format json";
//			case BOOLEAN:
//				return "(" + customWriteExpression + ")=true";
			case TIME:
				return "varchar_format(timestamp('1970-01-01'," + customWriteExpression + "),'HH24:MI:SS')";
			case TIMESTAMP:
				return "replace(varchar_format(" + customWriteExpression + ",'YYYY-MM-DD HH24:MI:SS.FF9'),' ','T')";
			case TIMESTAMP_UTC:
				return "replace(varchar_format(" + customWriteExpression + ",'YYYY-MM-DD HH24:MI:SS.FF9'),' ','T')||'Z'";
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
				if ( jsonSupport ) {
					// For JSON we always have to replace the whole object
					return aggregateParentAssignmentExpression;
				}
				break;
			case STRUCT:
				return aggregateParentAssignmentExpression + ".." + columnExpression;
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
				if ( jsonSupport ) {
					return null;
				}
				break;
			case STRUCT:
				final StringBuilder sb = new StringBuilder();
				appendStructCustomWriteExpression( aggregateColumn, aggregatedColumns, sb );
				return sb.toString();
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
	}

	private static void appendStructCustomWriteExpression(
			ColumnTypeInformation aggregateColumnType,
			List<Column> aggregatedColumns,
			StringBuilder sb) {
		sb.append( aggregateColumnType.getTypeName() ).append( "()" );
		for ( Column udtColumn : aggregatedColumns ) {
			sb.append( ".." ).append( udtColumn.getName() ).append( '(' );
			if ( udtColumn.getSqlTypeCode() == STRUCT ) {
				final AggregateColumn aggregateColumn = (AggregateColumn) udtColumn;
				appendStructCustomWriteExpression(
						aggregateColumn,
						aggregateColumn.getComponent().getAggregatedColumns(),
						sb
				);
			}
			else {
				sb.append( "cast(? as " ).append( udtColumn.getSqlType() ).append( ')' );
			}
			sb.append( ')' );
		}
	}

	@Override
	public int aggregateComponentSqlTypeCode(int aggregateColumnSqlTypeCode, int columnSqlTypeCode) {
		if ( aggregateColumnSqlTypeCode == STRUCT ) {
			// DB2 doesn't support booleans in structs
			return columnSqlTypeCode == BOOLEAN ? SMALLINT : columnSqlTypeCode;
		}
		else if ( aggregateColumnSqlTypeCode == JSON ) {
			return columnSqlTypeCode == ARRAY ? JSON_ARRAY : columnSqlTypeCode;
		}
		else {
			return columnSqlTypeCode;
		}
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		return aggregateSqlTypeCode == STRUCT || aggregateSqlTypeCode == JSON;
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		switch ( aggregateSqlTypeCode ) {
			case JSON:
				if ( jsonSupport ) {
					return jsonAggregateColumnWriter( aggregateColumn, columnsToUpdate );
				}
				break;
			case STRUCT:
				return structAggregateColumnWriter( aggregateColumn, columnsToUpdate, typeConfiguration );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
	}

	private WriteExpressionRenderer jsonAggregateColumnWriter(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columns) {
		return new RootJsonWriteExpression( aggregateColumn, columns );
	}

	private WriteExpressionRenderer structAggregateColumnWriter(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columns,
			TypeConfiguration typeConfiguration) {
		return new RootStructWriteExpression( aggregateColumn, columns, typeConfiguration );
	}

	private static String determineTypeName(SelectableMapping column, TypeConfiguration typeConfiguration) {
		final String typeName;
		if ( column.getColumnDefinition() == null ) {
			final DdlType ddlType = typeConfiguration.getDdlTypeRegistry().getDescriptor(
					column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode()
			);
			final Size size = new Size();
			size.setLength( column.getLength() );
			size.setPrecision( column.getPrecision() );
			size.setScale( column.getScale() );
			return ddlType.getCastTypeName(
					size,
					(SqlExpressible) column.getJdbcMapping(),
					typeConfiguration.getDdlTypeRegistry()
			);
		}
		else{
			typeName = column.getColumnDefinition();
		}
		return typeName;
	}

	interface AggregateWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}
	private static class AggregateStructWriteExpression implements AggregateWriteExpression {
		private final LinkedHashMap<String, AggregateWriteExpression> subExpressions = new LinkedHashMap<>();
		protected final EmbeddableMappingType embeddableMappingType;
		protected final String structTypeName;
		protected final boolean nullable;

		public AggregateStructWriteExpression(SelectableMapping selectableMapping) {
			final DB2StructJdbcType structJdbcType = (DB2StructJdbcType) selectableMapping.getJdbcMapping().getJdbcType();
			this.embeddableMappingType = structJdbcType.getEmbeddableMappingType();
			this.structTypeName = structJdbcType.getStructTypeName();
			this.nullable = selectableMapping.isNullable();
		}

		protected void initializeSubExpressions(SelectableMapping[] columns, TypeConfiguration typeConfiguration) {
			for ( SelectableMapping column : columns ) {
				final SelectablePath selectablePath = column.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				final String typeName = determineTypeName( column, typeConfiguration );
				AggregateStructWriteExpression currentAggregate = this;
				EmbeddableMappingType currentMappingType = embeddableMappingType;
				for ( int i = 1; i < parts.length - 1; i++ ) {
					final SelectableMapping selectableMapping = currentMappingType.getJdbcValueSelectable(
							currentMappingType.getSelectableIndex( parts[i].getSelectableName() )
					);
					currentAggregate = (AggregateStructWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[i].getSelectableName(),
							k -> new AggregateStructWriteExpression( selectableMapping )
					);
					currentMappingType = currentAggregate.embeddableMappingType;
				}
				final String customWriteExpression = column.getWriteExpression();
				currentAggregate.subExpressions.put(
						parts[parts.length - 1].getSelectableName(),
						new BasicStructWriteExpression(
								column,
								typeName,
								customWriteExpression
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
			if ( nullable ) {
				sb.append( "coalesce(" );
				sb.append( path );
				sb.append( "," );
				sb.append( structTypeName );
				sb.append( "())" );
			}
			else {
				sb.append( path );
			}
			for ( Map.Entry<String, AggregateWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final AggregateWriteExpression value = entry.getValue();
				sb.append( ".." );
				sb.append( column );
				sb.append( '(' );
				value.append( sb, path + ".." + column, translator, expression );
				sb.append( ')' );
			}
		}
	}

	private static class RootStructWriteExpression extends AggregateStructWriteExpression
			implements WriteExpressionRenderer {

		private final String selectableName;

		RootStructWriteExpression(
				SelectableMapping aggregateColumn,
				SelectableMapping[] columns,
				TypeConfiguration typeConfiguration) {
			super( aggregateColumn );
			this.selectableName = aggregateColumn.getSelectableName();
			initializeSubExpressions( columns, typeConfiguration );
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression aggregateColumnWriteExpression,
				String qualifier) {
			final String path;
			if ( qualifier == null || qualifier.isBlank() ) {
				path = selectableName;
			}
			else {
				path = qualifier + "." + selectableName;
			}
			append( sqlAppender, path, translator, aggregateColumnWriteExpression );
		}
	}
	private static class BasicStructWriteExpression implements AggregateWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String typeName;
		private final String customWriteExpressionStart;
		private final String customWriteExpressionEnd;

		BasicStructWriteExpression(SelectableMapping selectableMapping, String typeName, String customWriteExpression) {
			this.selectableMapping = selectableMapping;
			this.typeName = typeName;
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
			sb.append( "cast(" );
			sb.append( customWriteExpressionStart );
			translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.DEFAULT );
			sb.append( customWriteExpressionEnd );
			sb.append( " as " );
			sb.append( typeName );
			sb.append( ')' );
		}
	}

	@Override
	public boolean preferSelectAggregateMapping(int aggregateSqlTypeCode) {
		// The JDBC driver does not support selecting java.sql.Struct, so return false to select individual parts
		return aggregateSqlTypeCode != STRUCT;
	}

	@Override
	public boolean preferBindAggregateMapping(int aggregateSqlTypeCode) {
		// We bind individual parts through a special custom write expression because the JDBC driver support is bad
		return aggregateSqlTypeCode != STRUCT;
	}

	@Override
	public List<AuxiliaryDatabaseObject> aggregateAuxiliaryDatabaseObjects(
			Namespace namespace,
			String aggregatePath,
			AggregateColumn aggregateColumn,
			List<Column> aggregatedColumns) {
		if ( aggregateColumn.getTypeCode() != STRUCT ) {
			return Collections.emptyList();
		}
		final String columnType = aggregateColumn.getTypeName();
		// The serialize and deserialize functions, as well as the transform are for supporting struct types in native queries and functions
		var list = new ArrayList<AuxiliaryDatabaseObject>( 3 );
		var serializerSb = new StringBuilder();
		var deserializerSb = new StringBuilder();
		serializerSb.append( "create function " ).append( columnType ).append( "_serializer(v " ).append( columnType ).append( ") returns xml language sql " )
				.append( "return xmlelement(name \"").append( XmlHelper.ROOT_TAG ).append( "\"" );
		appendSerializer( aggregatedColumns, serializerSb, "v.." );
		serializerSb.append( ')' );

		deserializerSb.append( "create function " ).append( columnType ).append( "_deserializer(v xml) returns " ).append( columnType ).append( " language sql " )
				.append( "return select " ).append( columnType ).append( "()" );
		appendDeserializerConstructor( aggregatedColumns, deserializerSb, "" );
		deserializerSb.append( " from xmltable('$" ).append( XmlHelper.ROOT_TAG ).append( "' passing v as \"" )
				.append( XmlHelper.ROOT_TAG ).append( "\" columns" );
		appendDeserializerColumns( aggregatedColumns, deserializerSb, ' ', "" );
		deserializerSb.append( ") as t" );
		list.add(
				new NamedAuxiliaryDatabaseObject(
						"DB2 " + columnType + " serializer",
						namespace,
						serializerSb.toString(),
						"drop function " + columnType + "_serializer",
						Set.of( DB2Dialect.class.getName() )
				)
		);
		list.add(
				new NamedAuxiliaryDatabaseObject(
						"DB2 " + columnType + " deserializer",
						namespace,
						deserializerSb.toString(),
						"drop function " + columnType + "_deserializer",
						Set.of( DB2Dialect.class.getName() )
				)
		);
		list.add(
				new NamedAuxiliaryDatabaseObject(
						"DB2 " + columnType + " transform",
						namespace,
						"create transform for " + columnType + " db2_program (from sql with function " + columnType + "_serializer, to sql with function " + columnType + "_deserializer)",
						"drop transform db2_program for " + columnType,
						Set.of( DB2Dialect.class.getName() )
				)
		);
		return list;
	}

	private static void appendSerializer(List<Column> aggregatedColumns, StringBuilder serializerSb, String prefix) {
		char sep;
		if ( aggregatedColumns.size() > 1 ) {
			serializerSb.append( ",xmlconcat" );
			sep = '(';
		}
		else {
			sep = ',';
		}
		for ( Column udtColumn : aggregatedColumns ) {
			serializerSb.append( sep );
			serializerSb.append( "xmlelement(name \"" ).append( udtColumn.getName() ).append( "\"" );
			if ( udtColumn.getSqlTypeCode() == STRUCT ) {
				final AggregateColumn aggregateColumn = (AggregateColumn) udtColumn;
				appendSerializer(
						aggregateColumn.getComponent().getAggregatedColumns(),
						serializerSb,
						prefix + udtColumn.getName() + ".."
				);
			}
			else if ( needsVarcharForBitDataCast( udtColumn.getSqlType() ) ) {
				serializerSb.append( ",cast(" ).append( prefix ).append( udtColumn.getName() ).append( " as varchar(" )
						.append( udtColumn.getColumnSize( null, null ).getLength() ).append( ") for bit data)" );
			}
			else {
				serializerSb.append( ',' ).append( prefix ).append( udtColumn.getName() );
			}
			serializerSb.append( ')' );
			sep = ',';
		}
		if ( aggregatedColumns.size() > 1 ) {
			serializerSb.append( ')' );
		}
	}

	private static void appendDeserializerConstructor(
			List<Column> aggregatedColumns,
			StringBuilder deserializerSb,
			String prefix) {
		for ( Column udtColumn : aggregatedColumns ) {
			deserializerSb.append( ".." ).append( udtColumn.getName() ).append( '(' );
			if ( udtColumn.getSqlTypeCode() == STRUCT ) {
				final AggregateColumn aggregateColumn = (AggregateColumn) udtColumn;
				deserializerSb.append( udtColumn.getSqlType() ).append( "()" );
				appendDeserializerConstructor(
						aggregateColumn.getComponent().getAggregatedColumns(),
						deserializerSb,
						udtColumn.getName() + "_"
				);
				deserializerSb.append( ')' );
			}
			else if ( needsVarcharForBitDataCast( udtColumn.getSqlType() ) ) {
				deserializerSb.append( "cast(t." ).append( prefix ).append( udtColumn.getName() ).append( " as " )
						.append( udtColumn.getSqlType() ).append( "))" );
			}
			else {
				deserializerSb.append( "t." ).append( prefix ).append( udtColumn.getName() ).append( ')' );
			}
		}
	}

	private static void appendDeserializerColumns(
			List<Column> aggregatedColumns,
			StringBuilder deserializerSb,
			char sep,
			String prefix) {
		for ( Column udtColumn : aggregatedColumns ) {
			if ( udtColumn.getSqlTypeCode() == STRUCT ) {
				final AggregateColumn aggregateColumn = (AggregateColumn) udtColumn;
				appendDeserializerColumns(
						aggregateColumn.getComponent().getAggregatedColumns(),
						deserializerSb,
						sep,
						udtColumn.getName() + "_"
				);
			}
			else {
				deserializerSb.append( sep );
				deserializerSb.append( prefix ).append( udtColumn.getName() ).append( ' ' );
				if ( needsVarcharForBitDataCast( udtColumn.getSqlType() ) ) {
					deserializerSb.append( "varchar(" )
							.append( udtColumn.getColumnSize( null, null ).getLength() ).append( ") for bit data" );
				}
				else {
					deserializerSb.append( udtColumn.getSqlType() );
				}
				deserializerSb.append( " path '/" ).append( XmlHelper.ROOT_TAG ).append( '/' ).append( udtColumn.getName() ).append( '\'' );
			}
			sep = ',';
		}
	}

	private static boolean needsVarcharForBitDataCast(String columnType) {
		// xmlelement and xmltable don't seem to support the "varbinary", "binary" or "char for bit data" types
		final String columTypeLC = columnType.toLowerCase( Locale.ROOT ).trim();
		return columTypeLC.contains( "binary" )
				|| columTypeLC.startsWith( "char" ) && columTypeLC.endsWith( " bit data" );
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
				final String subPath = "json_query(" + path + ",'$." + column + "') format json";
				sb.append( separator );
				if ( value instanceof AggregateJsonWriteExpression ) {
					sb.append( '\'' );
					sb.append( column );
					sb.append( "' value coalesce(" );
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
			sb.append( "' value " );
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
			sb.append( "' value " );
			sb.append( path );
		}
	}

}

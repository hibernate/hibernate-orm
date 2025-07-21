/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.tool.schema.extract.spi.ColumnTypeInformation;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.STRUCT;

public class DB2AggregateSupport extends AggregateSupportImpl {

	public static final AggregateSupport INSTANCE = new DB2AggregateSupport();

	@Override
	public String aggregateComponentCustomReadExpression(
			String template,
			String placeholder,
			String aggregateParentReadExpression,
			String columnExpression,
			AggregateColumn aggregateColumn,
			Column column) {
		switch ( aggregateColumn.getTypeCode() ) {
			case STRUCT:
				return template.replace( placeholder, aggregateParentReadExpression + ".." + columnExpression );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
	}

	@Override
	public String aggregateComponentAssignmentExpression(
			String aggregateParentAssignmentExpression,
			String columnExpression,
			AggregateColumn aggregateColumn,
			Column column) {
		switch ( aggregateColumn.getTypeCode() ) {
			case STRUCT:
				return aggregateParentAssignmentExpression + ".." + columnExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
	}

	@Override
	public String aggregateCustomWriteExpression(
			AggregateColumn aggregateColumn,
			List<Column> aggregatedColumns) {
		switch ( aggregateColumn.getTypeCode() ) {
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
		if ( aggregateColumnSqlTypeCode == STRUCT && columnSqlTypeCode == BOOLEAN ) {
			// DB2 doesn't support booleans in structs
			return SMALLINT;
		}
		return columnSqlTypeCode;
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		return aggregateSqlTypeCode == STRUCT;
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		switch ( aggregateSqlTypeCode ) {
			case STRUCT:
				return structAggregateColumnWriter( aggregateColumn, columnsToUpdate, typeConfiguration );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
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
				final String[] parts = customWriteExpression.split( "\\?" );
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
				.append( "return case when v is null then null else xmlelement(name \"").append( XmlHelper.ROOT_TAG ).append( "\"" );
		appendSerializer( aggregatedColumns, serializerSb, "v.." );
		serializerSb.append( ") end" );

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
			if ( udtColumn.getSqlTypeCode() == STRUCT ) {
				serializerSb.append( "case when ").append( prefix ).append( udtColumn.getName() )
						.append( " is null then null else " );
			}
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
			if ( udtColumn.getSqlTypeCode() == STRUCT ) {
				serializerSb.append( " end" );
			}
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

}

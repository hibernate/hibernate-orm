/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.aggregate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleArrayJdbcType;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.AbstractSqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.StructJdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.STRUCT;
import static org.hibernate.type.SqlTypes.STRUCT_ARRAY;
import static org.hibernate.type.SqlTypes.STRUCT_TABLE;
import static org.hibernate.type.SqlTypes.TABLE;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;

public class OracleAggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport V23_INSTANCE = new OracleAggregateSupport( true, JsonSupport.OSON );
	private static final AggregateSupport V21_INSTANCE = new OracleAggregateSupport( false, JsonSupport.OSON );
	private static final AggregateSupport V19_INSTANCE = new OracleAggregateSupport( false, JsonSupport.MERGEPATCH );
	private static final AggregateSupport V18_INSTANCE = new OracleAggregateSupport( false, JsonSupport.QUERY_AND_PATH );
	private static final AggregateSupport V12_INSTANCE = new OracleAggregateSupport( false, JsonSupport.QUERY );
	private static final AggregateSupport LEGACY_INSTANCE = new OracleAggregateSupport( false, JsonSupport.NONE );

	private static final String JSON_QUERY_START = "json_query(";
	private static final String JSON_QUERY_JSON_END = "' returning json)";
	private static final String JSON_QUERY_BLOB_END = "' returning blob)";

	private final boolean checkConstraintSupport;
	private final JsonSupport jsonSupport;

	private OracleAggregateSupport(boolean checkConstraintSupport, JsonSupport jsonSupport) {
		this.checkConstraintSupport = checkConstraintSupport;
		this.jsonSupport = jsonSupport;
	}

	public static AggregateSupport valueOf(Dialect dialect) {
		final DatabaseVersion version = dialect.getVersion();
		switch ( version.getMajor() ) {
			case 12:
			case 13:
			case 14:
			case 15:
			case 16:
			case 17:
				return V12_INSTANCE;
			case 18:
				return V18_INSTANCE;
			case 19:
			case 20:
				return V19_INSTANCE;
			case 21:
			case 22:
				return V21_INSTANCE;
		}
		return version.isSameOrAfter( 23 )
				? OracleAggregateSupport.V23_INSTANCE
				: OracleAggregateSupport.LEGACY_INSTANCE;
	}

	@Override
	public String aggregateComponentCustomReadExpression(
			String template,
			String placeholder,
			String aggregateParentReadExpression,
			String columnExpression,
			AggregateColumn aggregateColumn,
			Column column) {
		switch ( aggregateColumn.getTypeCode() ) {
			case JSON:
				String jsonTypeName = "json";
				switch ( jsonSupport ) {
					case MERGEPATCH:
					case QUERY_AND_PATH:
					case QUERY:
						jsonTypeName = "blob";
					case OSON:
						final String parentPartExpression;
						if ( aggregateParentReadExpression.startsWith( JSON_QUERY_START )
								&& ( aggregateParentReadExpression.endsWith( JSON_QUERY_JSON_END ) || aggregateParentReadExpression.endsWith( JSON_QUERY_BLOB_END ) ) ) {
							parentPartExpression = aggregateParentReadExpression.substring( JSON_QUERY_START.length(), aggregateParentReadExpression.length() - JSON_QUERY_JSON_END.length() ) + ".";
						}
						else {
							parentPartExpression = aggregateParentReadExpression + ",'$.";
						}
						switch ( column.getTypeCode() ) {
							case BOOLEAN:
								if ( column.getTypeName().toLowerCase( Locale.ROOT ).trim().startsWith( "number" ) ) {
									return template.replace(
											placeholder,
											"decode(json_value(" + parentPartExpression + columnExpression + "'),'true',1,'false',0,null)"
									);
								}
								// Fall-through intended
							case TINYINT:
							case SMALLINT:
							case INTEGER:
							case BIGINT:
								return template.replace(
										placeholder,
										"json_value(" + parentPartExpression + columnExpression + "' returning " + column.getTypeName() + ')'
								);
							case DATE:
								return template.replace(
										placeholder,
										"to_date(json_value(" + parentPartExpression + columnExpression + "'),'YYYY-MM-DD')"
								);
							case TIME:
								return template.replace(
										placeholder,
										"to_timestamp(json_value(" + parentPartExpression + columnExpression + "'),'hh24:mi:ss')"
								);
							case TIMESTAMP:
								return template.replace(
										placeholder,
										"to_timestamp(json_value(" + parentPartExpression + columnExpression + "'),'YYYY-MM-DD\"T\"hh24:mi:ss.FF9')"
								);
							case TIMESTAMP_WITH_TIMEZONE:
							case TIMESTAMP_UTC:
								return template.replace(
										placeholder,
										"to_timestamp_tz(json_value(" + parentPartExpression + columnExpression + "'),'YYYY-MM-DD\"T\"hh24:mi:ss.FF9TZH:TZM')"
								);
							case BINARY:
							case VARBINARY:
							case LONG32VARBINARY:
								// We encode binary data as hex, so we have to decode here
								return template.replace(
										placeholder,
										"hextoraw(json_value(" + parentPartExpression + columnExpression + "'))"
								);
							case CLOB:
							case NCLOB:
							case BLOB:
								// We encode binary data as hex, so we have to decode here
								return template.replace(
										placeholder,
										"(select * from json_table(" + aggregateParentReadExpression + ",'$' columns (" + columnExpression + " " + column.getTypeName() + " path '$." + columnExpression + "')))"
								);
							case ARRAY:
								final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) column.getValue().getType();
								final OracleArrayJdbcType jdbcType = (OracleArrayJdbcType) pluralType.getJdbcType();
								switch ( jdbcType.getElementJdbcType().getDefaultSqlTypeCode() ) {
									case BOOLEAN:
									case DATE:
									case TIME:
									case TIMESTAMP:
									case TIMESTAMP_WITH_TIMEZONE:
									case TIMESTAMP_UTC:
									case BINARY:
									case VARBINARY:
									case LONG32VARBINARY:
										return template.replace(
												placeholder,
												jdbcType.getSqlTypeName() + "_from_json(json_query(" + parentPartExpression + columnExpression + "' returning " + jsonTypeName + "))"
										);
									default:
										return template.replace(
												placeholder,
												"json_value(" + parentPartExpression + columnExpression + "' returning " + column.getTypeName() + ')'
										);
								}
							case JSON:
								return template.replace(
										placeholder,
										"json_query(" + parentPartExpression + columnExpression + "' returning " + jsonTypeName + ")"
								);
							default:
								return template.replace(
										placeholder,
										"cast(json_value(" + parentPartExpression + columnExpression + "') as " + column.getTypeName() + ')'
								);
						}
					case NONE:
						throw new UnsupportedOperationException( "The Oracle version doesn't support JSON aggregates!" );
				}
			case STRUCT:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				return template.replace( placeholder, aggregateParentReadExpression + "." + columnExpression );
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
			case JSON:
				// For JSON we always have to replace the whole object
				return aggregateParentAssignmentExpression;
			case STRUCT:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				return aggregateParentAssignmentExpression + "." + columnExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumn.getTypeCode() );
	}

	private String jsonCustomWriteExpression(
			String customWriteExpression,
			JdbcMapping jdbcMapping,
			SelectableMapping column,
			TypeConfiguration typeConfiguration) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( jsonSupport ) {
			case OSON:
			case MERGEPATCH:
				switch ( sqlTypeCode ) {
					case CLOB:
						return "to_clob(" + customWriteExpression + ")";
					case ARRAY:
						final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) jdbcMapping;
						final OracleArrayJdbcType jdbcType = (OracleArrayJdbcType) pluralType.getJdbcType();
						switch ( jdbcType.getElementJdbcType().getDefaultSqlTypeCode() ) {
							case CLOB:
								return "(select json_arrayagg(to_clob(t.column_value)) from table(" + customWriteExpression + ") t)";
							case BOOLEAN:
								final String elementTypeName = determineElementTypeName( column.toSize(), pluralType, typeConfiguration );
								if ( elementTypeName.toLowerCase( Locale.ROOT ).trim().startsWith( "number" ) ) {
									return "(select json_arrayagg(decode(t.column_value,1,'true',0,'false',null)) from table(" + customWriteExpression + ") t)";
								}
							default:
								break;
						}
					case BOOLEAN:
						final String sqlTypeName = AbstractSqlAstTranslator.getSqlTypeName( column, typeConfiguration );
						if ( sqlTypeName.toLowerCase( Locale.ROOT ).trim().startsWith( "number" ) ) {
							return "decode(" + customWriteExpression + ",1,'true',0,'false',null)";
						}
						// Fall-through intended
					default:
						return customWriteExpression;
				}
		}
		throw new IllegalStateException( "JSON not supported!" );
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

	@Override
	public boolean supportsComponentCheckConstraints() {
		return checkConstraintSupport;
	}

	@Override
	public List<AuxiliaryDatabaseObject> aggregateAuxiliaryDatabaseObjects(
			Namespace namespace,
			String aggregatePath,
			AggregateColumn aggregateColumn,
			List<Column> aggregatedColumns) {
		final int typeCode = aggregateColumn.getTypeCode();
		if ( typeCode == STRUCT_ARRAY || typeCode == STRUCT_TABLE ) {
			final UserDefinedArrayType arrayType = namespace.createUserDefinedArrayType(
					Identifier.toIdentifier( aggregateColumn.getSqlType() ),
					name -> new UserDefinedArrayType( "orm", namespace, name )
			);
			final ArrayJdbcType jdbcType = (ArrayJdbcType) ( (BasicType<?>) aggregateColumn.getValue().getType() ).getJdbcType();
			final StructJdbcType elementJdbcType = (StructJdbcType) jdbcType.getElementJdbcType();
			if ( typeCode == STRUCT_ARRAY ) {
				arrayType.setArraySqlTypeCode( ARRAY );
				arrayType.setArrayLength( aggregateColumn.getArrayLength() == null ? 127 : aggregateColumn.getArrayLength() );
			}
			else {
				arrayType.setArraySqlTypeCode( TABLE );
			}
			arrayType.setElementTypeName( elementJdbcType.getStructTypeName() );
			arrayType.setElementSqlTypeCode( elementJdbcType.getDefaultSqlTypeCode() );
		}
		return super.aggregateAuxiliaryDatabaseObjects(
				namespace,
				aggregatePath,
				aggregateColumn,
				aggregatedColumns
		);
	}

	private String determineJsonTypeName(SelectableMapping aggregateColumn) {
		final String columnDefinition = aggregateColumn.getColumnDefinition();
		if ( columnDefinition == null ) {
			assert aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() == JSON;
			switch ( jsonSupport ) {
				case OSON:
					return "json";
				case MERGEPATCH:
				case QUERY_AND_PATH:
				case QUERY:
					return "blob";
				case NONE:
					return "clob";
			}
		}
		return columnDefinition;
	}

	enum JsonSupport {
		OSON,
		MERGEPATCH,
		QUERY_AND_PATH,
		QUERY,
		NONE;
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
		protected final String ddlTypeName;

		public AggregateJsonWriteExpression(
				SelectableMapping selectableMapping,
				OracleAggregateSupport aggregateSupport) {
			this.embeddableMappingType = ( (AggregateJdbcType) selectableMapping.getJdbcMapping().getJdbcType() )
					.getEmbeddableMappingType();
			this.ddlTypeName = aggregateSupport.determineJsonTypeName( selectableMapping );
		}

		protected void initializeSubExpressions(
				SelectableMapping[] columns,
				OracleAggregateSupport aggregateSupport,
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
			sb.append( "json_object" );
			char separator = '(';
			for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final JsonWriteExpression value = entry.getValue();
				final String subPath = path + "->'" + column + "'";
				sb.append( separator );
				if ( value instanceof AggregateJsonWriteExpression ) {
					sb.append( '\'' );
					sb.append( column );
					sb.append( "':" );
					value.append( sb, subPath, translator, expression );
				}
				else {
					value.append( sb, subPath, translator, expression );
				}
				separator = ',';
			}
			sb.append( " returning " );
			sb.append( ddlTypeName );
			sb.append( ')' );
		}
	}

	private static class RootJsonWriteExpression extends AggregateJsonWriteExpression
			implements WriteExpressionRenderer {
		private final boolean nullable;
		private final String path;

		RootJsonWriteExpression(
				SelectableMapping aggregateColumn,
				SelectableMapping[] columns,
				OracleAggregateSupport aggregateSupport,
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
			sqlAppender.append( "json_mergepatch(" );
			if ( nullable ) {
				sqlAppender.append( "coalesce(" );
				sqlAppender.append( basePath );
				sqlAppender.append( ",json_object(returning " );
				sqlAppender.append( ddlTypeName );
				sqlAppender.append( "))" );
			}
			else {
				sqlAppender.append( basePath );
			}
			sqlAppender.append( ',' );
			append( sqlAppender, basePath, translator, aggregateColumnWriteExpression );
			sqlAppender.append( " returning " );
			sqlAppender.append( ddlTypeName );
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

}

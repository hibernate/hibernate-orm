/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.type.OracleArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.UserDefinedArrayType;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.Literal;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.StructuredJdbcType;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hibernate.type.SqlTypes.*;

public class OracleAggregateSupport extends AggregateSupportImpl {

	protected static final AggregateSupport V23_INSTANCE = new OracleAggregateSupport( true, JsonSupport.OSON, true );
	// Special instance used when an Oracle OSON extension is available and used
	protected static final AggregateSupport V23_OSON_EXT_INSTANCE = new OracleAggregateSupport( true, JsonSupport.OSON,false);
	protected static final AggregateSupport V21_INSTANCE = new OracleAggregateSupport( false, JsonSupport.OSON, true );
	protected static final AggregateSupport V19_INSTANCE = new OracleAggregateSupport( false, JsonSupport.MERGEPATCH , true);
	protected static final AggregateSupport V18_INSTANCE = new OracleAggregateSupport( false, JsonSupport.QUERY_AND_PATH, true );
	protected static final AggregateSupport V12_INSTANCE = new OracleAggregateSupport( false, JsonSupport.QUERY , true);
	protected static final AggregateSupport LEGACY_INSTANCE = new OracleAggregateSupport( false, JsonSupport.NONE , true);

	private static final String JSON_QUERY_START = "json_query(";
	private static final String JSON_QUERY_JSON_END = "' returning json)";
	private static final String JSON_QUERY_BLOB_END = "' returning blob)";
	private static final String XML_EXTRACT_START = "xmlelement(\"" + XmlHelper.ROOT_TAG + "\",xmlquery(";
	private static final String XML_EXTRACT_SEPARATOR = "/*' passing ";
	private static final String XML_EXTRACT_END = " returning content))";
	private static final String XML_QUERY_START = "xmlquery(";
	private static final String XML_QUERY_SEPARATOR = "' passing ";
	private static final String XML_QUERY_END = " returning content)";

	private final boolean checkConstraintSupport;
	private final JsonSupport jsonSupport;
	private final boolean dateTypesStoreAsString;

	OracleAggregateSupport(boolean checkConstraintSupport, JsonSupport jsonSupport, boolean dateTypesStoreAsString) {
		this.checkConstraintSupport = checkConstraintSupport;
		this.jsonSupport = jsonSupport;
		// this flag tell us if data is serialized/de-serialized as String. As opposed to using OSON
		// In other words, this flag tells us if the Oracle OSON JDBC extension is used or not.
		this.dateTypesStoreAsString = dateTypesStoreAsString;
	}

	public static AggregateSupport valueOf(Dialect dialect, boolean useDateStoredAsString) {
		final DatabaseVersion version = dialect.getVersion();
		return switch ( version.getMajor() ) {
			case 12, 13, 14, 15, 16, 17 -> V12_INSTANCE;
			case 18 -> V18_INSTANCE;
			case 19, 20 -> V19_INSTANCE;
			case 21, 22 -> V21_INSTANCE;
			default -> version.isSameOrAfter( 23 )
				? useDateStoredAsString?OracleAggregateSupport.V23_INSTANCE:
				OracleAggregateSupport.V23_OSON_EXT_INSTANCE
								: OracleAggregateSupport.LEGACY_INSTANCE;
		};
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
						switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
							case BIT:
							case BOOLEAN:
								//noinspection unchecked
								final JdbcLiteralFormatter<Boolean> jdbcLiteralFormatter = (JdbcLiteralFormatter<Boolean>) column.getJdbcMapping().getJdbcType()
										.getJdbcLiteralFormatter( column.getJdbcMapping().getMappedJavaType() );
								final Dialect dialect = typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
								final WrapperOptions wrapperOptions = getWrapperOptions( typeConfiguration );
								final String trueLiteral = jdbcLiteralFormatter.toJdbcLiteral( true, dialect, wrapperOptions );
								final String falseLiteral = jdbcLiteralFormatter.toJdbcLiteral( false, dialect, wrapperOptions );
								return template.replace(
										placeholder,
										"decode(json_value(" + parentPartExpression + columnExpression + "'),'true'," + trueLiteral + ",'false'," + falseLiteral + ",null)"
								);
							case TINYINT:
							case SMALLINT:
							case INTEGER:
							case BIGINT:
							case CLOB:
							case NCLOB:
								return template.replace(
										placeholder,
										"json_value(" + parentPartExpression + columnExpression + "' returning " + column.getColumnDefinition() + ')'
								);

							case DATE:
								if (this.dateTypesStoreAsString) {
									return template.replace(
											placeholder,
											"to_date(json_value(" + parentPartExpression + columnExpression + "'),'YYYY-MM-DD')"
									);
								}
								else {
									// Oracle OSON extension is used, value is not stored as string
									return template.replace(
											placeholder,
											"json_value(" + parentPartExpression + columnExpression + "' returning date)"
									);
								}

							case TIME:
								return template.replace(
										placeholder,
										"to_timestamp(json_value(" + parentPartExpression + columnExpression + "'),'hh24:mi:ss')"
								);
							case TIMESTAMP:
								if (this.dateTypesStoreAsString) {
									return template.replace(
											placeholder,
											"to_timestamp(json_value(" + parentPartExpression + columnExpression + "'),'YYYY-MM-DD\"T\"hh24:mi:ss.FF9')"
									);
								}
								else {

									return template.replace(
											placeholder,
											"json_value(" + parentPartExpression + columnExpression + "' returning timestamp)"
									);
								}
							case DURATION:
								if (this.dateTypesStoreAsString) {
									return template.replace(
											placeholder,
											"cast(json_value(" + parentPartExpression + columnExpression + "') as " + column.getColumnDefinition() + ')'
									);
								}
								else {
									return template.replace(
											placeholder,
											"json_value(" + parentPartExpression + columnExpression + "' returning interval day to second)"
									);
								}
							case TIMESTAMP_WITH_TIMEZONE:
							case TIMESTAMP_UTC:
								if (this.dateTypesStoreAsString) {
									return template.replace(
											placeholder,
											"to_timestamp_tz(json_value(" + parentPartExpression + columnExpression + "'),'YYYY-MM-DD\"T\"hh24:mi:ss.FF9TZH:TZM')"
									);
								}
								else {
									// Oracle OSON extension is used, value is not stored as string
									return template.replace(
											placeholder,
											"json_value(" + parentPartExpression + columnExpression + "')"
									);
								}
							case UUID:
								if (this.dateTypesStoreAsString) {
									return template.replace(
											placeholder,
											"hextoraw(replace(json_value(" + parentPartExpression + columnExpression + "'),'-',''))"
									);
								}
							case BINARY:
							case VARBINARY:
							case LONG32VARBINARY:
								// We encode binary data as hex, so we have to decode here
								if ( determineLength( column ) * 2 < 4000L ) {
									return template.replace(
											placeholder,
											"hextoraw(json_value(" + parentPartExpression + columnExpression + "'))"
									);
								}
								// Fall-through intended
							case BLOB:
								// We encode binary data as hex, so we have to decode here
								return template.replace(
										placeholder,
										// returning binary data is not yet implemented in the json functions,
										// so use the xml implementation
										"xmlcast(xmlcdata(json_value(" + parentPartExpression + columnExpression + "' returning clob))) as " + column.getColumnDefinition() + ')'
								);
							case ARRAY:
								final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) column.getJdbcMapping();
								final OracleArrayJdbcType jdbcType = (OracleArrayJdbcType) pluralType.getJdbcType();
								switch ( jdbcType.getElementJdbcType().getDefaultSqlTypeCode() ) {

									case DATE:
										return template.replace(
												placeholder,
												"json_value(" + parentPartExpression + columnExpression + "' returning " + column.getColumnDefinition() + ')'
										);
									case BOOLEAN:
									case TIME:
									case TIMESTAMP:
									case TIMESTAMP_WITH_TIMEZONE:
									case TIMESTAMP_UTC:
									case BINARY:
									case VARBINARY:
									case LONG32VARBINARY:
									case UUID:
									default:
										return template.replace(
												placeholder,
												jdbcType.getSqlTypeName() + "_from_json(json_query(" + parentPartExpression + columnExpression + "' returning " + jsonTypeName + "))"
										);
								}
							case JSON:
							case JSON_ARRAY:
								return template.replace(
										placeholder,
										"json_query(" + parentPartExpression + columnExpression + "' returning " + jsonTypeName + ")"
								);
							default:
								return template.replace(
										placeholder,
										"cast(json_value(" + parentPartExpression + columnExpression + "') as " + column.getColumnDefinition() + ')'
								);

						}
					case NONE:
						throw new UnsupportedOperationException( "The Oracle version doesn't support JSON aggregates!" );
				}
			case SQLXML:
			case XML_ARRAY:
				switch ( column.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode() ) {
					case BIT:
					case BOOLEAN:
						//noinspection unchecked
						final JdbcLiteralFormatter<Boolean> jdbcLiteralFormatter = (JdbcLiteralFormatter<Boolean>) column.getJdbcMapping().getJdbcType()
								.getJdbcLiteralFormatter( column.getJdbcMapping().getMappedJavaType() );
						final Dialect dialect = typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
						final WrapperOptions wrapperOptions = getWrapperOptions( typeConfiguration );
						final String trueLiteral = jdbcLiteralFormatter.toJdbcLiteral( true, dialect, wrapperOptions );
						final String falseLiteral = jdbcLiteralFormatter.toJdbcLiteral( false, dialect, wrapperOptions );
						return template.replace(
								placeholder,
								"decode(xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as varchar(5)),'true'," + trueLiteral + ",'false'," + falseLiteral + ",null)"
						);
					case FLOAT:
					case REAL:
					case DOUBLE:
						// Since cast is the only way to do optional exponential form parsing, we have to use that.
						// Unfortunately, the parsing is nationalized, so we need to replace the standard decimal separator dot with the nationalized one first
						return template.replace(
								placeholder,
								"cast(replace(xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as varchar2(255)),'.',substr(to_char(0.1),1,1)) as " + column.getColumnDefinition() + ")"
						);
					case DATE:
						return template.replace(
								placeholder,
								"to_date(xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as varchar(35)),'YYYY-MM-DD')"
						);
					case TIME:
						return template.replace(
								placeholder,
								"to_timestamp(xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as varchar(35)),'hh24:mi:ss')"
						);
					case TIMESTAMP:
						return template.replace(
								placeholder,
								"to_timestamp(xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as varchar(35)),'YYYY-MM-DD\"T\"hh24:mi:ss.FF9')"
						);
					case TIMESTAMP_WITH_TIMEZONE:
					case TIMESTAMP_UTC:
						return template.replace(
								placeholder,
								"to_timestamp_tz(xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as varchar(35)),'YYYY-MM-DD\"T\"hh24:mi:ss.FF9TZH:TZM')"
						);
					case ARRAY:
						throw new UnsupportedOperationException( "Transforming XML_ARRAY to native arrays is not supported on Oracle!" );
					case SQLXML:
						return template.replace(
								placeholder,
								XML_EXTRACT_START + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/*" ) + "))"
						);
					case XML_ARRAY:
						if ( typeConfiguration.getCurrentBaseSqlTypeIndicators().isXmlFormatMapperLegacyFormatEnabled() ) {
							throw new IllegalArgumentException( "XML array '" + columnExpression + "' in '" + aggregateParentReadExpression + "' is not supported with legacy format enabled." );
						}
						else {
							return template.replace(
									placeholder,
									"xmlelement(\"Collection\",xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/*" ) + "))"
							);
						}
					case UUID:
						if ( SqlTypes.isBinaryType( column.getJdbcMapping().getJdbcType().getDdlTypeCode() ) ) {
							return template.replace(
									placeholder,
									"hextoraw(replace(xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as varchar(36)),'-',''))"
							);
						}
						// Fall-through intended
					default:
						return template.replace(
								placeholder,
								"xmlcast(xmlquery(" + xmlExtractArguments( aggregateParentReadExpression, columnExpression + "/text()" ) + ") as " + column.getColumnDefinition() + ")"
						);
				}
			case STRUCT:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				return template.replace( placeholder, aggregateParentReadExpression + "." + columnExpression );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
	}

	private static WrapperOptions getWrapperOptions(TypeConfiguration typeConfiguration) {
		try {
			return typeConfiguration.getSessionFactory().getWrapperOptions();
		}
		catch (HibernateException e) {
			// before we have a SessionFactory, no useful WrapperOptions to pass
			return null;
		}
	}

	private static String xmlExtractArguments(String aggregateParentReadExpression, String xpathFragment) {
		final String extractArguments;
		int separatorIndex;
		if ( aggregateParentReadExpression.startsWith( XML_EXTRACT_START )
			&& aggregateParentReadExpression.endsWith( XML_EXTRACT_END )
			&& (separatorIndex = aggregateParentReadExpression.indexOf( XML_EXTRACT_SEPARATOR )) != -1 ) {
			final StringBuilder sb = new StringBuilder( aggregateParentReadExpression.length() - XML_EXTRACT_START.length() + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, XML_EXTRACT_START.length(), separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex + 2, aggregateParentReadExpression.length() - 2 );
			extractArguments = sb.toString();
		}
		else if ( aggregateParentReadExpression.startsWith( XML_QUERY_START )
				&& aggregateParentReadExpression.endsWith( XML_QUERY_END )
				&& (separatorIndex = aggregateParentReadExpression.indexOf( XML_QUERY_SEPARATOR )) != -1 ) {
			final StringBuilder sb = new StringBuilder( aggregateParentReadExpression.length() - XML_QUERY_START.length() + xpathFragment.length() );
			sb.append( aggregateParentReadExpression, XML_QUERY_START.length(), separatorIndex );
			sb.append( '/' );
			sb.append( xpathFragment );
			sb.append( aggregateParentReadExpression, separatorIndex, aggregateParentReadExpression.length() - 1 );
			extractArguments = sb.toString();
		}
		else {
			extractArguments = "'/" + XmlHelper.ROOT_TAG + "/" + xpathFragment + "' passing " + aggregateParentReadExpression + " returning content";
		}
		return extractArguments;
	}

	private static long determineLength(SqlTypedMapping column) {
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
			return 4000L;
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
			case SQLXML:
			case XML_ARRAY:
				// For JSON/XML we always have to replace the whole object
				return aggregateParentAssignmentExpression;
			case STRUCT:
			case STRUCT_ARRAY:
			case STRUCT_TABLE:
				return aggregateParentAssignmentExpression + "." + columnExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateColumnTypeCode );
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
			case QUERY_AND_PATH:
			case QUERY:
				switch ( sqlTypeCode ) {
					case CLOB:
						return "to_clob(" + customWriteExpression + ")";
					case UUID:
						return "regexp_replace(lower(rawtohex(" + customWriteExpression + ")),'^(.{8})(.{4})(.{4})(.{4})(.{12})$','\\1-\\2-\\3-\\4-\\5')";
					case ARRAY:
						final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) jdbcMapping;
						final OracleArrayJdbcType jdbcType = (OracleArrayJdbcType) pluralType.getJdbcType();
						switch ( jdbcType.getElementJdbcType().getDefaultSqlTypeCode() ) {
							case CLOB:
								return "(select json_arrayagg(to_clob(t.column_value)) from table(" + customWriteExpression + ") t)";
							case UUID:
								return "(select json_arrayagg(regexp_replace(lower(rawtohex(t.column_value)),'^(.{8})(.{4})(.{4})(.{4})(.{12})$','\\1-\\2-\\3-\\4-\\5')) from table(" + customWriteExpression + ") t)";
							case BIT:
								return "decode(" + customWriteExpression + ",1,'true',0,'false',null)";
							case BOOLEAN:
								final String elementTypeName = determineElementTypeName( column.toSize(), pluralType, typeConfiguration );
								if ( elementTypeName.toLowerCase( Locale.ROOT ).trim().startsWith( "number" ) ) {
									return "(select json_arrayagg(decode(t.column_value,1,'true',0,'false',null)) from table(" + customWriteExpression + ") t)";
								}
							default:
								break;
						}
						return customWriteExpression;
					case BIT:
						return "decode(" + customWriteExpression + ",1,'true',0,'false',null)";
					case BOOLEAN:
						//noinspection unchecked
						final JdbcLiteralFormatter<Boolean> jdbcLiteralFormatter = (JdbcLiteralFormatter<Boolean>) jdbcMapping.getJdbcType()
								.getJdbcLiteralFormatter( jdbcMapping.getMappedJavaType() );
						final Dialect dialect = typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
						final WrapperOptions wrapperOptions = getWrapperOptions( typeConfiguration );
						final String trueLiteral = jdbcLiteralFormatter.toJdbcLiteral( true, dialect, wrapperOptions );
						final String falseLiteral = jdbcLiteralFormatter.toJdbcLiteral( false, dialect, wrapperOptions );
						return "decode(" + customWriteExpression + "," + trueLiteral + ",'true'," + falseLiteral + ",'false')";
						// Fall-through intended
					default:
						return customWriteExpression;
				}
		}
		throw new IllegalStateException( "JSON not supported!" );
	}

	private static String xmlCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping, TypeConfiguration typeConfiguration) {
		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case UUID:
				return "regexp_replace(lower(rawtohex(" + customWriteExpression + ")),'^(.{8})(.{4})(.{4})(.{4})(.{12})$','\\1-\\2-\\3-\\4-\\5')";
//			case ARRAY:
//			case XML_ARRAY:
//				return "(" + customWriteExpression + ") format json";
			case BOOLEAN:
				//noinspection unchecked
				final JdbcLiteralFormatter<Boolean> jdbcLiteralFormatter = (JdbcLiteralFormatter<Boolean>) jdbcMapping.getJdbcType()
						.getJdbcLiteralFormatter( jdbcMapping.getMappedJavaType() );
				final Dialect dialect = typeConfiguration.getCurrentBaseSqlTypeIndicators().getDialect();
				final WrapperOptions wrapperOptions = getWrapperOptions( typeConfiguration );
				final String trueLiteral = jdbcLiteralFormatter.toJdbcLiteral( true, dialect, wrapperOptions );
				final String falseLiteral = jdbcLiteralFormatter.toJdbcLiteral( false, dialect, wrapperOptions );
				return "decode(" + customWriteExpression + "," + trueLiteral + ",'true'," + falseLiteral + ",'false')";
//			case TIME:
//				return "varchar_format(timestamp('1970-01-01'," + customWriteExpression + "),'HH24:MI:SS')";
//			case TIMESTAMP:
//				return "replace(varchar_format(" + customWriteExpression + ",'YYYY-MM-DD HH24:MI:SS.FF9'),' ','T')";
//			case TIMESTAMP_UTC:
//				return "replace(varchar_format(" + customWriteExpression + ",'YYYY-MM-DD HH24:MI:SS.FF9'),' ','T')||'Z'";
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
				return new RootJsonWriteExpression( aggregateColumn, columnsToUpdate, this, typeConfiguration );
			case SQLXML:
				return new RootXmlWriteExpression( aggregateColumn, columnsToUpdate, typeConfiguration );
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
			final StructuredJdbcType elementJdbcType = (StructuredJdbcType) jdbcType.getElementJdbcType();
			if ( typeCode == STRUCT_ARRAY ) {
				arrayType.setArraySqlTypeCode( ARRAY );
				arrayType.setArrayLength( aggregateColumn.getArrayLength() == null ? 127 : aggregateColumn.getArrayLength() );
			}
			else {
				arrayType.setArraySqlTypeCode( TABLE );
			}
			arrayType.setElementTypeName( elementJdbcType.getStructTypeName() );
			arrayType.setElementSqlTypeCode( elementJdbcType.getDefaultSqlTypeCode() );
			arrayType.setElementDdlTypeCode( elementJdbcType.getDdlTypeCode() );
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
			return switch ( jsonSupport ) {
				case OSON -> "json";
				case MERGEPATCH, QUERY_AND_PATH, QUERY -> "blob";
				case NONE -> "clob";
			};
		}
		return columnDefinition;
	}

	enum JsonSupport {
		OSON,
		MERGEPATCH,
		QUERY_AND_PATH,
		QUERY,
		NONE
	}

	interface JsonWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}
	private static class AggregateJsonWriteExpression implements JsonWriteExpression {
		private final boolean colonSyntax;
		private final LinkedHashMap<String, JsonWriteExpression> subExpressions = new LinkedHashMap<>();
		protected final EmbeddableMappingType embeddableMappingType;
		protected final String ddlTypeName;

		public AggregateJsonWriteExpression(SelectableMapping selectableMapping, OracleAggregateSupport aggregateSupport) {
			this.colonSyntax = aggregateSupport.jsonSupport == JsonSupport.OSON
					|| aggregateSupport.jsonSupport == JsonSupport.MERGEPATCH;
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
								),
								colonSyntax
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
					if ( colonSyntax ) {
						sb.append( "':" );
					}
					else {
						sb.append( "' value " );
					}
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

		private final boolean colonSyntax;
		private final SelectableMapping selectableMapping;
		private final String customWriteExpressionStart;
		private final String customWriteExpressionEnd;

		BasicJsonWriteExpression(SelectableMapping selectableMapping, String customWriteExpression, boolean colonSyntax) {
			this.selectableMapping = selectableMapping;
			this.colonSyntax = colonSyntax;
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
			if ( colonSyntax ) {
				sb.append( "':" );
			}
			else {
				sb.append( "' value " );
			}
			sb.append( customWriteExpressionStart );
			// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
			// since we don't know how the custom write expression looks like where this is embedded,
			// so we have to be pessimistic and avoid ambiguities
			final Expression valueExpression = expression.getValueExpression( selectableMapping );
			if ( valueExpression instanceof Literal literal && literal.getLiteralValue() == null ) {
				// Except for the null literal. That is just rendered as-is
				sb.append( "null" );
			}
			else {
				translator.render( valueExpression, SqlAstNodeRenderingMode.NO_UNTYPED );
			}
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

		protected void initializeSubExpressions(SelectableMapping aggregateColumn, SelectableMapping[] columns, TypeConfiguration typeConfiguration) {
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
								xmlCustomWriteExpression( customWriteExpression, column.getJdbcMapping(), typeConfiguration )
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
			sb.append( "xmlelement(" );
			sb.appendDoubleQuoteEscapedString( getTagName() );
			sb.append( ",xmlconcat" );
			char separator = '(';
			for ( Map.Entry<String, XmlWriteExpression> entry : subExpressions.entrySet() ) {
				sb.append( separator );

				final XmlWriteExpression value = entry.getValue();
				if ( value instanceof AggregateXmlWriteExpression ) {
					final String subPath = "xmlquery(" + xmlExtractArguments( path, entry.getKey() ) + ")";
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

	private static class RootXmlWriteExpression extends AggregateXmlWriteExpression
			implements WriteExpressionRenderer {
		private final String path;

		RootXmlWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns, TypeConfiguration typeConfiguration) {
			super( aggregateColumn, aggregateColumn.getColumnDefinition() );
			path = aggregateColumn.getSelectionExpression();
			initializeSubExpressions( aggregateColumn, columns, typeConfiguration );
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
			append( sqlAppender, "xmlquery('/" + getTagName() + "' passing " + basePath + " returning content)", translator, aggregateColumnWriteExpression );
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
			sb.append( "xmlelement(" );
			sb.appendDoubleQuoteEscapedString( selectableMapping.getSelectableName() );
			sb.append( ',' );
			if ( isArray ) {
				// Remove the <Collection> tag to wrap the value into the selectable specific tag
				sb.append( "xmlquery('/Collection/*' passing " );
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
				sb.append( " returning content)" );
			}
			sb.append( ')' );
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
			sb.append( "xmlquery(" );
			sb.append( xmlExtractArguments( path, selectableMapping.getSelectableName() ) );
			sb.append( ")" );
		}
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;
import java.lang.reflect.Array;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.TimeZone;

import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.StructAttributeValues;
import org.hibernate.type.descriptor.jdbc.StructHelper;
import org.hibernate.type.descriptor.jdbc.StructuredJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.descriptor.jdbc.StructHelper.getEmbeddedPart;
import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * Implementation for serializing/deserializing an embeddable aggregate to/from the GaussDB component format.
 * For regular queries, we select the individual struct elements because the GaussDB component format encoding
 * is probably not very efficient.
 *
 * @author liubao
 *
 * Notes: Original code of this class is based on AbstractPostgreSQLStructJdbcType.
 */
public abstract class GaussDBAbstractStructuredJdbcType implements StructuredJdbcType {

	private static final DateTimeFormatter LOCAL_DATE_TIME;
	static {
		LOCAL_DATE_TIME = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.append(DateTimeFormatter.ISO_LOCAL_DATE)
				.appendLiteral(' ')
				.append(DateTimeFormatter.ISO_LOCAL_TIME)
				.optionalStart()
				.appendOffset( "+HH:mm", "+00" )
				.toFormatter();
	}

	// Need a custom formatter for parsing what PostgresPlus/EDB produces
	private static final DateTimeFormatter LOCAL_DATE;
	static {
		LOCAL_DATE = new DateTimeFormatterBuilder()
				.parseCaseInsensitive()
				.append(DateTimeFormatter.ISO_LOCAL_DATE)
				.optionalStart()
				.appendLiteral(' ')
				.append(DateTimeFormatter.ISO_LOCAL_TIME)
				.optionalStart()
				.appendOffset( "+HH:mm", "+00" )
				.toFormatter();
	}
	private final String typeName;
	private final int[] orderMapping;
	private final int[] inverseOrderMapping;
	private final EmbeddableMappingType embeddableMappingType;

	protected GaussDBAbstractStructuredJdbcType(
			EmbeddableMappingType embeddableMappingType,
			String typeName,
			int[] orderMapping) {
		this.typeName = typeName;
		this.embeddableMappingType = embeddableMappingType;
		this.orderMapping = orderMapping;
		if ( orderMapping == null ) {
			this.inverseOrderMapping = null;
		}
		else {
			final int[] inverseOrderMapping = new int[orderMapping.length];
			for ( int i = 0; i < orderMapping.length; i++ ) {
				inverseOrderMapping[orderMapping[i]] = i;
			}
			this.inverseOrderMapping = inverseOrderMapping;
		}
	}

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.STRUCT;
	}

	@Override
	public String getStructTypeName() {
		return typeName;
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return embeddableMappingType;
	}

	@Override
	public <T> JavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		if ( embeddableMappingType == null ) {
			return typeConfiguration.getJavaTypeRegistry().getDescriptor( Object[].class );
		}
		else {
			//noinspection unchecked
			return (JavaType<T>) embeddableMappingType.getMappedJavaType();
		}
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getObject( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getObject( statement.getObject( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getObject( statement.getObject( name ), options );
			}

			private X getObject(Object object, WrapperOptions options) throws SQLException {
				if ( object == null ) {
					return null;
				}
				return ( (GaussDBAbstractStructuredJdbcType) getJdbcType() ).fromString(
						object.toString(),
						getJavaType(),
						options
				);
			}
		};
	}

	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( string == null ) {
			return null;
		}
		final boolean returnEmbeddable = javaType.getJavaTypeClass() != Object[].class;
		final int end;
		final Object[] array;
		if ( embeddableMappingType == null ) {
			assert !returnEmbeddable;
			final ArrayList<Object> values = new ArrayList<>( 8 );
			end = deserializeStruct( string, 0, string.length() - 1, values );
			array = values.toArray();
		}
		else {
			array = new Object[embeddableMappingType.getJdbcValueCount() + ( embeddableMappingType.isPolymorphic() ? 1 : 0 )];
			end = deserializeStruct( string, 0, 0, array, returnEmbeddable, options );
		}
		assert end == string.length();
		if ( returnEmbeddable ) {
			final StructAttributeValues attributeValues = getAttributeValues( embeddableMappingType, orderMapping, array, options );
			//noinspection unchecked
			return (X) instantiate( embeddableMappingType, attributeValues );
		}
		else if ( inverseOrderMapping != null ) {
			StructHelper.orderJdbcValues( embeddableMappingType, inverseOrderMapping, array.clone(), array );
		}
		//noinspection unchecked
		return (X) array;
	}

	private int deserializeStruct(
			String string,
			int begin,
			int end,
			ArrayList<Object> values) {
		int column = 0;
		boolean inQuote = false;
		boolean hasEscape = false;
		assert string.charAt( begin ) == '(';
		int start = begin + 1;
		int element = 1;
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '"':
					if ( inQuote ) {
						if ( i + 1 != end && string.charAt( i + 1 ) == '"' ) {
							// Skip double quotes as that will be unescaped later
							i++;
							hasEscape = true;
							continue;
						}
						if ( hasEscape ) {
							values.add( unescape( string, start, i ) );
						}
						else {
							values.add( string.substring( start, i ) );
						}
						column++;
						inQuote = false;
					}
					else {
						inQuote = true;
					}
					hasEscape = false;
					start = i + 1;
					break;
				case ',':
					if ( !inQuote ) {
						if ( column < element ) {
							if ( start == i ) {
								values.add( null );
							}
							else {
								values.add( string.substring( start, i ) );
							}
							column++;
						}
						start = i + 1;
						element++;
					}
					break;
				case ')':
					if ( !inQuote ) {
						if ( column < element ) {
							if ( start == i ) {
								values.add( null );
							}
							else {
								values.add( string.substring( start, i ) );
							}
						}
						return i + 1;
					}
					break;
			}
		}

		throw new IllegalArgumentException( "Struct not properly formed: " + string.subSequence( start, end ) );
	}

	private int deserializeStruct(
			String string,
			int begin,
			int quotes,
			Object[] values,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		int column = 0;
		boolean inQuote = false;
		StringBuilder escapingSb = null;
		assert string.charAt( begin ) == '(';
		int start = begin + 1;
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '\\':
					if ( inQuote ) {
						final int expectedQuoteCount = 1 << quotes;
						if ( repeatsChar( string, i, expectedQuoteCount, '\\' ) ) {
							if ( isDoubleQuote( string, i + expectedQuoteCount, expectedQuoteCount ) ) {
								// Skip quote escaping as that will be unescaped later
								if ( escapingSb == null ) {
									escapingSb = new StringBuilder();
								}
								escapingSb.append( string, start, i );
								escapingSb.append( '"' );
								// Move forward to the last quote
								i += expectedQuoteCount + expectedQuoteCount - 1;
								start = i + 1;
								continue;
							}
							else {
								assert repeatsChar( string, i + expectedQuoteCount, expectedQuoteCount, '\\' );
								// Don't create an escaping string builder for binary literals
								if ( i != start || !isBinary( column ) ) {
									// Skip quote escaping as that will be unescaped later
									if ( escapingSb == null ) {
										escapingSb = new StringBuilder();
									}
									escapingSb.append( string, start, i );
									escapingSb.append( '\\' );
									start = i + expectedQuoteCount + expectedQuoteCount;
								}
								// Move forward to the last backslash
								i += expectedQuoteCount + expectedQuoteCount - 1;
								continue;
							}
						}
					}
					// Fall-through since a backslash is an escaping mechanism for a start quote within arrays
				case '"':
					if ( inQuote ) {
						if ( isDoubleQuote( string, i, 1 << ( quotes + 1 ) ) ) {
							// Skip quote escaping as that will be unescaped later
							if ( escapingSb == null ) {
								escapingSb = new StringBuilder();
							}
							escapingSb.append( string, start, i );
							escapingSb.append( '"' );
							// Move forward to the last quote
							i += ( 1 << ( quotes + 1 ) ) - 1;
							start = i + 1;
							continue;
						}
						assert isDoubleQuote( string, i, 1 << quotes );
						final JdbcMapping jdbcMapping = getJdbcValueSelectable( column ).getJdbcMapping();
						switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
							case SqlTypes.DATE:
								values[column] = fromRawObject(
										jdbcMapping,
										parseDate(
												CharSequenceHelper.subSequence(
														string,
														start,
														i
												)
										),
										options
								);
								break;
							case SqlTypes.TIME:
							case SqlTypes.TIME_WITH_TIMEZONE:
							case SqlTypes.TIME_UTC:
								values[column] = fromRawObject(
										jdbcMapping,
										parseTime(
												CharSequenceHelper.subSequence(
														string,
														start,
														i
												)
										),
										options
								);
								break;
							case SqlTypes.TIMESTAMP:
								values[column] = fromRawObject(
										jdbcMapping,
										parseTimestamp(
												CharSequenceHelper.subSequence(
														string,
														start,
														i
												),
												jdbcMapping.getJdbcJavaType()
										),
										options
								);
								break;
							case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
							case SqlTypes.TIMESTAMP_UTC:
								values[column] = fromRawObject(
										jdbcMapping,
										parseTimestampWithTimeZone(
												CharSequenceHelper.subSequence(
														string,
														start,
														i
												),
												jdbcMapping.getJdbcJavaType()
										),
										options
								);
								break;
							case SqlTypes.BINARY:
							case SqlTypes.VARBINARY:
							case SqlTypes.LONGVARBINARY:
							case SqlTypes.LONG32VARBINARY:
								final int backslashes = 1 << ( quotes + 1 );
								assert repeatsChar( string, start, backslashes, '\\' );
								final int xCharPosition = start + backslashes;
								assert string.charAt( xCharPosition ) == 'x';
								values[column] = fromString(
										jdbcMapping,
										string,
										xCharPosition + 1,
										i
								);
								break;
							default:
								if ( escapingSb == null || escapingSb.length() == 0 ) {
									values[column] = fromString(
											jdbcMapping,
											string,
											start,
											i
									);
								}
								else {
									escapingSb.append( string, start, i );
									values[column] = fromString(
											jdbcMapping,
											escapingSb,
											0,
											escapingSb.length()
									);
									escapingSb.setLength( 0 );
								}
								break;
						}
						column++;
						inQuote = false;
						// move forward the index by 2 ^ quoteLevel to point to the next char after the quote
						i += 1 << quotes;
						if ( string.charAt( i ) == ')' ) {
							// Return the end position if this is the last element
							assert column == values.length;
							return i + 1;
						}
						// at this point, we must see a comma to indicate the next element
						assert string.charAt( i ) == ',';
					}
					else {
						// This is a start quote, so move forward the index to the last quote
						final int expectedQuotes = 1 << quotes;
						assert isDoubleQuote( string, i, expectedQuotes );
						i += expectedQuotes - 1;
						if ( string.charAt( i + 1 ) == '(' ) {
							// This could be a nested struct
							final JdbcMapping jdbcMapping = getJdbcValueSelectable( column ).getJdbcMapping();
							if ( jdbcMapping.getJdbcType() instanceof GaussDBAbstractStructuredJdbcType structJdbcType ) {
								final Object[] subValues = new Object[structJdbcType.embeddableMappingType.getJdbcValueCount()];
								final int subEnd = structJdbcType.deserializeStruct(
										string,
										i + 1,
										quotes + 1,
										subValues,
										returnEmbeddable,
										options
								);
								if ( returnEmbeddable ) {
									final StructAttributeValues attributeValues = structJdbcType.getAttributeValues(
											structJdbcType.embeddableMappingType,
											structJdbcType.orderMapping,
											subValues,
											options
									);
									values[column] = instantiate( structJdbcType.embeddableMappingType, attributeValues );
								}
								else {
									if ( structJdbcType.inverseOrderMapping != null ) {
										StructHelper.orderJdbcValues(
												structJdbcType.embeddableMappingType,
												structJdbcType.inverseOrderMapping,
												subValues.clone(),
												subValues
										);
									}
									values[column] = subValues;
								}
								column++;
								// The subEnd points to the first character after the ')',
								// so move forward the index to point to the next char after quotes
								assert isDoubleQuote( string, subEnd, expectedQuotes );
								i = subEnd + expectedQuotes;
								if ( string.charAt( i ) == ')' ) {
									// Return the end position if this is the last element
									assert column == values.length;
									return i + 1;
								}
								// at this point, we must see a comma to indicate the next element
								assert string.charAt( i ) == ',';
							}
							else {
								inQuote = true;
							}
						}
						else if ( string.charAt( i + 1 ) == '{' ) {
							// This could be a quoted array
							final JdbcMapping jdbcMapping = getJdbcValueSelectable( column ).getJdbcMapping();
							if ( jdbcMapping instanceof BasicPluralType<?, ?> pluralType ) {
								final ArrayList<Object> arrayList = new ArrayList<>();
								//noinspection unchecked
								final int subEnd = deserializeArray(
										string,
										i + 1,
										quotes + 1,
										arrayList,
										(BasicType<Object>) pluralType.getElementType(),
										returnEmbeddable,
										options
								);
								assert string.charAt( subEnd - 1 ) == '}';
								values[column] = pluralType.getJdbcJavaType().wrap( arrayList, options );
								column++;
								// The subEnd points to the first character after the ')',
								// so move forward the index to point to the next char after quotes
								assert isDoubleQuote( string, subEnd, expectedQuotes );
								i = subEnd + expectedQuotes;
								if ( string.charAt( i ) == ')' ) {
									// Return the end position if this is the last element
									assert column == values.length;
									return i + 1;
								}
								// at this point, we must see a comma to indicate the next element
								assert string.charAt( i ) == ',';
							}
							else {
								inQuote = true;
							}
						}
						else {
							inQuote = true;
						}
					}
					start = i + 1;
					break;
				case ',':
					if ( !inQuote ) {
						if ( start == i ) {
							values[column] = null;
						}
						else {
							final JdbcMapping jdbcMapping = getJdbcValueSelectable( column ).getJdbcMapping();
							if ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() == SqlTypes.BOOLEAN ) {
								values[column] = fromRawObject(
										jdbcMapping,
										string.charAt( start ) == 't',
										options
								);
							}
							else if ( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass().isEnum()
									&& jdbcMapping.getJdbcType().isInteger() ) {
								values[column] = fromRawObject(
										jdbcMapping,
										IntegerJavaType.INSTANCE.fromEncodedString( string, start, i ),
										options
								);
							}
							else {
								values[column] = fromString(
										jdbcMapping,
										string,
										start,
										i
								);
							}
						}
						column++;
						start = i + 1;
					}
					break;
				case ')':
					if ( !inQuote ) {
						if ( column < values.length ) {
							if ( start == i ) {
								values[column] = null;
							}
							else {
								final JdbcMapping jdbcMapping = getJdbcValueSelectable( column ).getJdbcMapping();
								if ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() == SqlTypes.BOOLEAN ) {
									values[column] = fromRawObject(
											jdbcMapping,
											string.charAt( start ) == 't',
											options
									);
								}
								else if ( jdbcMapping.getJavaTypeDescriptor().getJavaTypeClass().isEnum()
										&& jdbcMapping.getJdbcType().isInteger() ) {
									values[column] = fromRawObject(
											jdbcMapping,
											IntegerJavaType.INSTANCE.fromEncodedString( string, start, i ),
											options
									);
								}
								else {
									values[column] = fromString(
											jdbcMapping,
											string,
											start,
											i
									);
								}
							}
						}
						return i + 1;
					}
					break;
				case '{':
					if ( !inQuote ) {
						final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) getJdbcValueSelectable( column ).getJdbcMapping();
						final ArrayList<Object> arrayList = new ArrayList<>();
						//noinspection unchecked
						i = deserializeArray(
								string,
								i,
								quotes + 1,
								arrayList,
								(BasicType<Object>) pluralType.getElementType(),
								returnEmbeddable,
								options
						);
						assert string.charAt( i - 1 ) == '}';
						values[column] = pluralType.getJdbcJavaType().wrap( arrayList, options );
						column++;
						if ( string.charAt( i ) == ')' ) {
							// Return the end position if this is the last element
							assert column == values.length;
							return i + 1;
						}
						// at this point, we must see a comma to indicate the next element
						assert string.charAt( i ) == ',';
						start = i + 1;
					}
					break;
			}
		}

		throw new IllegalArgumentException( "Struct not properly formed: " + string.substring( start ) );
	}

	private boolean isBinary(int column) {
		return isBinary( getJdbcValueSelectable( column ).getJdbcMapping() );
	}

	private static boolean isBinary(JdbcMapping jdbcMapping) {
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
				return true;
		}
		return false;
	}

	private int deserializeArray(
			String string,
			int begin,
			int quotes,
			ArrayList<Object> values,
			BasicType<Object> elementType,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		boolean inQuote = false;
		StringBuilder escapingSb = null;
		assert string.charAt( begin ) == '{';
		int start = begin + 1;
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '\\':
					if ( inQuote ) {
						final int expectedQuoteCount = 1 << quotes;
						if ( repeatsChar( string, i, expectedQuoteCount, '\\' ) ) {
							if ( isDoubleQuote( string, i + expectedQuoteCount, expectedQuoteCount ) ) {
								// Skip quote escaping as that will be unescaped later
								if ( escapingSb == null ) {
									escapingSb = new StringBuilder();
								}
								escapingSb.append( string, start, i );
								escapingSb.append( '"' );
								// Move forward to the last quote
								i += expectedQuoteCount + expectedQuoteCount - 1;
								start = i + 1;
								continue;
							}
							else {
								assert repeatsChar( string, i + expectedQuoteCount, expectedQuoteCount, '\\' );
								// Don't create an escaping string builder for binary literals
								if ( i != start || !isBinary( elementType ) ) {
									// Skip quote escaping as that will be unescaped later
									if ( escapingSb == null ) {
										escapingSb = new StringBuilder();
									}
									escapingSb.append( string, start, i );
									escapingSb.append( '\\' );
									start = i + expectedQuoteCount + expectedQuoteCount;
								}
								// Move forward to the last backslash
								i += expectedQuoteCount + expectedQuoteCount - 1;
								continue;
							}
						}
					}
					// Fall-through since a backslash is an escaping mechanism for a start quote within arrays
				case '"':
					if ( inQuote ) {
						if ( isDoubleQuote( string, i, 1 << ( quotes + 1 ) ) ) {
							// Skip quote escaping as that will be unescaped later
							if ( escapingSb == null ) {
								escapingSb = new StringBuilder();
							}
							escapingSb.append( string, start, i );
							escapingSb.append( '"' );
							// Move forward to the last quote
							i += ( 1 << ( quotes + 1 ) ) - 1;
							start = i + 1;
							continue;
						}
						assert isDoubleQuote( string, i, 1 << quotes );
						switch ( elementType.getJdbcType().getDefaultSqlTypeCode() ) {
							case SqlTypes.DATE:
								values.add(
										fromRawObject(
												elementType,
												parseDate(
														CharSequenceHelper.subSequence(
																string,
																start,
																i
														)
												),
												options
										)
								);
								break;
							case SqlTypes.TIME:
							case SqlTypes.TIME_WITH_TIMEZONE:
							case SqlTypes.TIME_UTC:
								values.add(
										fromRawObject(
												elementType,
												parseTime(
														CharSequenceHelper.subSequence(
																string,
																start,
																i
														)
												),
												options
										)
								);
								break;
							case SqlTypes.TIMESTAMP:
								values.add(
										fromRawObject(
												elementType,
												parseTimestamp(
														CharSequenceHelper.subSequence(
																string,
																start,
																i
														),
														elementType.getJdbcJavaType()
												),
												options
										)
								);
								break;
							case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
							case SqlTypes.TIMESTAMP_UTC:
								values.add(
										fromRawObject(
												elementType,
												parseTimestampWithTimeZone(
														CharSequenceHelper.subSequence(
																string,
																start,
																i
														),
														elementType.getJdbcJavaType()
												),
												options
										)
								);
								break;
							case SqlTypes.BINARY:
							case SqlTypes.VARBINARY:
							case SqlTypes.LONGVARBINARY:
							case SqlTypes.LONG32VARBINARY:
								final int backslashes = 1 << ( quotes + 1 );
								assert repeatsChar( string, start, backslashes, '\\' );
								final int xCharPosition = start + backslashes;
								assert string.charAt( xCharPosition ) == 'x';
								values.add(
										fromString(
												elementType,
												string,
												xCharPosition + 1,
												i
										)
								);
								break;
							default:
								if ( escapingSb == null || escapingSb.length() == 0 ) {
									values.add(
											fromString(
													elementType,
													string,
													start,
													i
											)
									);
								}
								else {
									escapingSb.append( string, start, i );
									values.add(
											fromString(
													elementType,
													escapingSb,
													0,
													escapingSb.length()
											)
									);
									escapingSb.setLength( 0 );
								}
								break;
						}
						inQuote = false;
						// move forward the index by 2 ^ quotes to point to the next char after the quote
						i += 1 << quotes;
						if ( string.charAt( i ) == '}' ) {
							// Return the end position if this is the last element
							return i + 1;
						}
						// at this point, we must see a comma to indicate the next element
						assert string.charAt( i ) == ',';
					}
					else {
						// This is a start quote, so move forward the index to the last quote
						final int expectedQuotes = 1 << quotes;
						assert isDoubleQuote( string, i, expectedQuotes );
						i += expectedQuotes - 1;
						if ( string.charAt( i + 1 ) == '(' ) {
							// This could be a nested struct
							if ( elementType.getJdbcType() instanceof GaussDBAbstractStructuredJdbcType structJdbcType ) {
								final Object[] subValues = new Object[structJdbcType.embeddableMappingType.getJdbcValueCount()];
								final int subEnd = structJdbcType.deserializeStruct(
										string,
										i + 1,
										quotes + 1,
										subValues,
										returnEmbeddable,
										options
								);
								if ( returnEmbeddable ) {
									final StructAttributeValues attributeValues = structJdbcType.getAttributeValues(
											structJdbcType.embeddableMappingType,
											structJdbcType.orderMapping,
											subValues,
											options
									);
									values.add( instantiate( structJdbcType.embeddableMappingType, attributeValues ) );
								}
								else {
									if ( structJdbcType.inverseOrderMapping != null ) {
										StructHelper.orderJdbcValues(
												structJdbcType.embeddableMappingType,
												structJdbcType.inverseOrderMapping,
												subValues.clone(),
												subValues
										);
									}
									values.add( subValues );
								}
								// The subEnd points to the first character after the '}',
								// so move forward the index to point to the next char after quotes
								assert isDoubleQuote( string, subEnd, expectedQuotes );
								i = subEnd + expectedQuotes;
								if ( string.charAt( i ) == '}' ) {
									// Return the end position if this is the last element
									return i + 1;
								}
								// at this point, we must see a comma to indicate the next element
								assert string.charAt( i ) == ',';
							}
							else {
								inQuote = true;
							}
						}
						else {
							inQuote = true;
						}
					}
					start = i + 1;
					switch ( elementType.getJdbcType().getDefaultSqlTypeCode() ) {
						case SqlTypes.BINARY:
						case SqlTypes.VARBINARY:
						case SqlTypes.LONGVARBINARY:
						case SqlTypes.LONG32VARBINARY:
							// Skip past the backslashes in the binary literal, this will be handled later
							final int backslashes = 1 << ( quotes + 1 );
							assert repeatsChar( string, start, backslashes, '\\' );
							i += backslashes;
							break;
					}
					break;
				case ',':
					if ( !inQuote ) {
						if ( start == i ) {
							values.add( null );
						}
						else {
							if ( elementType.getJdbcType().getDefaultSqlTypeCode() == SqlTypes.BOOLEAN ) {
								values.add(
										fromRawObject(
												elementType,
												string.charAt( start ) == 't',
												options
										)
								);
							}
							else if ( elementType.getJavaTypeDescriptor().getJavaTypeClass().isEnum()
									&& elementType.getJdbcType().isInteger() ) {
								values.add(
										fromRawObject(
												elementType,
												IntegerJavaType.INSTANCE.fromEncodedString( string, start, i ),
												options
										)
								);
							}
							else {
								values.add(
										fromString(
												elementType,
												string,
												start,
												i
										)
								);
							}
						}
						start = i + 1;
					}
					break;
				case '}':
					if ( !inQuote ) {
						if ( start == i ) {
							values.add( null );
						}
						else {
							if ( elementType.getJdbcType().getDefaultSqlTypeCode() == SqlTypes.BOOLEAN ) {
								values.add(
										fromRawObject(
												elementType,
												string.charAt( start ) == 't',
												options
										)
								);
							}
							else if ( elementType.getJavaTypeDescriptor().getJavaTypeClass().isEnum()
									&& elementType.getJdbcType().isInteger() ) {
								values.add(
										fromRawObject(
												elementType,
												IntegerJavaType.INSTANCE.fromEncodedString( string, start, i ),
												options
										)
								);
							}
							else {
								values.add(
										fromString(
												elementType,
												string,
												start,
												i
										)
								);
							}
						}
						return i + 1;
					}
					break;
			}
		}

		throw new IllegalArgumentException( "Array not properly formed: " + string.substring( start ) );
	}

	private SelectableMapping getJdbcValueSelectable(int jdbcValueSelectableIndex) {
		if ( orderMapping != null ) {
			final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
			final int size = numberOfAttributeMappings + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
			int count = 0;
			for ( int i = 0; i < size; i++ ) {
				final ValuedModelPart modelPart = getEmbeddedPart( embeddableMappingType, orderMapping[i] );
				if ( modelPart.getMappedType() instanceof EmbeddableMappingType embeddableMappingType ) {
					final SelectableMapping aggregateMapping = embeddableMappingType.getAggregateMapping();
					if ( aggregateMapping == null ) {
						final SelectableMapping subSelectable = embeddableMappingType.getJdbcValueSelectable( jdbcValueSelectableIndex - count );
						if ( subSelectable != null ) {
							return subSelectable;
						}
						count += embeddableMappingType.getJdbcValueCount();
					}
					else {
						if ( count == jdbcValueSelectableIndex ) {
							return aggregateMapping;
						}
						count++;
					}
				}
				else {
					if ( count == jdbcValueSelectableIndex ) {
						return (SelectableMapping) modelPart;
					}
					count += modelPart.getJdbcTypeCount();
				}
			}
			return null;
		}
		return embeddableMappingType.getJdbcValueSelectable( jdbcValueSelectableIndex );
	}

	private static boolean repeatsChar(String string, int start, int times, char expectedChar) {
		final int end = start + times;
		if ( end < string.length() ) {
			for ( ; start < end; start++ ) {
				if ( string.charAt( start ) != expectedChar ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private static boolean isDoubleQuote(String string, int start, int escapes) {
		if ( escapes == 1 ) {
			return string.charAt( start ) == '"';
		}
		assert ( escapes & 1 ) == 0 : "Only an even number of escapes allowed";
		final int end = start + escapes;
		if ( end < string.length() ) {
			for ( ; start < end; start += 2 ) {
				final char c1 = string.charAt( start );
				final char c2 = string.charAt( start + 1 );
				switch ( c1 ) {
					case '\\':
						// After a backslash, another backslash or a double quote may follow
						if ( c2 != '\\' && c2 != '"' ) {
							return false;
						}
						break;
					case '"':
						// After a double quote, only another double quote may follow
						if ( c2 != '"' ) {
							return false;
						}
						break;
					default:
						return false;
				}
			}
			return string.charAt( end - 1 ) == '"';
		}
		return false;
	}

	private Object fromString(
			int selectableIndex,
			String string,
			int start,
			int end) {
		return fromString(
				getJdbcValueSelectable( selectableIndex ).getJdbcMapping(),
				string,
				start,
				end
		);
	}

	private static Object fromString(JdbcMapping jdbcMapping, CharSequence charSequence, int start, int end) {
		return jdbcMapping.getJdbcJavaType().fromEncodedString(
				charSequence,
				start,
				end
		);
	}

	private static Object fromRawObject(JdbcMapping jdbcMapping, Object raw, WrapperOptions options) {
		return jdbcMapping.getJdbcJavaType().wrap(
				raw,
				options
		);
	}

	private Object parseDate(CharSequence subSequence) {
		return LOCAL_DATE.parse( subSequence, LocalDate::from );
	}

	private Object parseTime(CharSequence subSequence) {
		return DateTimeFormatter.ISO_LOCAL_TIME.parse( subSequence, LocalTime::from );
	}

	private Object parseTimestamp(CharSequence subSequence, JavaType<?> jdbcJavaType) {
		final TemporalAccessor temporalAccessor = LOCAL_DATE_TIME.parse( subSequence );
		final LocalDateTime localDateTime = LocalDateTime.from( temporalAccessor );
		final Timestamp timestamp = Timestamp.valueOf( localDateTime );
		timestamp.setNanos( temporalAccessor.get( ChronoField.NANO_OF_SECOND ) );
		return timestamp;
	}

	private Object parseTimestampWithTimeZone(CharSequence subSequence, JavaType<?> jdbcJavaType) {
		final TemporalAccessor temporalAccessor = LOCAL_DATE_TIME.parse( subSequence );
		if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
			if ( jdbcJavaType.getJavaTypeClass() == Instant.class ) {
				return Instant.from( temporalAccessor );
			}
			else {
				return OffsetDateTime.from( temporalAccessor );
			}
		}
		return LocalDateTime.from( temporalAccessor );
	}

	private static String unescape(CharSequence string, int start, int end) {
		StringBuilder sb = new StringBuilder( end - start );
		for ( int i = start; i < end; i++ ) {
			final char c = string.charAt( i );
			if ( c == '\\' || c == '"' ) {
				i++;
				sb.append( string.charAt( i ) );
				continue;
			}
			sb.append( c );
		}
		return sb.toString();
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		final StringBuilder sb = new StringBuilder();
		serializeStructTo( new PostgreSQLAppender( sb ), domainValue, options );
		return sb.toString();
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		final Object[] array = new Object[embeddableMappingType.getJdbcValueCount()];
		deserializeStruct( getRawStructFromJdbcValue( rawJdbcValue ), 0, 0, array, true, options );
		if ( inverseOrderMapping != null ) {
			StructHelper.orderJdbcValues( embeddableMappingType, inverseOrderMapping, array.clone(), array );
		}
		return array;
	}

	protected String getRawStructFromJdbcValue(Object rawJdbcValue) {
		return rawJdbcValue.toString();
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( value == null ) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		serializeStructTo( new PostgreSQLAppender( sb ), value, options );
		return sb.toString();
	}

	private void serializeStructTo(PostgreSQLAppender appender, Object value, WrapperOptions options) throws SQLException {
		serializeDomainValueTo( appender, options, value, '(' );
		appender.append( ')' );
	}

	private void serializeDomainValueTo(
			PostgreSQLAppender appender,
			WrapperOptions options,
			Object domainValue,
			char separator) throws SQLException {
		serializeJdbcValuesTo(
				appender,
				options,
				StructHelper.getJdbcValues( embeddableMappingType, orderMapping, domainValue, options ),
				separator
		);
	}

	private void serializeJdbcValuesTo(
			PostgreSQLAppender appender,
			WrapperOptions options,
			Object[] jdbcValues,
			char separator) throws SQLException {
		for ( int i = 0; i < jdbcValues.length; i++ ) {
			appender.append( separator );
			separator = ',';
			final Object jdbcValue = jdbcValues[i];
			if ( jdbcValue == null ) {
				continue;
			}
			final SelectableMapping selectableMapping = orderMapping == null ?
					embeddableMappingType.getJdbcValueSelectable( i ) :
					embeddableMappingType.getJdbcValueSelectable( orderMapping[i] );
			final JdbcMapping jdbcMapping = selectableMapping.getJdbcMapping();
			if ( jdbcMapping.getJdbcType() instanceof GaussDBAbstractStructuredJdbcType structJdbcType ) {
				appender.quoteStart();
				structJdbcType.serializeJdbcValuesTo(
						appender,
						options,
						(Object[]) jdbcValue,
						'('
				);
				appender.append( ')' );
				appender.quoteEnd();
			}
			else {
				serializeConvertedBasicTo( appender, options, jdbcMapping, jdbcValue );
			}
		}
	}

	private void serializeConvertedBasicTo(
			PostgreSQLAppender appender,
			WrapperOptions options,
			JdbcMapping jdbcMapping,
			Object subValue) throws SQLException {
		//noinspection unchecked
		final JavaType<Object> jdbcJavaType = (JavaType<Object>) jdbcMapping.getJdbcJavaType();
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( subValue instanceof Boolean booleanValue ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( booleanValue ? '1' : '0' );
					break;
				}
				if ( subValue instanceof Enum<?> enumValue ) {
					appender.appendSql( enumValue.ordinal() );
					break;
				}
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.BIGINT:
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
			case SqlTypes.DURATION:
				appender.append( subValue.toString() );
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( subValue instanceof Boolean booleanValue ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( booleanValue ? 'Y' : 'N' );
					break;
				}
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONG32VARCHAR:
			case SqlTypes.LONG32NVARCHAR:
			case SqlTypes.ENUM:
			case SqlTypes.NAMED_ENUM:
				appender.quoteStart();
				appender.append( (String) subValue );
				appender.quoteEnd();
				break;
			case SqlTypes.DATE:
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
			case SqlTypes.TIMESTAMP:
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				appendTemporal( appender, jdbcMapping, subValue, options );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
				final byte[] bytes = jdbcJavaType.unwrap(
						subValue,
						byte[].class,
						options
				);
				appender.ensureCanFit( appender.quote + 1 + ( bytes.length << 1 ) );
				appender.append( '\\' );
				appender.append( '\\' );
				appender.append( 'x' );
				PrimitiveByteArrayJavaType.INSTANCE.appendString(
						appender,
						bytes
				);
				break;
			case SqlTypes.UUID:
				appender.append( subValue.toString() );
				break;
			case SqlTypes.ARRAY:
				if ( subValue != null ) {
					final int length = Array.getLength( subValue );
					if ( length == 0 ) {
						appender.append( "{}" );
					}
					else {
						//noinspection unchecked
						final BasicType<Object> elementType = ((BasicPluralType<?, Object>) jdbcMapping).getElementType();
						appender.quoteStart();
						appender.append( '{' );
						Object arrayElement = Array.get( subValue, 0 );
						if ( arrayElement == null ) {
							appender.appendNull();
						}
						else {
							serializeConvertedBasicTo( appender, options, elementType, arrayElement );
						}
						for ( int i = 1; i < length; i++ ) {
							arrayElement = Array.get( subValue, i );
							appender.append( ',' );
							if ( arrayElement == null ) {
								appender.appendNull();
							}
							else {
								serializeConvertedBasicTo( appender, options, elementType, arrayElement );
							}
						}

						appender.append( '}' );
						appender.quoteEnd();
					}
				}
				break;
			case SqlTypes.STRUCT:
				if ( subValue != null ) {
					final GaussDBAbstractStructuredJdbcType structJdbcType = (GaussDBAbstractStructuredJdbcType) jdbcMapping.getJdbcType();
					appender.quoteStart();
					structJdbcType.serializeJdbcValuesTo( appender, options, (Object[]) subValue, '(' );
					appender.append( ')' );
					appender.quoteEnd();
				}
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in struct: " + jdbcMapping.getJdbcType() );
		}
	}

	private StructAttributeValues getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final int size = numberOfAttributeMappings + ( embeddableMappingType.isPolymorphic() ? 1 : 0 );
		final StructAttributeValues attributeValues = new StructAttributeValues(
				numberOfAttributeMappings,
				orderMapping != null ?
						null :
						rawJdbcValues
		);
		int jdbcIndex = 0;
		for ( int i = 0; i < size; i++ ) {
			final int attributeIndex;
			if ( orderMapping == null ) {
				attributeIndex = i;
			}
			else {
				attributeIndex = orderMapping[i];
			}
			jdbcIndex += injectAttributeValue(
					getEmbeddedPart( embeddableMappingType, attributeIndex ),
					attributeValues,
					attributeIndex,
					rawJdbcValues,
					jdbcIndex,
					options
			);
		}
		return attributeValues;
	}

	private int injectAttributeValue(
			ValuedModelPart modelPart,
			StructAttributeValues attributeValues,
			int attributeIndex,
			Object[] rawJdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final MappingType mappedType = modelPart.getMappedType();
		final int jdbcValueCount;
		final Object rawJdbcValue = rawJdbcValues[jdbcIndex];
		if ( mappedType instanceof EmbeddableMappingType embeddableMappingType ) {
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
				attributeValues.setAttributeValue( attributeIndex, rawJdbcValue );
			}
			else {
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final Object[] subJdbcValues = new Object[jdbcValueCount];
				System.arraycopy( rawJdbcValues, jdbcIndex, subJdbcValues, 0, subJdbcValues.length );
				final StructAttributeValues subValues = getAttributeValues(
						embeddableMappingType,
						null,
						subJdbcValues,
						options
				);
				attributeValues.setAttributeValue( attributeIndex, instantiate( embeddableMappingType, subValues ) );
			}
		}
		else {
			assert modelPart.getJdbcTypeCount() == 1;
			jdbcValueCount = 1;
			final JdbcMapping jdbcMapping = modelPart.getSingleJdbcMapping();
			final Object jdbcValue = jdbcMapping.getJdbcJavaType().wrap(
					rawJdbcValue,
					options
			);
			attributeValues.setAttributeValue( attributeIndex, jdbcMapping.convertToDomainValue( jdbcValue ) );
		}
		return jdbcValueCount;
	}

	private void appendTemporal(SqlAppender appender, JdbcMapping jdbcMapping, Object value, WrapperOptions options) {
		final TimeZone jdbcTimeZone = getJdbcTimeZone( options );
		//noinspection unchecked
		final JavaType<Object> javaType = (JavaType<Object>) jdbcMapping.getJdbcJavaType();
		appender.append( '"' );
		switch ( jdbcMapping.getJdbcType().getJdbcTypeCode() ) {
			case SqlTypes.DATE:
				if ( value instanceof java.util.Date date ) {
					appendAsDate( appender, date );
				}
				else if ( value instanceof java.util.Calendar calendar ) {
					appendAsDate( appender, calendar );
				}
				else if ( value instanceof TemporalAccessor temporalAccessor ) {
					appendAsDate( appender, temporalAccessor );
				}
				else {
					appendAsDate(
							appender,
							javaType.unwrap( value, java.util.Date.class, options )
					);
				}
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				if ( value instanceof java.util.Date date ) {
					appendAsTime( appender, date, jdbcTimeZone );
				}
				else if ( value instanceof java.util.Calendar calendar ) {
					appendAsTime( appender, calendar, jdbcTimeZone );
				}
				else if ( value instanceof TemporalAccessor temporalAccessor ) {
					if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
						appendAsTime( appender, temporalAccessor, true, jdbcTimeZone );
					}
					else {
						appendAsLocalTime( appender, temporalAccessor );
					}
				}
				else {
					appendAsTime(
							appender,
							javaType.unwrap( value, java.sql.Time.class, options ),
							jdbcTimeZone
					);
				}
				break;
			case SqlTypes.TIMESTAMP:
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				if ( value instanceof java.util.Date date ) {
					appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				}
				else if ( value instanceof java.util.Calendar calendar ) {
					appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				}
				else if ( value instanceof TemporalAccessor temporalAccessor ) {
					appendAsTimestampWithMicros( appender, temporalAccessor, temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ), jdbcTimeZone );
				}
				else {
					appendAsTimestampWithMicros(
							appender,
							javaType.unwrap( value, java.util.Date.class, options ),
							jdbcTimeZone
					);
				}
				break;
			default:
				throw new IllegalArgumentException();
		}

		appender.append( '"' );
	}
	private static TimeZone getJdbcTimeZone(WrapperOptions options) {
		return options == null || options.getJdbcTimeZone() == null
				? TimeZone.getDefault()
				: options.getJdbcTimeZone();
	}

	protected <X> Object getBindValue(X value, WrapperOptions options) throws SQLException {
		return StructHelper.getJdbcValues( embeddableMappingType, orderMapping, value, options );
	}

	private static class PostgreSQLAppender extends StringBuilderSqlAppender {

		private int quote = 1;

		public PostgreSQLAppender(StringBuilder sb) {
			super( sb );
		}

		public void quoteStart() {
			append( '"' );
			quote = quote << 1;
		}

		public void quoteEnd() {
			quote = quote >> 1;
			append( '"' );
		}

		public void appendNull() {
			sb.append( "NULL" );
		}

		@Override
		public PostgreSQLAppender append(char fragment) {
			if ( quote != 1 ) {
				appendWithQuote( fragment );
			}
			else {
				sb.append( fragment );
			}
			return this;
		}

		@Override
		public PostgreSQLAppender append(CharSequence csq) {
			return append( csq, 0, csq.length() );
		}

		@Override
		public PostgreSQLAppender append(CharSequence csq, int start, int end) {
			if ( quote != 1 ) {
				int len = end - start;
				sb.ensureCapacity( sb.length() + len );
				for ( int i = start; i < end; i++ ) {
					appendWithQuote( csq.charAt( i ) );
				}
			}
			else {
				sb.append( csq, start, end );
			}
			return this;
		}

		private void appendWithQuote(char fragment) {
			if ( fragment == '"' || fragment == '\\' ) {
				sb.ensureCapacity( sb.length() + quote );
				for ( int i = 1; i < quote; i++ ) {
					sb.append( '\\' );
				}
			}
			sb.append( fragment );
		}

		public void ensureCanFit(int lengthIncrease) {
			sb.ensureCapacity( sb.length() + lengthIncrease );
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

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
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.IntegerJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

/**
 * Implementation for serializing/deserializing an embeddable aggregate to/from the PostgreSQL component format.
 * For regular queries, we select the individual struct elements because the PostgreSQL component format encoding
 * is probably not very efficient.
 *
 * @author Christian Beikov
 */
public abstract class AbstractPostgreSQLStructJdbcType implements AggregateJdbcType {

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

	protected AbstractPostgreSQLStructJdbcType(
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

	public String getTypeName() {
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
				return ( (AbstractPostgreSQLStructJdbcType) getJdbcType() ).fromString(
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
			array = new Object[embeddableMappingType.getJdbcValueCount()];
			end = deserializeStruct( string, 0, 0, array, returnEmbeddable, options );
		}
		assert end == string.length();
		if ( returnEmbeddable ) {
			final Object[] attributeValues = getAttributeValues( embeddableMappingType, orderMapping, array, options );
			//noinspection unchecked
			return (X) embeddableMappingType.getRepresentationStrategy().getInstantiator().instantiate(
					() -> attributeValues,
					options.getSessionFactory()
			);
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
			int quoteLevel,
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
				case '"':
					if ( inQuote ) {
						if ( repeatsChar( string, i, 1 << ( quoteLevel + 1 ), '"' ) ) {
							// Skip quote escaping as that will be unescaped later
							if ( escapingSb == null ) {
								escapingSb = new StringBuilder();
							}
							escapingSb.append( string, start, i );
							escapingSb.append( '"' );
							// Move forward to the last quote
							i += ( 1 << ( quoteLevel + 1 ) ) - 1;
							start = i + 1;
							continue;
						}
						assert repeatsChar( string, i, 1 << quoteLevel, '"' );
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
								final int backslashes = 1 << ( quoteLevel + 1 );
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
						i += 1 << quoteLevel;
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
						final int expectedQuotes = Math.max( 1, 1 << quoteLevel );
						assert repeatsChar( string, i, expectedQuotes, '"' );
						i += expectedQuotes - 1;
						if ( string.charAt( i + 1 ) == '(' ) {
							// This could be a nested struct
							final JdbcMapping jdbcMapping = getJdbcValueSelectable( column ).getJdbcMapping();
							if ( jdbcMapping.getJdbcType() instanceof AbstractPostgreSQLStructJdbcType ) {
								final AbstractPostgreSQLStructJdbcType structJdbcType;
								structJdbcType = (AbstractPostgreSQLStructJdbcType) jdbcMapping.getJdbcType();
								final Object[] subValues = new Object[structJdbcType.embeddableMappingType.getJdbcValueCount()];
								final int subEnd = structJdbcType.deserializeStruct(
										string,
										i + 1,
										quoteLevel + 1,
										subValues,
										returnEmbeddable,
										options
								);
								if ( returnEmbeddable ) {
									final Object[] attributeValues = structJdbcType.getAttributeValues(
											structJdbcType.embeddableMappingType,
											structJdbcType.orderMapping,
											subValues,
											options
									);
									final Object subValue = structJdbcType.embeddableMappingType.getRepresentationStrategy()
											.getInstantiator()
											.instantiate( () -> attributeValues, options.getSessionFactory() );
									values[column] = subValue;
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
								assert repeatsChar( string, subEnd, expectedQuotes, '"' );
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
								values[column] =  fromRawObject(
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
								values[column] = fromString(
										column,
										string,
										start,
										i
								);
							}
						}
						return i + 1;
					}
					break;
			}
		}

		throw new IllegalArgumentException( "Struct not properly formed: " + string.substring( start ) );
	}

	private SelectableMapping getJdbcValueSelectable(int jdbcValueSelectableIndex) {
		if ( orderMapping != null ) {
			final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
			int count = 0;
			for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
				final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( orderMapping[i] );
				final MappingType mappedType = attributeMapping.getMappedType();
				if ( mappedType instanceof EmbeddableMappingType ) {
					final EmbeddableMappingType embeddableMappingType = (EmbeddableMappingType) mappedType;
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
						return (SelectableMapping) attributeMapping;
					}
					count += attributeMapping.getJdbcTypeCount();
				}
			}
			return null;
		}
		return embeddableMappingType.getJdbcValueSelectable( jdbcValueSelectableIndex );
	}

	private static boolean repeatsChar(String string, int start, int times, char c) {
		final int end = start + times;
		if ( end < string.length() ) {
			for ( ; start < end; start++ ) {
				if ( string.charAt( start ) != c ) {
					return false;
				}
			}
			return true;
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
		deserializeStruct( (String) rawJdbcValue, 0, 0, array, true, options );
		if ( inverseOrderMapping != null ) {
			StructHelper.orderJdbcValues( embeddableMappingType, inverseOrderMapping, array.clone(), array );
		}
		return array;
	}

	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		serializeStructTo( new PostgreSQLAppender( sb ), value, options );
		return sb.toString();
	}

	private void serializeStructTo(PostgreSQLAppender appender, Object value, WrapperOptions options) {
		final Object[] array = embeddableMappingType.getValues( value );
		serializeValuesTo( appender, options, embeddableMappingType, array, '(' );
		appender.append( ')' );
	}

	private void serializeValuesTo(
			PostgreSQLAppender appender,
			WrapperOptions options,
			EmbeddableMappingType embeddableMappingType,
			Object[] array,
			char separator) {
		final int end = embeddableMappingType.getNumberOfAttributeMappings();
		for ( int i = 0; i < end; i++ ) {
			final AttributeMapping attributeMapping;
			final Object attributeValue;
			if ( orderMapping == null ) {
				attributeMapping = embeddableMappingType.getAttributeMapping( i );
				attributeValue = array == null ? null : array[i];
			}
			else {
				attributeMapping = embeddableMappingType.getAttributeMapping( orderMapping[i] );
				attributeValue = array == null ? null : array[orderMapping[i]];
			}
			if ( attributeMapping instanceof BasicValuedMapping ) {
				appender.append( separator );
				separator = ',';
				if ( attributeValue == null ) {
					continue;
				}
				final JdbcMapping jdbcMapping = ( (BasicValuedMapping) attributeMapping ).getJdbcMapping();
				serializeBasicTo( appender, options, jdbcMapping, attributeValue );
			}
			else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if ( aggregateMapping == null ) {
					serializeValuesTo(
							appender,
							options,
							mappingType,
							attributeValue == null ? null : mappingType.getValues( attributeValue ),
							separator
					);
					separator = ',';
				}
				else {
					appender.append( separator );
					separator = ',';
					if ( attributeValue == null ) {
						continue;
					}
					appender.quoteStart();
					( (AbstractPostgreSQLStructJdbcType) aggregateMapping.getJdbcMapping().getJdbcType() ).serializeStructTo(
							appender,
							attributeValue,
							options
					);
					appender.quoteEnd();
				}
			}
			else {
				throw new UnsupportedOperationException( "Unsupported attribute mapping: " + attributeMapping );
			}
		}
	}

	private void serializeBasicTo(
			PostgreSQLAppender appender,
			WrapperOptions options,
			JdbcMapping jdbcMapping,
			Object array) {
		//noinspection unchecked
		final JavaType<Object> jdbcJavaType = (JavaType<Object>) jdbcMapping.getJdbcJavaType();
		final Object subValue = jdbcMapping.convertToRelationalValue( array );
		switch ( jdbcMapping.getJdbcType().getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( subValue instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( (Boolean) subValue ? '1' : '0' );
					break;
				}
				if ( subValue instanceof Enum ) {
					appender.appendSql( ((Enum<?>) subValue).ordinal() );
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
				jdbcJavaType.appendEncodedString(
						appender,
						jdbcJavaType.unwrap(
								subValue,
								jdbcJavaType.getJavaTypeClass(),
								options
						)
				);
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( subValue instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( (Boolean) subValue ? 'Y' : 'N' );
					break;
				}
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONG32VARCHAR:
			case SqlTypes.LONG32NVARCHAR:
			case SqlTypes.ENUM:
			case SqlTypes.NAMED_ENUM:
				appender.quoteStart();
				jdbcJavaType.appendEncodedString(
						appender,
						jdbcJavaType.unwrap(
								subValue,
								jdbcJavaType.getJavaTypeClass(),
								options
						)
				);
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
				final int escapes = 1 << appender.quote;
				appender.ensureCanFit( escapes + 1 + ( bytes.length << 1 ) );
				for ( int i = 0; i < escapes; i++ ) {
					appender.append( '\\' );
				}
				appender.append( 'x' );
				PrimitiveByteArrayJavaType.INSTANCE.appendString(
						appender,
						bytes
				);
				break;
			case SqlTypes.UUID:
				appender.append( subValue.toString() );
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in struct: " + jdbcMapping.getJdbcType() );
		}
	}

	private Object[] getAttributeValues(
			EmbeddableMappingType embeddableMappingType,
			int[] orderMapping,
			Object[] rawJdbcValues,
			WrapperOptions options) throws SQLException {
		final int numberOfAttributeMappings = embeddableMappingType.getNumberOfAttributeMappings();
		final Object[] attributeValues;
		if ( numberOfAttributeMappings != rawJdbcValues.length || orderMapping != null ) {
			attributeValues = new Object[numberOfAttributeMappings];
		}
		else {
			attributeValues = rawJdbcValues;
		}
		int jdbcIndex = 0;
		for ( int i = 0; i < numberOfAttributeMappings; i++ ) {
			final int attributeIndex;
			if ( orderMapping == null ) {
				attributeIndex = i;
			}
			else {
				attributeIndex = orderMapping[i];
			}
			final AttributeMapping attributeMapping = embeddableMappingType.getAttributeMapping( attributeIndex );
			jdbcIndex += injectAttributeValue(
					attributeMapping,
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
			AttributeMapping attributeMapping,
			Object[] attributeValues,
			int attributeIndex,
			Object[] rawJdbcValues,
			int jdbcIndex,
			WrapperOptions options) throws SQLException {
		final MappingType mappedType = attributeMapping.getMappedType();
		final int jdbcValueCount;
		final Object rawJdbcValue = rawJdbcValues[jdbcIndex];
		if ( mappedType instanceof EmbeddableMappingType ) {
			final EmbeddableMappingType embeddableMappingType = (EmbeddableMappingType) mappedType;
			if ( embeddableMappingType.getAggregateMapping() != null ) {
				jdbcValueCount = 1;
				attributeValues[attributeIndex] = rawJdbcValue;
			}
			else {
				jdbcValueCount = embeddableMappingType.getJdbcValueCount();
				final Object[] subJdbcValues = new Object[jdbcValueCount];
				System.arraycopy( rawJdbcValues, jdbcIndex, subJdbcValues, 0, subJdbcValues.length );
				final Object[] subValues = getAttributeValues( embeddableMappingType, null, subJdbcValues, options );
				attributeValues[attributeIndex] = embeddableMappingType.getRepresentationStrategy()
						.getInstantiator()
						.instantiate(
								() -> subValues,
								embeddableMappingType.findContainingEntityMapping()
										.getEntityPersister()
										.getFactory()
						);
			}
		}
		else {
			assert attributeMapping.getJdbcTypeCount() == 1;
			jdbcValueCount = 1;
			final JdbcMapping jdbcMapping = attributeMapping.getSingleJdbcMapping();
			final Object jdbcValue = jdbcMapping.getJdbcJavaType().wrap(
					rawJdbcValue,
					options
			);
			attributeValues[attributeIndex] = jdbcMapping.convertToDomainValue( jdbcValue );
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
				if ( value instanceof java.util.Date ) {
					appendAsDate( appender, (java.util.Date) value );
				}
				else if ( value instanceof java.util.Calendar ) {
					appendAsDate( appender, (java.util.Calendar) value );
				}
				else if ( value instanceof TemporalAccessor ) {
					appendAsDate( appender, (TemporalAccessor) value );
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
				if ( value instanceof java.util.Date ) {
					appendAsTime( appender, (java.util.Date) value, jdbcTimeZone );
				}
				else if ( value instanceof java.util.Calendar ) {
					appendAsTime( appender, (java.util.Calendar) value, jdbcTimeZone );
				}
				else if ( value instanceof TemporalAccessor ) {
					final TemporalAccessor temporalAccessor = (TemporalAccessor) value;
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
				if ( value instanceof java.util.Date ) {
					appendAsTimestampWithMicros( appender, (java.util.Date) value, jdbcTimeZone );
				}
				else if ( value instanceof java.util.Calendar ) {
					appendAsTimestampWithMillis( appender, (java.util.Calendar) value, jdbcTimeZone );
				}
				else if ( value instanceof TemporalAccessor ) {
					final TemporalAccessor temporalAccessor = (TemporalAccessor) value;
					if ( temporalAccessor.isSupported( ChronoField.OFFSET_SECONDS ) ) {
						appendAsTimestampWithMicros( appender, temporalAccessor, true, jdbcTimeZone );
					}
					else {
						appendAsTimestampWithMicros( appender, temporalAccessor, false, jdbcTimeZone );
					}
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
			if ( fragment == '"' ) {
				sb.ensureCapacity( sb.length() + quote );
				for ( int i = 0; i < quote; i++ ) {
					sb.append( '"' );
				}
			}
			else {
				sb.append( fragment );
			}
		}

		public void ensureCanFit(int lengthIncrease) {
			sb.ensureCapacity( sb.length() + lengthIncrease );
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.hibernate.Internal;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.CharSequenceHelper;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.format.JsonDocumentWriter;
import static org.hibernate.type.descriptor.jdbc.StructHelper.getEmbeddedPart;
import static org.hibernate.type.descriptor.jdbc.StructHelper.instantiate;

/**
 * A Helper for serializing and deserializing JSON, based on an {@link org.hibernate.metamodel.mapping.EmbeddableMappingType}.
 */
@Internal
public class JsonHelper {

	public static String toString(EmbeddableMappingType embeddableMappingType, Object value, WrapperOptions options) {
		if ( value == null ) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		toString( embeddableMappingType, value, options, new JsonAppender( sb ) );
		return sb.toString();
	}


	public static void serializeArray(MappingType elementMappingType, Object[] values, WrapperOptions options, JsonDocumentWriter writer) {
		writer.startArray();
		if ( values.length == 0 ) {
			writer.endArray();
			return;
		}
		for ( Object value : values ) {
			try {
				if (value == null) {
					writer.nullValue();
				}
				else {
					JsonHelper.serialize( (EmbeddableMappingType) elementMappingType, value, options, writer );
				}
			}
			catch (IOException e) {
				// TODO : do better than this
				throw new RuntimeException( e );
			}
		}
		writer.endArray();
	}

	public static void serializeArray(JavaType<?> elementJavaType, JdbcType elementJdbcType, Object[] values, WrapperOptions options, JsonDocumentWriter writer) {
		writer.startArray();
		if ( values.length == 0 ) {
			writer.endArray();
			return;
		}
		for ( Object value : values ) {
			if (value == null) {
				writer.nullValue();
			}
			else {
				writer.serializeJsonValue( value ,(JavaType<Object>) elementJavaType,elementJdbcType,options);
			}
		}
		writer.endArray();
	}

	/**
	 * Checks that a <code>JDBCType</code> is assignable to an array
	 * @param type the jdbc type
	 * @return <code>true</code> if types is of array kind <code>false</code> otherwise.
	 */
	private static boolean isArrayType(JdbcType type) {
		return (type.getDefaultSqlTypeCode() == SqlTypes.ARRAY ||
				type.getDefaultSqlTypeCode() == SqlTypes.JSON_ARRAY);
	}

	/**
	 * Serialized an Object value to JSON object using a document writer.
	 *
	 * @param embeddableMappingType the embeddable mapping definition of the given value.
	 * @param domainValue the value to be serialized.
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException if the underlying writer failed to serialize a mpped value or failed to perform need I/O.
	 */
	public static void serialize(EmbeddableMappingType embeddableMappingType,
										Object domainValue, WrapperOptions options, JsonDocumentWriter writer) throws IOException {
		writer.startObject();
		serializeMapping(embeddableMappingType, domainValue, options, writer);
		writer.endObject();
	}

	/**
	 * JSON object attirbute serialization
	 * @see #serialize(EmbeddableMappingType, Object, WrapperOptions, JsonDocumentWriter)
	 * @param embeddableMappingType the embeddable mapping definition of the given value.
	 * @param domainValue the value to be serialized.
	 * @param options wrapping options
	 * @param writer the document writer
	 * @throws IOException
	 */
	private static void serializeMapping(EmbeddableMappingType embeddableMappingType,
								Object domainValue, WrapperOptions options, JsonDocumentWriter writer) throws IOException {
		final Object[] values = embeddableMappingType.getValues( domainValue );
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping ) {
				final String name = ( (SelectableMapping) attributeMapping ).getSelectableName();
				writer.objectKey( name );
				if (values[i] == null) {
					writer.nullValue();
				}
				else if (attributeMapping.getMappedType() instanceof BasicType<?>) {
					final BasicType<Object> basicType = (BasicType<Object>) attributeMapping.getMappedType();
					if ( isArrayType(basicType.getJdbcType())) {
						final int length = Array.getLength( values[i] );
						writer.startArray();
						if ( length != 0 ) {
							//noinspection unchecked
							final JavaType<Object> elementJavaType = ( (BasicPluralJavaType<Object>) basicType.getJdbcJavaType() ).getElementJavaType();
							final JdbcType elementJdbcType = ( (ArrayJdbcType) basicType.getJdbcType() ).getElementJdbcType();
							final Object domainArray = basicType.convertToRelationalValue(   values[i] );
							for ( int j = 0; j < length; j++ ) {
								writer.serializeJsonValue(Array.get(domainArray,j), elementJavaType, elementJdbcType, options);
							}
						}
						writer.endArray();
					}
					else {
						writer.serializeJsonValue(basicType.convertToRelationalValue( values[i]),
								(JavaType<Object>)basicType.getJdbcJavaType(),basicType.getJdbcType(), options);
					}
				}
				else if ( attributeMapping.getMappedType() instanceof EmbeddableMappingType ) {
					writer.startObject();
					serializeMapping(  (EmbeddableMappingType)attributeMapping.getMappedType(), values[i], options,writer);
					writer.endObject();
				}

			}
			else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
				if ( values[i] == null ) {
					//writer.nullValue();
					continue;
				}
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if (aggregateMapping == null) {
					serializeMapping(
							mappingType,
							values[i],
							options,
							writer );
				}
				else {
					final String name = aggregateMapping.getSelectableName();
					writer.objectKey( name );
					writer.startObject();
					serializeMapping(
							mappingType,
							values[i],
							options,
							writer);
					writer.endObject();

				}
			}
			else {
				throw new UnsupportedOperationException( "Support for attribute mapping type not yet implemented: " + attributeMapping.getClass().getName() );
			}

		}
	}

	private static void toString(EmbeddableMappingType embeddableMappingType, Object value, WrapperOptions options, JsonAppender appender) {
		toString( embeddableMappingType, options, appender, value, '{' );
		appender.append( '}' );
	}

	private static void toString(
			EmbeddableMappingType embeddableMappingType,
			WrapperOptions options,
			JsonAppender appender,
			Object domainValue,
			char separator) {
		final Object[] values = embeddableMappingType.getValues( domainValue );
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping selectableMapping ) {
				final String name = selectableMapping.getSelectableName();
				appender.append( separator );
				appender.append( '"' );
				appender.append( name );
				appender.append( "\":" );
				toString( attributeMapping.getMappedType(), values[i], options, appender );
			}
			else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
				if ( values[i] == null ) {
					// Skipping the update of the separator is on purpose
					continue;
				}
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if ( mappingType.shouldSelectAggregateMapping()) {
					final String name = aggregateMapping.getSelectableName();
					appender.append( separator );
					appender.append( '"' );
					appender.append( name );
					appender.append( "\":" );
					toString( mappingType, values[i], options, appender );
				}
				else {
					toString(
							mappingType,
							options,
							appender,
							values[i],
							separator
					);
				}
			}
			else {
				throw new UnsupportedOperationException( "Support for attribute mapping type not yet implemented: " + attributeMapping.getClass().getName() );
			}
			separator = ',';
		}
	}

	private static void toString(MappingType mappedType, Object value, WrapperOptions options, JsonAppender appender) {
		if ( value == null ) {
			appender.append( "null" );
		}
		else if ( mappedType instanceof EmbeddableMappingType embeddableMappingType ) {
			toString( embeddableMappingType, value, options, appender );
		}
		else if ( mappedType instanceof BasicType<?> ) {
			//noinspection unchecked
			final BasicType<Object> basicType = (BasicType<Object>) mappedType;
			convertedBasicValueToString( basicType.convertToRelationalValue( value ), options, appender, basicType );
		}
		else {
			throw new UnsupportedOperationException( "Support for mapping type not yet implemented: " + mappedType.getClass().getName() );
		}
	}

	private static void convertedValueToString(
			JavaType<Object> javaType,
			JdbcType jdbcType,
			Object value,
			WrapperOptions options,
			JsonAppender appender) {
		if ( value == null ) {
			appender.append( "null" );
		}
		else if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
			toString( aggregateJdbcType.getEmbeddableMappingType(), value, options, appender );
		}
		else {
			convertedBasicValueToString( value, options, appender, javaType, jdbcType );
		}
	}


	private static void convertedBasicValueToString(
			Object value,
			WrapperOptions options,
			JsonAppender appender,
			BasicType<Object> basicType) {
		//noinspection unchecked
		convertedBasicValueToString(
				value,
				options,
				appender,
				(JavaType<Object>) basicType.getJdbcJavaType(),
				basicType.getJdbcType()
		);
	}

	private static void convertedBasicValueToString(
			Object value,
			WrapperOptions options,
			JsonAppender appender,
			JavaType<Object> javaType,
			JdbcType jdbcType) {
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( value instanceof Boolean booleanValue ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( booleanValue ? '1' : '0' );
					break;
				}
				if ( value instanceof Enum<?> enumValue ) {
					appender.appendSql( enumValue.ordinal() );
					break;
				}
			case SqlTypes.BOOLEAN:
			case SqlTypes.BIT:
			case SqlTypes.BIGINT:
			case SqlTypes.FLOAT:
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
				// These types fit into the native representation of JSON, so let's use that
				javaType.appendEncodedString( appender, value );
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( value instanceof Boolean booleanValue ) {
					// BooleanJavaType has this as an implicit conversion
					appender.append( '"' );
					appender.append( booleanValue ? 'Y' : 'N' );
					appender.append( '"' );
					break;
				}
			case SqlTypes.LONGVARCHAR:
			case SqlTypes.LONGNVARCHAR:
			case SqlTypes.LONG32VARCHAR:
			case SqlTypes.LONG32NVARCHAR:
			case SqlTypes.CLOB:
			case SqlTypes.MATERIALIZED_CLOB:
			case SqlTypes.NCLOB:
			case SqlTypes.MATERIALIZED_NCLOB:
			case SqlTypes.ENUM:
			case SqlTypes.NAMED_ENUM:
				// These literals can contain the '"' character, so we need to escape it
				appender.append( '"' );
				appender.startEscaping();
				javaType.appendEncodedString( appender, value );
				appender.endEscaping();
				appender.append( '"' );
				break;
			case SqlTypes.DATE:
				appender.append( '"' );
				JdbcDateJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( value, java.sql.Date.class, options )
				);
				appender.append( '"' );
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				appender.append( '"' );
				JdbcTimeJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( value, java.sql.Time.class, options )
				);
				appender.append( '"' );
				break;
			case SqlTypes.TIMESTAMP:
				appender.append( '"' );
				JdbcTimestampJavaType.INSTANCE.appendEncodedString(
						appender,
						javaType.unwrap( value, java.sql.Timestamp.class, options )
				);
				appender.append( '"' );
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				appender.append( '"' );
				DateTimeFormatter.ISO_OFFSET_DATE_TIME.formatTo(
						javaType.unwrap( value, OffsetDateTime.class, options ),
						appender
				);
				appender.append( '"' );
				break;
			case SqlTypes.DECIMAL:
			case SqlTypes.NUMERIC:
			case SqlTypes.DURATION:
			case SqlTypes.UUID:
				// These types need to be serialized as JSON string, but don't have a need for escaping
				appender.append( '"' );
				javaType.appendEncodedString( appender, value );
				appender.append( '"' );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				// These types need to be serialized as JSON string, and for efficiency uses appendString directly
				appender.append( '"' );
				appender.write( javaType.unwrap( value, byte[].class, options ) );
				appender.append( '"' );
				break;
			case SqlTypes.ARRAY:
			case SqlTypes.JSON_ARRAY:
				final int length = Array.getLength( value );
				appender.append( '[' );
				if ( length != 0 ) {
					//noinspection unchecked
					final JavaType<Object> elementJavaType = ( (BasicPluralJavaType<Object>) javaType ).getElementJavaType();
					final JdbcType elementJdbcType = ( (ArrayJdbcType) jdbcType ).getElementJdbcType();
					Object arrayElement = Array.get( value, 0 );
					convertedValueToString( elementJavaType, elementJdbcType, arrayElement, options, appender );
					for ( int i = 1; i < length; i++ ) {
						arrayElement = Array.get( value, i );
						appender.append( ',' );
						convertedValueToString( elementJavaType, elementJdbcType, arrayElement, options, appender );
					}
				}
				appender.append( ']' );
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in JSON: " + jdbcType );
		}
	}

	public static <X> X fromString(
			EmbeddableMappingType embeddableMappingType,
			String string,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		if ( string == null ) {
			return null;
		}

		final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
		final Object[] values = new Object[jdbcValueCount + ( embeddableMappingType.isPolymorphic() ? 1 : 0 )];
		final int end = fromString( embeddableMappingType, string, 0, string.length(), values, returnEmbeddable, options );
		assert string.substring( end ).isBlank();
		if ( returnEmbeddable ) {
			final StructAttributeValues attributeValues = StructHelper.getAttributeValues(
					embeddableMappingType,
					values,
					options
			);
			//noinspection unchecked
			return (X) instantiate( embeddableMappingType, attributeValues );
		}
		//noinspection unchecked
		return (X) values;
	}

	// This is also used by Hibernate Reactive
	public static <X> X arrayFromString(
			JavaType<X> javaType,
			JdbcType elementJdbcType,
			String string,
			WrapperOptions options) throws SQLException {
		if ( string == null ) {
			return null;
		}
		final JavaType<?> elementJavaType = ((BasicPluralJavaType<?>) javaType).getElementJavaType();
		final Class<?> preferredJavaTypeClass = elementJdbcType.getPreferredJavaTypeClass( options );
		final JavaType<?> jdbcJavaType;
		if ( preferredJavaTypeClass == null || preferredJavaTypeClass == elementJavaType.getJavaTypeClass() ) {
			jdbcJavaType = elementJavaType;
		}
		else {
			jdbcJavaType = options.getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( preferredJavaTypeClass );
		}
		final CustomArrayList arrayList = new CustomArrayList();
		final int i = fromArrayString(
				string,
				false,
				options,
				0,
				arrayList,
				elementJavaType,
				jdbcJavaType,
				elementJdbcType
		);
		assert string.charAt( i - 1 ) == ']';
		return javaType.wrap( arrayList, options );
	}

	private static int fromString(
			EmbeddableMappingType embeddableMappingType,
			String string,
			int begin,
			int end,
			Object[] values,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		boolean hasEscape = false;
		assert string.charAt( begin ) == '{';
		int start = begin + 1;
		State s = State.KEY_START;
		int selectableIndex = -1;
		// The following parsing logic assumes JSON is well-formed,
		// but for the sake of the Java compiler's flow analysis
		// and hopefully also for a better understanding, contains throws for some syntax errors
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '\\':
					assert s == State.KEY_QUOTE || s == State.VALUE_QUOTE;
					hasEscape = true;
					i++;
					break;
				case '"':
					switch ( s ) {
						case KEY_START:
							s = State.KEY_QUOTE;
							selectableIndex = -1;
							start = i + 1;
							hasEscape = false;
							break;
						case KEY_QUOTE:
							s = State.KEY_END;
							selectableIndex = getSelectableMapping(
									embeddableMappingType,
									string,
									start,
									i,
									hasEscape
							);
							start = -1;
							hasEscape = false;
							break;
						case VALUE_START:
							s = State.VALUE_QUOTE;
							start = i + 1;
							hasEscape = false;
							break;
						case VALUE_QUOTE:
							s = State.VALUE_END;
							values[selectableIndex] = fromString(
									embeddableMappingType.getJdbcValueSelectable( selectableIndex ).getJdbcMapping(),
									string,
									start,
									i,
									hasEscape,
									returnEmbeddable,
									options
							);
							selectableIndex = -1;
							start = -1;
							hasEscape = false;
							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case ':':
					switch ( s ) {
						case KEY_QUOTE:
							// I guess it's ok to have a ':' in the key..
						case VALUE_QUOTE:
							// In the value it's fine
							break;
						case KEY_END:
							s = State.VALUE_START;
							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case ',':
					switch ( s ) {
						case KEY_QUOTE:
							// I guess it's ok to have a ',' in the key..
						case VALUE_QUOTE:
							// In the value it's fine
							break;
						case VALUE_END:
							s = State.KEY_START;
							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case '{':
					switch ( s ) {
						case KEY_QUOTE:
							// I guess it's ok to have a '{' in the key..
						case VALUE_QUOTE:
							// In the value it's fine
							break;
						case VALUE_START:
							final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable(
									selectableIndex
							);
							if ( !( selectable.getJdbcMapping().getJdbcType()
									instanceof AggregateJdbcType aggregateJdbcType) ) {
								throw new IllegalArgumentException(
										String.format(
												"JSON starts sub-object for a non-aggregate type at index %d. Selectable [%s] is of type [%s]",
												i,
												selectable.getSelectableName(),
												selectable.getJdbcMapping().getJdbcType().getClass().getName()
										)
								);
							}
							final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
							// This encoding is only possible if the JDBC type is JSON again
							assert aggregateJdbcType.getJdbcTypeCode() == SqlTypes.JSON
									|| aggregateJdbcType.getDefaultSqlTypeCode() == SqlTypes.JSON;
							final Object[] subValues = new Object[subMappingType.getJdbcValueCount()];
							i = fromString( subMappingType, string, i, end, subValues, returnEmbeddable, options ) - 1;
							assert string.charAt( i ) == '}';
							if ( returnEmbeddable ) {
								final StructAttributeValues attributeValues = StructHelper.getAttributeValues(
										subMappingType,
										subValues,
										options
								);
								values[selectableIndex] = instantiate( embeddableMappingType, attributeValues );
							}
							else {
								values[selectableIndex] = subValues;
							}
							s = State.VALUE_END;
							selectableIndex = -1;
							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case '[':
					switch ( s ) {
						case KEY_QUOTE:
							// I guess it's ok to have a '[' in the key..
						case VALUE_QUOTE:
							// In the value it's fine
							break;
						case VALUE_START:
							final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable(
									selectableIndex
							);
							final JdbcMapping jdbcMapping = selectable.getJdbcMapping();
							if ( !(jdbcMapping instanceof BasicPluralType<?, ?> pluralType) ) {
								throw new IllegalArgumentException(
										String.format(
												"JSON starts array for a non-plural type at index %d. Selectable [%s] is of type [%s]",
												i,
												selectable.getSelectableName(),
												jdbcMapping.getJdbcType().getClass().getName()
										)
								);
							}
							final BasicType<?> elementType = pluralType.getElementType();
							final CustomArrayList arrayList = new CustomArrayList();
							i = fromArrayString( string, returnEmbeddable, options, i, arrayList, elementType ) - 1;
							assert string.charAt( i ) == ']';
							values[selectableIndex] = pluralType.getJdbcJavaType().wrap( arrayList, options );
							s = State.VALUE_END;
							selectableIndex = -1;
							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case '}':
					switch ( s ) {
						case KEY_QUOTE:
							// I guess it's ok to have a '}' in the key..
						case VALUE_QUOTE:
							// In the value it's fine
							break;
						case VALUE_END:
							// At this point, we are done
							return i + 1;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				default:
					switch ( s ) {
						case KEY_QUOTE:
						case VALUE_QUOTE:
							// In keys and values, all chars are fine
							break;
						case VALUE_START:
							// Skip whitespace
							if ( Character.isWhitespace( c ) ) {
								break;
							}
							// Here we also allow certain literals
							final int endIdx = consumeLiteral(
									string,
									i,
									values,
									embeddableMappingType.getJdbcValueSelectable( selectableIndex ).getJdbcMapping(),
									selectableIndex,
									returnEmbeddable,
									options
							);
							if ( endIdx != -1 ) {
								i = endIdx;
								s = State.VALUE_END;
								selectableIndex = -1;
								start = -1;
								break;
							}
							throw syntaxError( string, s, i );
						case KEY_START:
						case KEY_END:
						case VALUE_END:
							// Only whitespace is allowed here
							if ( Character.isWhitespace( c ) ) {
								break;
							}
						default:
							throw syntaxError( string, s, i );
					}
					break;
			}
		}

		throw new IllegalArgumentException( "JSON not properly formed: " + string.subSequence( start, end ) );
	}

	private static int fromArrayString(
			String string,
			boolean returnEmbeddable,
			WrapperOptions options,
			int begin,
			CustomArrayList arrayList,
			BasicType<?> elementType) throws SQLException {
		return fromArrayString(
				string,
				returnEmbeddable,
				options,
				begin,
				arrayList,
				elementType.getMappedJavaType(),
				elementType.getJdbcJavaType(),
				elementType.getJdbcType()
		);
	}

	private static int fromArrayString(
			String string,
			boolean returnEmbeddable,
			WrapperOptions options,
			int begin,
			CustomArrayList arrayList,
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType) throws SQLException {
		if ( string.length() == begin + 2 ) {
			return begin + 2;
		}
		boolean hasEscape = false;
		assert string.charAt( begin ) == '[';
		int start = begin + 1;
		State s = State.VALUE_START;
		// The following parsing logic assumes JSON is well-formed,
		// but for the sake of the Java compiler's flow analysis
		// and hopefully also for a better understanding, contains throws for some syntax errors
		for ( int i = start; i < string.length(); i++ ) {
			final char c = string.charAt( i );
			switch ( c ) {
				case '\\':
					assert s == State.VALUE_QUOTE;
					hasEscape = true;
					i++;
					break;
				case '"':
					switch ( s ) {
						case VALUE_START:
							s = State.VALUE_QUOTE;
							start = i + 1;
							hasEscape = false;
							break;
						case VALUE_QUOTE:
							s = State.VALUE_END;
							arrayList.add(
									fromString(
											javaType,
											jdbcJavaType,
											jdbcType,
											string,
											start,
											i,
											hasEscape,
											returnEmbeddable,
											options
									)
							);
							start = -1;
							hasEscape = false;
							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case ',':
					switch ( s ) {
						case VALUE_QUOTE:
							// In the value it's fine
							break;
						case VALUE_END:
							s = State.VALUE_START;
							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case '{':
					switch ( s ) {
						case VALUE_QUOTE:
							// In the value it's fine
							break;
//						case VALUE_START:
//							final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable(
//									selectableIndex
//							);
//							if ( !( selectable.getJdbcMapping().getJdbcType() instanceof AggregateJdbcType ) ) {
//								throw new IllegalArgumentException(
//										String.format(
//												"JSON starts sub-object for a non-aggregate type at index %d. Selectable [%s] is of type [%s]",
//												i,
//												selectable.getSelectableName(),
//												selectable.getJdbcMapping().getJdbcType().getClass().getName()
//										)
//								);
//							}
//							final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping().getJdbcType();
//							final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
//							// This encoding is only possible if the JDBC type is JSON again
//							assert aggregateJdbcType.getJdbcTypeCode() == SqlTypes.JSON
//									|| aggregateJdbcType.getDefaultSqlTypeCode() == SqlTypes.JSON;
//							final Object[] subValues = new Object[subMappingType.getJdbcValueCount()];
//							i = fromString( subMappingType, string, i, end, subValues, returnEmbeddable, options ) - 1;
//							assert string.charAt( i ) == '}';
//							if ( returnEmbeddable ) {
//								final Object[] attributeValues = StructHelper.getAttributeValues(
//										subMappingType,
//										subValues,
//										options
//								);
//								values[selectableIndex] = embeddableMappingType.getRepresentationStrategy()
//										.getInstantiator()
//										.instantiate(
//												() -> attributeValues,
//												options.getSessionFactory()
//										);
//							}
//							else {
//								values[selectableIndex] = subValues;
//							}
//							s = State.VALUE_END;
//							selectableIndex = -1;
//							break;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				case ']':
					switch ( s ) {
						case VALUE_QUOTE:
							// In the value it's fine
							break;
						case VALUE_END:
							// At this point, we are done
							return i + 1;
						default:
							throw syntaxError( string, s, i );
					}
					break;
				default:
					switch ( s ) {
						case VALUE_QUOTE:
							// In keys and values, all chars are fine
							break;
						case VALUE_START:
							// Skip whitespace
							if ( Character.isWhitespace( c ) ) {
								break;
							}
							final int elementIndex = arrayList.size();
							arrayList.add( null );
							// Here we also allow certain literals
							final int endIdx = consumeLiteral(
									string,
									i,
									arrayList.getUnderlyingArray(),
									javaType,
									jdbcJavaType,
									jdbcType,
									elementIndex,
									returnEmbeddable,
									options
							);
							if ( endIdx != -1 ) {
								i = endIdx;
								s = State.VALUE_END;
								start = -1;
								break;
							}
							throw syntaxError( string, s, i );
						case VALUE_END:
							// Only whitespace is allowed here
							if ( Character.isWhitespace( c ) ) {
								break;
							}
						default:
							throw syntaxError( string, s, i );
					}
					break;
			}
		}

		throw new IllegalArgumentException( "JSON not properly formed: " + string.subSequence( start, string.length() ) );
	}

	private static int consumeLiteral(
			String string,
			int start,
			Object[] values,
			JdbcMapping jdbcMapping,
			int selectableIndex,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		return consumeLiteral(
				string,
				start,
				values,
				jdbcMapping.getMappedJavaType(),
				jdbcMapping.getJdbcJavaType(),
				jdbcMapping.getJdbcType(),
				selectableIndex,
				returnEmbeddable,
				options
		);
	}

	private static int consumeLiteral(
			String string,
			int start,
			Object[] values,
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType,
			int selectableIndex,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		final char c = string.charAt( start );
		switch ( c ) {
			case 'n':
				// only null is possible
				values[selectableIndex] = null;
				return consume(string, start, "null");
			case 'f':
				// only false is possible
				values[selectableIndex] = false;
				return consume(string, start, "false");
			case 't':
				// only false is possible
				values[selectableIndex] = true;
				return consume(string, start, "true");
			case '0':
				switch ( string.charAt( start + 1 ) ) {
					case '.':
						return consumeFractional(
								string,
								start,
								start + 1,
								values,
								javaType,
								jdbcJavaType,
								jdbcType,
								selectableIndex,
								returnEmbeddable,
								options
						);
					case 'E':
					case 'e':
						return consumeExponential(
								string,
								start,
								start + 1,
								values,
								javaType,
								jdbcJavaType,
								jdbcType,
								selectableIndex,
								returnEmbeddable,
								options
						);
				}
				values[selectableIndex] = fromString(
						javaType,
						jdbcJavaType,
						jdbcType,
						string,
						start,
						start + 1,
						returnEmbeddable,
						options
				);
				return start;
			case '-':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				// number = [ minus ] int [ frac ] [ exp ]
				// decimal-point = %x2E       ; .
				// digit1-9 = %x31-39         ; 1-9
				// e = %x65 / %x45            ; e E
				// exp = e [ minus / plus ] 1*DIGIT
				// frac = decimal-point 1*DIGIT
				// int = zero / ( digit1-9 *DIGIT )
				// minus = %x2D               ; -
				// plus = %x2B                ; +
				// zero = %x30                ; 0
				for (int i = start + 1; i < string.length(); i++) {
					final char digit = string.charAt( i );
					switch ( digit ) {
						case '.':
							return consumeFractional(
									string,
									start,
									i,
									values,
									javaType,
									jdbcJavaType,
									jdbcType,
									selectableIndex,
									returnEmbeddable,
									options
							);
						case 'E':
						case 'e':
							return consumeExponential(
									string,
									start,
									i,
									values,
									javaType,
									jdbcJavaType,
									jdbcType,
									selectableIndex,
									returnEmbeddable,
									options
							);
						case '0':
						case '1':
						case '2':
						case '3':
						case '4':
						case '5':
						case '6':
						case '7':
						case '8':
						case '9':
							break;
						default:
							values[selectableIndex] = fromString(
									javaType,
									jdbcJavaType,
									jdbcType,
									string,
									start,
									i,
									returnEmbeddable,
									options
							);
							return i - 1;
					}
				}
		}

		return -1;
	}

	private static int consumeFractional(
			String string,
			int start,
			int dotIndex,
			Object[] values,
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType,
			int selectableIndex,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		for (int i = dotIndex + 1; i < string.length(); i++) {
			final char digit = string.charAt( i );
			switch ( digit ) {
				case 'E':
				case 'e':
					return consumeExponential(
							string,
							start,
							i,
							values,
							javaType,
							jdbcJavaType,
							jdbcType,
							selectableIndex,
							returnEmbeddable,
							options
					);
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					break;
				default:
					values[selectableIndex] = fromString(
							javaType,
							jdbcJavaType,
							jdbcType,
							string,
							start,
							i,
							returnEmbeddable,
							options
					);
					return i - 1;
			}
		}
		return start;
	}

	private static int consumeExponential(
			String string,
			int start,
			int eIndex,
			Object[] values,
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType,
			int selectableIndex,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		int i = eIndex + 1;
		switch ( string.charAt( i ) ) {
			case '-':
			case '+':
				i++;
				break;
		}
		for (; i < string.length(); i++) {
			final char digit = string.charAt( i );
			switch ( digit ) {
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
					break;
				default:
					values[selectableIndex] = fromString(
							javaType,
							jdbcJavaType,
							jdbcType,
							string,
							start,
							i,
							returnEmbeddable,
							options
					);
					return i - 1;
			}
		}
		return start;
	}

	private static int consume(String string, int start, String text) {
		if ( !string.regionMatches( start + 1, text, 1, text.length() - 1 ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Syntax error at position %d. Unexpected char [%s]. Expecting [%s]",
							start + 1,
							string.charAt( start + 1 ),
							text
					)
			);
		}
		return start + text.length() - 1;
	}

	private static IllegalArgumentException syntaxError(String string, State s, int charIndex) {
		return new IllegalArgumentException(
				String.format(
						"Syntax error at position %d. Unexpected char [%s]. Expecting one of [%s]",
						charIndex,
						string.charAt( charIndex ),
						s.expectedChars()
				)
		);
	}

	private static int getSelectableMapping(
			EmbeddableMappingType embeddableMappingType,
			String string,
			int start,
			int end,
			boolean hasEscape) {
		final String name = hasEscape
				? unescape( string, start, end )
				: string.substring( start, end );
		final int selectableIndex = embeddableMappingType.getSelectableIndex( name );
		if ( selectableIndex == -1 ) {
			throw new IllegalArgumentException(
					String.format(
							"Could not find selectable [%s] in embeddable type [%s] for JSON processing.",
							name,
							embeddableMappingType.getMappedJavaType().getJavaTypeClass().getName()
					)
			);
		}
		return selectableIndex;
	}

	private static Object fromString(
			JdbcMapping jdbcMapping,
			String string,
			int start,
			int end,
			boolean hasEscape,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		return fromString(
				jdbcMapping.getMappedJavaType(),
				jdbcMapping.getJdbcJavaType(),
				jdbcMapping.getJdbcType(),
				string,
				start,
				end,
				hasEscape,
				returnEmbeddable,
				options
		);
	}

	private static Object fromString(
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType,
			String string,
			int start,
			int end,
			boolean hasEscape,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		if ( hasEscape ) {
			final String unescaped = unescape( string, start, end );
			return fromString(
					javaType,
					jdbcJavaType,
					jdbcType,
					unescaped,
					0,
					unescaped.length(),
					returnEmbeddable,
					options
			);
		}
		return fromString(
				javaType,
				jdbcJavaType,
				jdbcType,
				string,
				start,
				end,
				returnEmbeddable,
				options
		);
	}

	private static Object fromString(
			JavaType<?> javaType,
			JavaType<?> jdbcJavaType,
			JdbcType jdbcType,
			String string,
			int start,
			int end,
			boolean returnEmbeddable,
			WrapperOptions options) throws SQLException {
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
				return jdbcJavaType.wrap(
						PrimitiveByteArrayJavaType.INSTANCE.fromEncodedString(
							string,
							start,
							end
						),
						options
				);
			case SqlTypes.UUID:
				return jdbcJavaType.wrap(
						PrimitiveByteArrayJavaType.INSTANCE.fromString(
								string.substring( start, end ).replace( "-", "" )
						),
						options
				);
			case SqlTypes.DATE:
				return jdbcJavaType.wrap(
						JdbcDateJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				return jdbcJavaType.wrap(
						JdbcTimeJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIMESTAMP:
				return jdbcJavaType.wrap(
						JdbcTimestampJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
			case SqlTypes.TIMESTAMP_UTC:
				return jdbcJavaType.wrap(
						OffsetDateTimeJavaType.INSTANCE.fromEncodedString(
								string,
								start,
								end
						),
						options
				);
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class ) {
					return jdbcJavaType.wrap( Integer.parseInt( string, start, end, 10 ), options );
				}
				else if ( jdbcJavaType instanceof EnumJavaType<?> ) {
					return jdbcJavaType.wrap( Integer.parseInt( string, start, end, 10 ), options );
				}
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( jdbcJavaType.getJavaTypeClass() == Boolean.class && end == start + 1 ) {
					return jdbcJavaType.wrap( string.charAt( start ), options );
				}
			default:
				if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
					final Object[] subValues = aggregateJdbcType.extractJdbcValues(
							CharSequenceHelper.subSequence(
									string,
									start,
									end
							),
							options
					);
					if ( returnEmbeddable ) {
						final StructAttributeValues subAttributeValues = StructHelper.getAttributeValues(
								aggregateJdbcType.getEmbeddableMappingType(),
								subValues,
								options
						);
						return instantiate( aggregateJdbcType.getEmbeddableMappingType(), subAttributeValues ) ;
					}
					return subValues;
				}

				return jdbcJavaType.fromEncodedString( string, start, end );
		}
	}

	private static String unescape(String string, int start, int end) {
		final StringBuilder sb = new StringBuilder( end - start );
		for ( int i = start; i < end; i++ ) {
			final char c = string.charAt( i );
			if ( c == '\\' ) {
				i++;
				final char cNext = string.charAt( i );
				switch ( cNext ) {
					case '\\':
					case '"':
					case '/':
						sb.append( cNext );
						break;
					case 'b':
						sb.append( '\b' );
						break;
					case 'f':
						sb.append( '\f' );
						break;
					case 'n':
						sb.append( '\n' );
						break;
					case 'r':
						sb.append( '\r' );
						break;
					case 't':
						sb.append( '\t' );
						break;
					case 'u':
						sb.append( (char) Integer.parseInt( string, i + 1, i + 5, 16 ) );
						i += 4;
						break;
				}
				continue;
			}
			sb.append( c );
		}
		return sb.toString();
	}

	enum State {
		KEY_START( "\"\\s" ),
		KEY_QUOTE( "" ),
		KEY_END( ":\\s" ),
		VALUE_START( "\"\\s" ),
		VALUE_QUOTE( "" ),
		VALUE_END( ",}\\s" );

		final String expectedChars;

		State(String expectedChars) {
			this.expectedChars = expectedChars;
		}

		String expectedChars() {
			return expectedChars;
		}
	}

	public static class JsonAppender extends OutputStream implements SqlAppender {

		private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

		private final StringBuilder sb;
		private boolean escape;

		public JsonAppender(StringBuilder sb) {
			this.sb = sb;
		}

		@Override
		public void appendSql(String fragment) {
			append( fragment );
		}

		@Override
		public void appendSql(char fragment) {
			append( fragment );
		}

		@Override
		public void appendSql(int value) {
			sb.append( value );
		}

		@Override
		public void appendSql(long value) {
			sb.append( value );
		}

		@Override
		public void appendSql(boolean value) {
			sb.append( value );
		}

		@Override
		public String toString() {
			return sb.toString();
		}

		public void startEscaping() {
			assert !escape;
			escape = true;
		}

		public void endEscaping() {
			assert escape;
			escape = false;
		}

		@Override
		public JsonAppender append(char fragment) {
			if ( escape ) {
				appendEscaped( fragment );
			}
			else {
				sb.append( fragment );
			}
			return this;
		}

		@Override
		public JsonAppender append(CharSequence csq) {
			return append( csq, 0, csq.length() );
		}

		@Override
		public JsonAppender append(CharSequence csq, int start, int end) {
			if ( escape ) {
				int len = end - start;
				sb.ensureCapacity( sb.length() + len );
				for ( int i = start; i < end; i++ ) {
					appendEscaped( csq.charAt( i ) );
				}
			}
			else {
				sb.append( csq, start, end );
			}
			return this;
		}

		@Override
		public void write(int v) {
			final String hex = Integer.toHexString( v );
			sb.ensureCapacity( sb.length() + hex.length() + 1 );
			if ( ( hex.length() & 1 ) == 1 ) {
				sb.append( '0' );
			}
			sb.append( hex );
		}

		@Override
		public void write(byte[] bytes) {
			write(bytes, 0, bytes.length);
		}

		@Override
		public void write(byte[] bytes, int off, int len) {
			sb.ensureCapacity( sb.length() + ( len << 1 ) );
			for ( int i = 0; i < len; i++ ) {
				final int v = bytes[off + i] & 0xFF;
				sb.append( HEX_ARRAY[v >>> 4] );
				sb.append( HEX_ARRAY[v & 0x0F] );
			}
		}

		private void appendEscaped(char fragment) {
			switch ( fragment ) {
				case 0:
				case 1:
				case 2:
				case 3:
				case 4:
				case 5:
				case 6:
				case 7:
				//   8 is '\b'
				//   9 is '\t'
				//   10 is '\n'
				case 11:
				//   12 is '\f'
				//   13 is '\r'
				case 14:
				case 15:
				case 16:
				case 17:
				case 18:
				case 19:
				case 20:
				case 21:
				case 22:
				case 23:
				case 24:
				case 25:
				case 26:
				case 27:
				case 28:
				case 29:
				case 30:
				case 31:
					sb.append( "\\u" ).append( Integer.toHexString( fragment ) );
					break;
				case '\b':
					sb.append("\\b");
					break;
				case '\t':
					sb.append("\\t");
					break;
				case '\n':
					sb.append("\\n");
					break;
				case '\f':
					sb.append("\\f");
					break;
				case '\r':
					sb.append("\\r");
					break;
				case '"':
					sb.append( "\\\"" );
					break;
				case '\\':
					sb.append( "\\\\" );
					break;
				default:
					sb.append( fragment );
					break;
			}
		}

	}

	private static class CustomArrayList extends AbstractCollection<Object> implements Collection<Object> {
		Object[] array = ArrayHelper.EMPTY_OBJECT_ARRAY;
		int size;

		public void ensureCapacity(int minCapacity) {
			int oldCapacity = array.length;
			if ( minCapacity > oldCapacity ) {
				int newCapacity = oldCapacity + ( oldCapacity >> 1 );
				newCapacity = Math.max( Math.max( newCapacity, minCapacity ), 10 );
				array = Arrays.copyOf( array, newCapacity );
			}
		}

		public Object[] getUnderlyingArray() {
			return array;
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean add(Object o) {
			if ( size == array.length ) {
				ensureCapacity( size + 1 );
			}
			array[size++] = o;
			return true;
		}

		@Override
		public boolean isEmpty() {
			return size == 0;
		}

		@Override
		public boolean contains(Object o) {
			for ( int i = 0; i < size; i++ ) {
				if ( Objects.equals(o, array[i] ) ) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Iterator<Object> iterator() {
			return new Iterator<>() {
				int index;
				@Override
				public boolean hasNext() {
					return index != size;
				}

				@Override
				public Object next() {
					if ( index == size ) {
						throw new NoSuchElementException();
					}
					return array[index++];
				}
			};
		}

		@Override
		public Object[] toArray() {
			return Arrays.copyOf( array, size );
		}

		@Override
		@AllowReflection // We need the ability to create arrays of requested types dynamically.
		public <T> T[] toArray(T[] a) {
			//noinspection unchecked
			final T[] r = a.length >= size
					? a
					: (T[]) java.lang.reflect.Array.newInstance( a.getClass().getComponentType(), size );
			for (int i = 0; i < size; i++) {
				//noinspection unchecked
				r[i] = (T) array[i];
			}
			return null;
		}
	}

}

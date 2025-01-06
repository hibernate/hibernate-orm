/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import oracle.jdbc.driver.json.tree.OracleJsonDateImpl;
import oracle.jdbc.driver.json.tree.OracleJsonTimestampImpl;
import oracle.sql.DATE;
import oracle.sql.TIMESTAMP;
import oracle.sql.json.OracleJsonDate;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;
import oracle.sql.json.OracleJsonTimestamp;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.UUIDJavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.hibernate.dialect.StructHelper.getEmbeddedPart;

/**
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private final ObjectMapper objectMapper;
	private final EmbeddableMappingType embeddableMappingType;

	// fields/Methods to retrieve serializer data


	/**
	 * Creates a new JacksonOsonFormatMapper
	 * @param objectMapper the Jackson object mapper
	 * same as JacksonOsonFormatMapper(objectMapper, null)
	 */
	public JacksonOsonFormatMapper(ObjectMapper objectMapper) {
		this(objectMapper, null);
	}

	/**
	 * Creates a new JacksonOsonFormatMapper
	 * @param objectMapper the Jackson object mapper
	 * @param embeddableMappingType the embeddable mapping definitions
	 */
	public JacksonOsonFormatMapper(ObjectMapper objectMapper, EmbeddableMappingType embeddableMappingType) {
		super(objectMapper);
		this.objectMapper = objectMapper;
		this.embeddableMappingType = embeddableMappingType;
	}


	/**
	 * Process OSON parser tokens
	 * @param osonParser the OSON parser
	 * @param currentEvent the current of the parser
	 * @param finalResult the populated object array
	 * @param embeddableMappingType the embeddable mapping definitions
	 * @param options the wrapping options
	 * @throws IOException error while reading from underlying parser
	 */
	private void consumeOsonTokens(OracleJsonParser osonParser, OracleJsonParser.Event currentEvent, Object [] finalResult, EmbeddableMappingType embeddableMappingType, WrapperOptions options)
			throws IOException {

		OracleJsonParser.Event event = currentEvent;

		int selectableIndex = -1;
		SelectableMapping mapping = null;
		String currentKeyName = null;
		List<Object> subArrayList = null;
		BasicPluralType<?, ?> pluralType = null;
		Object theOne;
		while ( event != null ) {
			switch ( event ) {
				case OracleJsonParser.Event.KEY_NAME:
					currentKeyName = osonParser.getString();
					selectableIndex = embeddableMappingType.getSelectableIndex( currentKeyName );
					if ( selectableIndex >= 0 ) {
						// we may not have a selectable mapping for that key
						mapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex );
					}
					break;
				case OracleJsonParser.Event.START_ARRAY:
					// initialize array to gather values
					subArrayList = new ArrayList<>();
					assert (mapping.getJdbcMapping() instanceof BasicPluralType<?, ?>)
							: "Array event received for non plural type";
					// initialize array's element type
					pluralType = (BasicPluralType<?, ?>) mapping.getJdbcMapping();
					break;
				case OracleJsonParser.Event.END_ARRAY:
					assert (subArrayList != null && pluralType != null) : "Wrong event ordering";
					// flush array values
					finalResult[selectableIndex] = pluralType.getJdbcJavaType().wrap( subArrayList, options );
					// reset until we encounter next array elem
					subArrayList = null;
					pluralType = null;
					break;
				case OracleJsonParser.Event.VALUE_DATE:
					LocalDateTime localDateTime = osonParser.getLocalDateTime();
					Class underlyingType = null;
					if(pluralType!=null) {
						underlyingType = pluralType.getElementType().getJavaType();
					} else {
						underlyingType = (Class) mapping.getJdbcMapping().getJdbcJavaType().getJavaType();
					}
					if (java.sql.Date.class.isAssignableFrom( underlyingType )) {
						theOne = Date.valueOf( localDateTime.toLocalDate());
					} else if (java.time.LocalDate.class.isAssignableFrom( underlyingType )) {
						theOne = localDateTime.toLocalDate();
					} else {
						throw new IllegalArgumentException("unexpected date type " + underlyingType);
					}
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( theOne );
					}
					else {
						finalResult[selectableIndex] = theOne;
					}
					break;
				case OracleJsonParser.Event.VALUE_TIMESTAMP:
					LocalDateTime local = osonParser.getLocalDateTime();
					if ( "java.sql.Timestamp".equals(
							mapping.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName() ) ) {
						theOne = Timestamp.valueOf( local );
					}
					else if ( "java.time.LocalTime".equals(
							mapping.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName() )) {
						theOne = local.toLocalTime();
					}
					else if ( "java.sql.Time".equals(
							mapping.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName() )) {
						theOne = Time.valueOf( local.toLocalTime() );
					}
					else {
						theOne = local;
					}
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( theOne );
					}
					else {
						finalResult[selectableIndex] = theOne;
					}
					break;
				case OracleJsonParser.Event.VALUE_TIMESTAMPTZ:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getOffsetDateTime() );
					}
					else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType()
										.wrap( osonParser.getOffsetDateTime(), options ) );
					}
					break;
				case OracleJsonParser.Event.VALUE_INTERVALDS:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getDuration() );
					}
					else {
						// TODO: shall I use mapping.getJdbcMapping().getJdbcJavaType().wrap(...) ?
						finalResult[selectableIndex] = osonParser.getDuration();
					}
					break;
				case OracleJsonParser.Event.VALUE_INTERVALYM:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getPeriod() );
					}
					else {
						// TODO: shall I use mapping.getJdbcMapping().getJdbcJavaType().wrap(...) ?
						finalResult[selectableIndex] = osonParser.getPeriod();
					}
					break;
				case OracleJsonParser.Event.VALUE_STRING:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add(
								pluralType.getElementType().getJdbcJavaType().fromString( osonParser.getString() ) );
					}
					else {
//						finalResult[selectableIndex] = mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getString(),options );
//						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue( mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getString(),options ));
						finalResult[selectableIndex] = mapping.getJdbcMapping().getJdbcJavaType().fromEncodedString( osonParser.getString(),0,osonParser.getString().length() );
					}
					break;
				case OracleJsonParser.Event.VALUE_TRUE:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( Boolean.TRUE );
					}
					else {
						finalResult[selectableIndex] = Boolean.TRUE;
					}
					break;
				case OracleJsonParser.Event.VALUE_FALSE:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( Boolean.FALSE );
					}
					else {
						finalResult[selectableIndex] = Boolean.FALSE;
					}
					break;
				case OracleJsonParser.Event.VALUE_NULL:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( null );
					}
					else {
						finalResult[selectableIndex] = null;
					}
					break;
				case OracleJsonParser.Event.VALUE_DECIMAL:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.isIntegralNumber() ? osonParser.getInt() : osonParser.getFloat() );
					}
					else {
						// not array case: wrap value directly

						if ( osonParser.isIntegralNumber() ) {
							if("java.lang.Double".equals( mapping.getJdbcMapping().getJdbcJavaType().getTypeName() ) ) {
								theOne = mapping.getJdbcMapping().convertToDomainValue(
										mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getDouble(), options ) );

							} else {
								theOne = mapping.getJdbcMapping().convertToDomainValue(
										mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getInt(), options ) );

							}
						}
						else {

							theOne = mapping.getJdbcMapping().convertToDomainValue(
									mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getFloat(), options ) );
						}
						finalResult[selectableIndex] = theOne;
					}
					break;
				case OracleJsonParser.Event.VALUE_DOUBLE:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getDouble() );
					}
					else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getDouble(), options ) );
					}
					break;
				case OracleJsonParser.Event.VALUE_FLOAT:
					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( osonParser.getFloat() );
					}
					else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getFloat(), options ) );
					}
					break;
				case OracleJsonParser.Event.VALUE_BINARY:
					if(pluralType!=null) {
						underlyingType = pluralType.getElementType().getJavaType();
					}
					else {
						underlyingType = (Class) mapping.getJdbcMapping().getJdbcJavaType().getJavaType();
					}

					if (java.util.UUID.class.isAssignableFrom( underlyingType ))  {
						theOne = UUIDJavaType.INSTANCE.wrap( osonParser.getBytes(), options );
					}
					else {
						theOne = osonParser.getBytes();
					}

					if ( pluralType != null ) {
						// dealing with arrays
						subArrayList.add( theOne );
					}
					else {
						finalResult[selectableIndex] = theOne;
					}
					break;
				case OracleJsonParser.Event.START_OBJECT:
					if ( currentKeyName == null ) {
						// that's the root
						consumeOsonTokens( osonParser, osonParser.next(), finalResult,
								embeddableMappingType,
								options );
					}
					else {
						selectableIndex = embeddableMappingType.getSelectableIndex( currentKeyName );
						if ( selectableIndex != -1 ) {
							final SelectableMapping selectable = embeddableMappingType.getJdbcValueSelectable(
									selectableIndex );
							final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) selectable.getJdbcMapping()
									.getJdbcType();
							final EmbeddableMappingType subMappingType = aggregateJdbcType.getEmbeddableMappingType();
							finalResult[selectableIndex] = new Object[subMappingType.getJdbcValueCount()];
							consumeOsonTokens( osonParser, osonParser.next(),
									(Object[]) finalResult[selectableIndex],
									subMappingType,
									options );
						}
					}
					break;
				case OracleJsonParser.Event.END_OBJECT:
					return;
				default:
					throw new IOException( "Unknown OSON event " + event );

			}
			event = osonParser.hasNext() ? osonParser.next() : null;
		}

	}


	/**
	 * Consumes OSON bytes and populate an Object array as described in the embeddable mapping definitions.
	 * @param embeddableMappingType the embeddable mapping definitions
	 * @param source the OSON bytes as <code>byte[]</code>
	 * @param options the wrapping options
	 * @return the Object array
	 * @param <T> return type i.e object array
	 * @throws IOException OSON parsing has failed
	 */
	public <T> T toObjectArray(EmbeddableMappingType embeddableMappingType, Object source, WrapperOptions options) throws IOException {
		Object []finalResult = new Object[embeddableMappingType.getJdbcValueCount()];
		OracleJsonParser osonParser = new OracleJsonFactory().createJsonBinaryParser( ByteBuffer.wrap( (byte[])source ) );
		consumeOsonTokens(osonParser, osonParser.next(), finalResult, embeddableMappingType, options);
		return (T)finalResult;
	}

	@Override
	public <T> T fromString(CharSequence charSequence, Type type) {
		try {
			return objectMapper.readValue( charSequence.toString(), objectMapper.constructType( type ) );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + type, e );
		}
	}

	@Override
	public <T> String toString(T value, Type type) {
		try {
			return objectMapper.writerFor( objectMapper.constructType( type ) ).writeValueAsString( value );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + type, e );
		}
	}


	public <X>byte[] toOson(X value, JavaType<X> javaType, WrapperOptions options) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OracleJsonGenerator generator = new OracleJsonFactory().createJsonBinaryGenerator( out );
		serializetoOsonApproach2( value,generator,javaType,options);
		generator.close();
		return out.toByteArray();
	}

	private <X> void serializetoOsonApproach2(X value, OracleJsonGenerator generator, JavaType<X> javaType, WrapperOptions options) {
		generator.writeStartObject();
		serializetoOsonApproach2Util( value, generator, javaType, options,embeddableMappingType );
		generator.writeEnd();
	}

	private <X> void serializetoOsonApproach2Util(X value,
												  OracleJsonGenerator generator,
												  JavaType<X> javaType,
												  WrapperOptions options,
												  EmbeddableMappingType embeddableMappingType) {

		final Object[] values = embeddableMappingType.getValues( value );
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping ) {
				final String name = ( (SelectableMapping) attributeMapping ).getSelectableName();
				final BasicType<Object> basicType = (BasicType<Object>) attributeMapping.getMappedType();

				generator.writeKey( name );
				serializeValue( basicType.convertToRelationalValue( values[i] ), (JavaType<Object>) basicType.getJdbcJavaType(),basicType.getJdbcType(), options,generator);

			}
			else if (attributeMapping instanceof EmbeddedAttributeMapping) {
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if ( values[i] == null ) {
					// Skipping the update of the separator is on purpose
					continue;
				}
				if (aggregateMapping == null) {
					// flattened case
					serializetoOsonApproach2Util( (X) values[i],
							generator,
							javaType,
							options,
							embeddableMappingType );
				}
				else {
					// non flattened case
					final String name = aggregateMapping.getSelectableName();
					generator.writeKey( name );
					generator.writeStartObject();
					serializetoOsonApproach2Util( (X) values[i],
							generator,
							javaType,
							options,
							embeddableMappingType);
					generator.writeEnd();

				}

			}
		}
	}

	private void serializeValue(Object value,
								JavaType<Object> javaType,
								JdbcType jdbcType,
								WrapperOptions options,
								OracleJsonGenerator generator) {
		switch ( jdbcType.getDefaultSqlTypeCode() ) {
			case SqlTypes.TINYINT:
			case SqlTypes.SMALLINT:
			case SqlTypes.INTEGER:
				if ( value instanceof Boolean ) {
					// BooleanJavaType has this as an implicit conversion
					int i = ((Boolean) value) ? 1 : 0;
					generator.write( i );
					break;
				}
				if ( value instanceof Enum ) {
					generator.write( ((Enum<?>) value ).ordinal() );
					break;
				}
				generator.write( javaType.unwrap( value,Integer.class,options ) );
				break;
			case SqlTypes.BOOLEAN:
				generator.write( (Boolean) value );
				break;
			case SqlTypes.BIT:
				generator.write( (Integer) value );
				break;
			case SqlTypes.BIGINT:
				generator.write( (BigInteger) value );
				break;
			case SqlTypes.FLOAT:
				generator.write( (Float) value );
				break;
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
				generator.write( (Double) value );
				break;
			case SqlTypes.CHAR:
			case SqlTypes.NCHAR:
			case SqlTypes.VARCHAR:
			case SqlTypes.NVARCHAR:
				if ( value instanceof Boolean ) {
					String c = ((Boolean) value) ? "Y" : "N";
					generator.write( c );
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
				// correct?
				generator.write( javaType.toString(value) );
				break;
			case SqlTypes.DATE:
				DATE dd = new DATE(javaType.unwrap( value,java.sql.Date.class,options ));
				OracleJsonDate jsonDate = new OracleJsonDateImpl(dd.shareBytes());
				generator.write(jsonDate);
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				generator.write( javaType.toString(value) );
				break;
			case SqlTypes.TIMESTAMP:
				TIMESTAMP TS = new TIMESTAMP(javaType.unwrap( value, Timestamp.class, options ));
				OracleJsonTimestamp writeTimeStamp = new OracleJsonTimestampImpl(TS.shareBytes());
				generator.write(writeTimeStamp);
				break;
			case SqlTypes.TIMESTAMP_WITH_TIMEZONE:
				try {
					OffsetDateTime dateTime = javaType.unwrap( value, OffsetDateTime.class, options );
					generator.write( dateTime );
				}
				catch (Exception e) {
					Timestamp tswtz = javaType.unwrap( value, Timestamp.class, options );
					TIMESTAMP TSWTZ = new TIMESTAMP(tswtz);
					OracleJsonTimestamp writeTimeStampWTZ = new OracleJsonTimestampImpl(TSWTZ.shareBytes());
					generator.write(writeTimeStampWTZ);
				}
				break;
			case SqlTypes.TIMESTAMP_UTC:
				if( value instanceof OffsetDateTime ) {
					OffsetDateTime odt = javaType.unwrap( value, OffsetDateTime.class, options );
					generator.write( odt );
					break;
				}
				else if (value instanceof Instant ) {
					Instant instant = javaType.unwrap( value, Instant.class, options );
					generator.write(instant.atOffset( ZoneOffset.UTC )  );
					break;
				}
				generator.write( javaType.toString(value) );
				break;
			case SqlTypes.NUMERIC:
			case SqlTypes.DECIMAL:
				BigDecimal bd = javaType.unwrap( value, BigDecimal.class, options );
				generator.write( bd );
				break;

			case SqlTypes.DURATION:
				Duration duration = javaType.unwrap( value, Duration.class, options );
				generator.write( duration );
				break;
			case SqlTypes.UUID:
				UUID uuid = javaType.unwrap( value, UUID.class, options );
				byte[] bytes = _asBytes( uuid );
				generator.write( bytes );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:

				break;
			case SqlTypes.ARRAY:
			case SqlTypes.JSON_ARRAY:
				final int length = Array.getLength( value );
				generator.writeStartArray();
				if ( length != 0 ) {
					//noinspection unchecked
					final JavaType<Object> elementJavaType = ( (BasicPluralJavaType<Object>) javaType ).getElementJavaType();
					final JdbcType elementJdbcType = ( (ArrayJdbcType) jdbcType ).getElementJdbcType();

					for ( int i = 0; i < length; i++ ) {
						Object arrayElement = Array.get( value, i );
						serializeValue( arrayElement,elementJavaType, elementJdbcType, options, generator );
					}
				}
				generator.writeEnd();
				break;
			default:
				throw new UnsupportedOperationException( "Unsupported JdbcType nested in JSON: " + jdbcType );
		}

	}
	private byte[] _asBytes(UUID uuid)
	{
		byte[] buffer = new byte[16];
		long hi = uuid.getMostSignificantBits();
		long lo = uuid.getLeastSignificantBits();
		_appendInt((int) (hi >> 32), buffer, 0);
		_appendInt((int) hi, buffer, 4);
		_appendInt((int) (lo >> 32), buffer, 8);
		_appendInt((int) lo, buffer, 12);
		return buffer;
	}

	private void _appendInt(int value, byte[] buffer, int offset)
	{
		buffer[offset] = (byte) (value >> 24);
		buffer[++offset] = (byte) (value >> 16);
		buffer[++offset] = (byte) (value >> 8);
		buffer[++offset] = (byte) value;
	}


	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws IOException {
		com.fasterxml.jackson.databind.JavaType jacksonJavaType = objectMapper.constructType( javaType.getJavaType() );

		if(embeddableMappingType == null ) {
			ObjectWriter writer = objectMapper.writerFor( jacksonJavaType );
			writer.writeValue( (JsonGenerator) target, value);
			return;
		}

		DefaultSerializerProvider provider =((DefaultSerializerProvider)objectMapper.getSerializerProvider())
				.createInstance( objectMapper.getSerializationConfig(),objectMapper.getSerializerFactory() );
		JsonSerializer<Object> valueSerializer = provider
				.findTypedValueSerializer( jacksonJavaType,true, null );
		serializetoOson(value,valueSerializer,embeddableMappingType,(JsonGenerator)target,objectMapper,provider);
		((JsonGenerator)target).flush();

	}

	private <T> void serializetoOson(T value, JsonSerializer<Object> valueSerializer, EmbeddableMappingType embeddableMappingType, JsonGenerator target, ObjectMapper objectMapper, DefaultSerializerProvider provider)
			throws IOException {
		target.writeStartObject();
		serializetoOsonUtil(value,valueSerializer,embeddableMappingType,target,objectMapper,provider);
		target.writeEndObject();

	}

	private <T> void serializetoOsonUtil(T value,
										JsonSerializer<Object> valueSerializer,
										EmbeddableMappingType embeddableMappingType,
										JsonGenerator generator,
										ObjectMapper mapper,
										DefaultSerializerProvider provider) throws IOException {
		final Object[] values = embeddableMappingType.getValues( value );

		Map<String, BeanPropertyWriter> beanPropertyWriterMap = buildBeanPropertyMap(valueSerializer.properties());
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping ) {
				// basic attribute ??
				final String name = ( (SelectableMapping) attributeMapping ).getSelectableName();

				final BasicType<Object> basicType = (BasicType<Object>) attributeMapping.getMappedType();
				BasicValueConverter<?, ?> valueConverter = basicType.getValueConverter();
				JavaType<?> javaType =
						valueConverter!=null ? valueConverter.getRelationalJavaType() : attributeMapping.getJavaType();
				generator.writeFieldName( name );

				BeanPropertyWriter writer = beanPropertyWriterMap.get( ( (BasicAttributeMapping) attributeMapping ).getAttributeName() );
				JsonSerializer<Object> serializer =
						provider.findValueSerializer( objectMapper.constructType( javaType.getJavaType() ), writer );
				JsonSerializer<Object> nullSerializer = provider.findNullValueSerializer( null );

				try {
					assert serializer != null;
					if ( values[i] == null ) {
						nullSerializer.serialize( null, generator, provider );
					}
					else {
						serializer.serialize( basicType.convertToRelationalValue( values[i] ),generator,provider);
					}

				}
				catch (Exception e) {
					throw new RuntimeException( e );
				}
			}
			else if ( attributeMapping instanceof EmbeddedAttributeMapping ) {
				if ( values[i] == null ) {
					// Skipping the update of the separator is on purpose
					continue;
				}
				final EmbeddableMappingType mappingType = (EmbeddableMappingType) attributeMapping.getMappedType();
				final SelectableMapping aggregateMapping = mappingType.getAggregateMapping();
				if ( aggregateMapping == null ){

					JsonSerializer<Object> serializer = provider
							.findTypedValueSerializer( objectMapper.constructType( attributeMapping.getJavaType().getJavaType() ),true,null );
					// flattened case
					serializetoOsonUtil( (T) values[i],
							serializer,
							mappingType,
							generator,
							mapper,
							provider);
				}
				else {
					// non flattened case
					final String name = aggregateMapping.getSelectableName();
					generator.writeFieldName( name );
					generator.writeStartObject();
					JsonSerializer<Object> serializer = provider
							.findTypedValueSerializer(
									objectMapper.constructType( attributeMapping.getJavaType().getJavaType() ),
									true,null );
					serializetoOsonUtil( (T)values[i],
							serializer,
							mappingType,
							generator,
							mapper,
							provider);
					generator.writeEndObject();
				}

			}

		}

	}

	private Map<String, BeanPropertyWriter> buildBeanPropertyMap(Iterator<PropertyWriter> properties) {
		Map<String,BeanPropertyWriter> result = new HashMap<String,BeanPropertyWriter>();
		while ( properties.hasNext() ) {
			BeanPropertyWriter writer = (BeanPropertyWriter) properties.next();
			result.put( writer.getName(), writer );
		}
		return result;
	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
		JsonParser osonParser = objectMapper.getFactory().createParser( (byte[]) source );
		return  objectMapper.readValue( osonParser, objectMapper.constructType( javaType.getJavaType()) );
	}



}

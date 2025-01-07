/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import org.hibernate.type.descriptor.jdbc.ArrayJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.format.JsonDocumentHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;


/**
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private  ObjectMapper objectMapper;
	private EmbeddableMappingType embeddableMappingType;

	// fields/Methods to retrieve serializer data


	/**
	 * Creates a new JacksonOsonFormatMapper
	 * @param objectMapper the Jackson object mapper
	 * same as JacksonOsonFormatMapper(objectMapper, null)
	 */
	public JacksonOsonFormatMapper(ObjectMapper objectMapper) {
		super(objectMapper);
	}

	/**
	 * Creates a new JacksonOsonFormatMapper
	 */
	public JacksonOsonFormatMapper() {
		super();
	}

	/**
	 * Process OSON parser tokens
	 * @param osonParser the OSON parser
	 * @param currentEvent the current of the parser
	 * @throws IOException error while reading from underlying parser
	 */
	private void consumeOsonTokens(OracleJsonParser osonParser, OracleJsonParser.Event currentEvent, JsonDocumentHandler handler)
			throws IOException {

		OracleJsonParser.Event event = currentEvent;

		while ( event != null ) {
			switch ( event ) {
				case OracleJsonParser.Event.KEY_NAME:
					handler.onObjectKey( osonParser.getString() );
					break;
				case OracleJsonParser.Event.START_ARRAY:
					handler.startArray();
					break;
				case OracleJsonParser.Event.END_ARRAY:
					handler.endArray();
					break;
				case OracleJsonParser.Event.VALUE_DATE:
				case OracleJsonParser.Event.VALUE_TIMESTAMP:
					((ObjectArrayOsonDocumentHandler)handler).onOsonDateValue(
							osonParser.getLocalDateTime());
					break;
				case OracleJsonParser.Event.VALUE_TIMESTAMPTZ:
					((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
							osonParser.getOffsetDateTime());
					break;
				case OracleJsonParser.Event.VALUE_INTERVALDS:
					((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
							osonParser.getDuration());
					break;
				case OracleJsonParser.Event.VALUE_INTERVALYM:
					((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
							osonParser.getPeriod());
					break;
				case OracleJsonParser.Event.VALUE_STRING:
					handler.onStringValue( osonParser.getString() );
					break;
				case OracleJsonParser.Event.VALUE_TRUE:
					handler.onBooleanValue( true );
					break;
				case OracleJsonParser.Event.VALUE_FALSE:
					handler.onBooleanValue( false );
					break;
				case OracleJsonParser.Event.VALUE_NULL:
					handler.onNullValue();
					break;
				case OracleJsonParser.Event.VALUE_DECIMAL:
					((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
							osonParser.getBigDecimal());
					break;
				case OracleJsonParser.Event.VALUE_DOUBLE:
					((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
							osonParser.getDouble());
					break;
				case OracleJsonParser.Event.VALUE_FLOAT:
					((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
							osonParser.getFloat());
					break;
				case OracleJsonParser.Event.VALUE_BINARY:
					((ObjectArrayOsonDocumentHandler)handler).onOsonBinaryValue(
							osonParser.getBytes());
					break;
				case OracleJsonParser.Event.START_OBJECT:
					handler.startObject(  );
					//consumeOsonTokens( osonParser, osonParser.next(), handler);
					break;
				case OracleJsonParser.Event.END_OBJECT:
					handler.endObject();
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
	 * @param <T> return type i.e., object array
	 * @throws IOException OSON parsing has failed
	 */
	public <T> T toObjectArray(EmbeddableMappingType embeddableMappingType, Object source, WrapperOptions options) throws IOException {

		OracleJsonParser osonParser = new OracleJsonFactory().createJsonBinaryParser( ByteBuffer.wrap( (byte[])source ) );

		ObjectArrayOsonDocumentHandler handler = new ObjectArrayOsonDocumentHandler( embeddableMappingType,
				options);

		consumeOsonTokens(osonParser, osonParser.next(), handler);

		return (T)handler.getObjectArray();
	}

	public <X>byte[] toOson(X value, JavaType<X> javaType, WrapperOptions options,EmbeddableMappingType embeddableMappingType) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OracleJsonGenerator generator = new OracleJsonFactory().createJsonBinaryGenerator( out );
		serializetoOsonApproach2( value,generator,javaType,options,embeddableMappingType);
		generator.close();
		return out.toByteArray();
	}

	private <X> void serializetoOsonApproach2(X value, OracleJsonGenerator generator, JavaType<X> javaType, WrapperOptions options, EmbeddableMappingType embeddableMappingType) {
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
				generator.write( javaType.unwrap( value,Boolean.class,options ) );
				break;
			case SqlTypes.BIT:
				generator.write( javaType.unwrap( value,Integer.class,options ) );
				break;
			case SqlTypes.BIGINT:
				generator.write( javaType.unwrap( value,BigInteger.class,options ) );
				break;
			case SqlTypes.FLOAT:
				generator.write( javaType.unwrap( value,Float.class,options ) );
				break;
			case SqlTypes.REAL:
			case SqlTypes.DOUBLE:
				generator.write( javaType.unwrap( value,Double.class,options ) );
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
				generator.write( javaType.unwrap( value,String.class,options ) );
				break;
			case SqlTypes.DATE:
				DATE dd = new DATE(javaType.unwrap( value,java.sql.Date.class,options ));
				OracleJsonDate jsonDate = new OracleJsonDateImpl(dd.shareBytes());
				generator.write(jsonDate);
				break;
			case SqlTypes.TIME:
			case SqlTypes.TIME_WITH_TIMEZONE:
			case SqlTypes.TIME_UTC:
				generator.write( javaType.unwrap( value,String.class,options ) );
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
				generator.write( javaType.unwrap( value,String.class,options ) );
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
				byte[] uuidBytes = _asBytes( uuid );
				generator.write( uuidBytes );
				break;
			case SqlTypes.BINARY:
			case SqlTypes.VARBINARY:
			case SqlTypes.LONGVARBINARY:
			case SqlTypes.LONG32VARBINARY:
				byte[] bytes = javaType.unwrap( value, byte[].class, options );
				generator.write( bytes );
				break;
			case SqlTypes.BLOB:
			case SqlTypes.MATERIALIZED_BLOB:
				// how to handle
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

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return JsonParser.class.isAssignableFrom( sourceType );
	}

	@Override
	public boolean supportsTargetType(Class<?> targetType) {
		return JsonParser.class.isAssignableFrom( targetType );
	}

	public void setJacksonObjectMapper(ObjectMapper objectMapper, EmbeddableMappingType embeddableMappingType) {
		this.objectMapper = objectMapper;

		this.embeddableMappingType = embeddableMappingType;

	}

}

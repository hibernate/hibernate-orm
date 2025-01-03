/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonParser;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.JsonDocumentHandler;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

/**
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private  ObjectMapper objectMapper;

	/**
	 * Creates a new JacksonOsonFormatMapper
	 * @param objectMapper the Jackson object mapper
	 * same as JacksonOsonFormatMapper(objectMapper, null)
	 */
	public JacksonOsonFormatMapper(ObjectMapper objectMapper) {
		super(objectMapper);
		this.objectMapper = objectMapper;
	}
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
					if (osonParser.isIntegralNumber()) {
						((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
								osonParser.getInt());
					} else {
						((ObjectArrayOsonDocumentHandler)handler).onOsonValue(
								osonParser.getFloat());
					}
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

		return (T)handler.getMappedObjectArray();
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

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws IOException {

		ObjectWriter writer = objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) );
		writer.writeValue( (JsonGenerator) target, value);
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

		// need this until annotation introspector is removed.
		if (embeddableMappingType != null) {
			this.objectMapper.setAnnotationIntrospector(
					new JacksonJakartaAnnotationIntrospector( embeddableMappingType ) );
		}

	}
}

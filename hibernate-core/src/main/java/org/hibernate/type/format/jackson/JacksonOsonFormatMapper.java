/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import oracle.sql.json.OracleJsonParser;
import org.hibernate.dialect.JsonHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.format.JsonDocumentHandler;
import org.hibernate.type.format.ObjectArrayOsonDocumentHandler;
import org.hibernate.type.format.ObjectArrayOsonDocumentWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * Implementation of FormatMapper for Orale OSON support
 *
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";


	private static final Class osonModuleKlass;
	static {
		try {
			osonModuleKlass = JacksonOsonFormatMapper.class.getClassLoader().loadClass( "oracle.jdbc.provider.oson.OsonModule" );
		}
		catch (ClassNotFoundException | LinkageError e) {
			// should not happen as JacksonOsonFormatMapper is loaded
			// only when Oracle OSON JDBC extension is present
			// see OracleDialect class.
			throw new ExceptionInInitializerError( "JacksonOsonFormatMapper class loaded without OSON extension: " + e.getClass()+" "+ e.getMessage());
		}
	}

	/**
	 * Creates a new JacksonOsonFormatMapper
	 */
	public JacksonOsonFormatMapper() {
		super();
		try {
			objectMapper.registerModule( (Module) osonModuleKlass.getDeclaredConstructor().newInstance() );
		}
		catch (Exception e) {
			throw new RuntimeException( "Cannot instanciate " + osonModuleKlass.getCanonicalName(), e );
		}
		objectMapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	/**
	 * Process OSON parser tokens.
	 * This method consume one by one event coming from an OSON parser and use the given JsonDocumentHandler
	 * to populate values into Object array
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
					handler.onStartArray();
					break;
				case OracleJsonParser.Event.END_ARRAY:
					handler.onEndArray();
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
					handler.onStartObject();
					break;
				case OracleJsonParser.Event.END_OBJECT:
					handler.onEndObject();
					break;
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

	public <X>byte[] fromObjectArray(X value, JavaType<X> javaType, WrapperOptions options,EmbeddableMappingType embeddableMappingType)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OracleJsonGenerator generator = new OracleJsonFactory().createJsonBinaryGenerator( out );
		ObjectArrayOsonDocumentWriter writer = new ObjectArrayOsonDocumentWriter(generator);
		JsonHelper.serialize( embeddableMappingType, value,options,writer);
		generator.close();
		return out.toByteArray();
	}

	public <X>byte[] arrayToOson(X value,
								JavaType<X> javaType,
								JdbcType elementJdbcType,
								WrapperOptions options) {

		final Object[] domainObjects = javaType.unwrap( value, Object[].class, options );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		OracleJsonGenerator generator = new OracleJsonFactory().createJsonBinaryGenerator( out );
		ObjectArrayOsonDocumentWriter writer = new ObjectArrayOsonDocumentWriter(generator);

		if ( elementJdbcType instanceof JsonJdbcType jsonElementJdbcType ) {
			final EmbeddableMappingType embeddableMappingType = jsonElementJdbcType.getEmbeddableMappingType();
			JsonHelper.serializeArray( embeddableMappingType, domainObjects, options,  writer);
		}
		else {
			assert !( elementJdbcType instanceof AggregateJdbcType);
			final JavaType<?> elementJavaType = ( (BasicPluralJavaType<?>) javaType ).getElementJavaType();
			JsonHelper.serializeArray( elementJavaType, elementJdbcType, domainObjects, options, writer );
		}

		generator.close();
		return out.toByteArray();
	}

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws IOException {
		ObjectWriter writer = objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) );
		writer.writeValue( (JsonGenerator) target, value);

	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
		return  objectMapper.readValue( (JsonParser)source, objectMapper.constructType( javaType.getJavaType()) );
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return JsonParser.class.isAssignableFrom( sourceType );
	}

	@Override
	public boolean supportsTargetType(Class<?> targetType) {
		return JsonGenerator.class.isAssignableFrom( targetType );
	}


}

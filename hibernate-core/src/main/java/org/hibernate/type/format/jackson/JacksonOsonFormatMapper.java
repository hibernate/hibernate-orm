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
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.type.BasicType;
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

	public void _arrayToOson(OracleJsonGenerator osonGen,
							EmbeddableMappingType elementMappingType,
							Object[] values,
							WrapperOptions options) {

		osonGen.writeStartArray();

		if ( values.length == 0 ) {
			osonGen.writeEnd();
			return;
		}

		for ( Object value : values ) {
			toOson( elementMappingType, value, options, osonGen);
		}
		osonGen.writeEnd();
	}

	private <X>void toOson(MappingType mappedType,
						Object value, WrapperOptions options, OracleJsonGenerator osonGen) {
		if (value == null) {
			osonGen.writeNull();
		}
		else if ( mappedType instanceof EmbeddableMappingType ) {
			toOson( (X) value, osonGen, options,(EmbeddableMappingType) mappedType );
		}
		else if ( mappedType instanceof BasicType<?> ) {
			//noinspection unchecked
			final BasicType<Object> basicType = (BasicType<Object>) mappedType;
			convertedBasicValueToOson(basicType.convertToRelationalValue( value ),
					options, osonGen, basicType);
		}
		else {
			throw new UnsupportedOperationException( "Support for mapping type not yet implemented: " + mappedType.getClass().getName() );
		}
	}

	private void convertedBasicValueToOson(Object value,
										WrapperOptions options,
										OracleJsonGenerator osonGen,
										BasicType<Object> basicType) {
		serializeValue(
				value,
				(JavaType<Object>) basicType.getJdbcJavaType(),
				basicType.getJdbcType(),
				options,
				osonGen
		);
	}

	private void serializeValue(Object value, JavaType<Object> jdbcJavaType, JdbcType jdbcType, WrapperOptions options, OracleJsonGenerator osonGen) {
		//TODO: remove me.
	}

	public void __arrayToOson(OracleJsonGenerator osonGen,
							JavaType<?> elementJavaType,
							JdbcType elementJdbcType,
							Object[] values,
							WrapperOptions options) {
		if ( values.length == 0 ) {
			osonGen.writeStartArray();
			osonGen.writeEnd();
		}

		osonGen.writeStartArray();
		for ( Object value : values ) {
			//noinspection unchecked
			convertedValueToOson((JavaType<Object>) elementJavaType, elementJdbcType, value, options, osonGen);
		}
		osonGen.writeEnd();
	}

	private void convertedValueToOson(JavaType<Object> javaType,
									JdbcType jdbcType,
									Object value,
									WrapperOptions options,
									OracleJsonGenerator osonGen) {
		if ( value == null ) {
			osonGen.writeNull();
		}
		else if ( jdbcType instanceof AggregateJdbcType aggregateJdbcType ) {
			toOson(value,  osonGen, options, aggregateJdbcType.getEmbeddableMappingType());
		}
		else {
			serializeValue( value, javaType, jdbcType, options, osonGen );
		}
	}

	private <X> void toOson(X value, OracleJsonGenerator generator, WrapperOptions options, EmbeddableMappingType embeddableMappingType) {
		generator.writeStartObject();
		toOsonUtil( value, generator, options,embeddableMappingType );
		generator.writeEnd();
	}

	private <X> void toOsonUtil(X value,
								OracleJsonGenerator generator,
								WrapperOptions options,
								EmbeddableMappingType embeddableMappingType) {

		final Object[] values = embeddableMappingType.getValues( value );
		for ( int i = 0; i < values.length; i++ ) {
			final ValuedModelPart attributeMapping = getEmbeddedPart( embeddableMappingType, i );
			if ( attributeMapping instanceof SelectableMapping ) {
				final String name = ( (SelectableMapping) attributeMapping ).getSelectableName();

				generator.writeKey( name );
				toOson( attributeMapping.getMappedType(), values[i], options,generator );

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
					toOsonUtil( (X) values[i],
							generator,
							options,
							mappingType );
				}
				else {
					// non flattened case
					final String name = aggregateMapping.getSelectableName();
					generator.writeKey( name );
					generator.writeStartObject();
					toOsonUtil( (X) values[i],
							generator,
							options,
							mappingType);
					generator.writeEnd();

				}

			}
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

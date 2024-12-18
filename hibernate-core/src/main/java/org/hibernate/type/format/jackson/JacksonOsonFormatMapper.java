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
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author Emmanuel Jannetti
 * @auther Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private final ObjectMapper objectMapper;
	private final EmbeddableMappingType embeddableMappingType;

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
		if (this.embeddableMappingType != null) {
			this.objectMapper.setAnnotationIntrospector(
					new JacksonJakartaAnnotationIntrospector( this.embeddableMappingType ) );
		}
	}

	/**
	 * Process OSON parser tokens
	 * @param osonParser the OSON parser
	 * @param currentEvent the current of the parser
	 * @param finalResult the populated object array
	 * @param embeddableMappingType the embeddable mapping definitions
	 * @param options the wrapping options
	 * @throws IOException
	 */
	private void consumeOsonTokens(OracleJsonParser osonParser, OracleJsonParser.Event currentEvent, Object [] finalResult, EmbeddableMappingType embeddableMappingType, WrapperOptions options)
			throws IOException {

		OracleJsonParser.Event event = currentEvent;

		int selectableIndex = -1;
		SelectableMapping mapping = null;
		String currentKeyName = null;
		while ( true ) {
			if ( event == null ) {
				break;
			}
			switch ( event ) {
				case OracleJsonParser.Event.KEY_NAME:
					currentKeyName = osonParser.getString();
					selectableIndex = embeddableMappingType.getSelectableIndex( currentKeyName);
					if (selectableIndex >= 0) {
						// we may not have a selectable mapping for that key
						mapping = embeddableMappingType.getJdbcValueSelectable( selectableIndex );
					}
					break;
				case OracleJsonParser.Event.START_ARRAY:
				case OracleJsonParser.Event.END_ARRAY:
					int i = 0;
					break;
				case OracleJsonParser.Event.VALUE_DATE :
					LocalDateTime localDate = osonParser.getLocalDateTime();
					finalResult[selectableIndex] =  java.sql.Date.valueOf(localDate.toLocalDate());
					break;
				case OracleJsonParser.Event.VALUE_TIMESTAMP:
					LocalDateTime local = osonParser.getLocalDateTime();
					if ("java.sql.Timestamp".equals(
							embeddableMappingType.getJdbcValueSelectable( selectableIndex )
									.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName())) {
						finalResult[selectableIndex] = Timestamp.valueOf( local );
					} else {
						finalResult[selectableIndex] = local;
					}
					break;
				case OracleJsonParser.Event.VALUE_TIMESTAMPTZ:
					finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
							mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getOffsetDateTime(), options ) );
					break;
				case OracleJsonParser.Event.VALUE_INTERVALDS:
				case OracleJsonParser.Event.VALUE_INTERVALYM:
					Duration duration = osonParser.getDuration();
					finalResult[selectableIndex] = duration;
					break;
				case OracleJsonParser.Event.VALUE_STRING:
					finalResult[selectableIndex] =
							mapping.getJdbcMapping().getJdbcJavaType().fromString( osonParser.getString() );
					break;
				case OracleJsonParser.Event.VALUE_TRUE:
					finalResult[selectableIndex] = Boolean.TRUE;
					break;
				case OracleJsonParser.Event.VALUE_FALSE:
					finalResult[selectableIndex] = Boolean.FALSE;
					break;
				case OracleJsonParser.Event.VALUE_NULL:
					finalResult[selectableIndex] = null;
					break;
				case OracleJsonParser.Event.VALUE_DECIMAL:
					if (osonParser.isIntegralNumber()) {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getInt(), options ) );
					} else {
						finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
								mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getFloat(), options ) );
					}
					break;
				case OracleJsonParser.Event.VALUE_DOUBLE:
				case OracleJsonParser.Event.VALUE_FLOAT:
					finalResult[selectableIndex] = mapping.getJdbcMapping().convertToDomainValue(
							mapping.getJdbcMapping().getJdbcJavaType().wrap( osonParser.getFloat(), options ) );
					break;
				case OracleJsonParser.Event.VALUE_BINARY:
					byte[] bytes = osonParser.getBytes();
					if ("java.util.UUID".equals(
							embeddableMappingType.getJdbcValueSelectable( selectableIndex )
									.getJdbcMapping().getJdbcJavaType().getJavaType().getTypeName())) {
						ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
						long mostSignificantBits = byteBuffer.getLong();
						long leastSignificantBits = byteBuffer.getLong();
						finalResult[selectableIndex] = new UUID(mostSignificantBits, leastSignificantBits);
					} else {
						finalResult[selectableIndex] = bytes;
					}
					break;
				case OracleJsonParser.Event.START_OBJECT:
					if ( currentKeyName == null ) {
						// that's the root
						consumeOsonTokens( osonParser, osonParser.next(), finalResult,
								embeddableMappingType,
								options  );
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
					throw new IOException("Unknown OSON event " + event);

			}
			event = osonParser.next();
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
	public <T> T readToArray(EmbeddableMappingType embeddableMappingType, Object source, WrapperOptions options) throws IOException {
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

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws IOException {

		ObjectWriter writer = objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) );
		writer.writeValue( (JsonGenerator) target, value);
	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
		JsonParser osonParser = objectMapper.getFactory().createParser( (byte[]) source );

		T t = objectMapper.readValue( osonParser, objectMapper.constructType( javaType.getJavaType()) );

		return t;
	}
}

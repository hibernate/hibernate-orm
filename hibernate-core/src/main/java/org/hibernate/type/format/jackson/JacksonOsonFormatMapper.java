/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import oracle.jdbc.provider.oson.OsonModule;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;

import java.io.IOException;
import java.lang.reflect.Type;


/**
 * Implementation of FormatMapper for Oracle OSON support
 *
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public final class JacksonOsonFormatMapper extends AbstractJsonFormatMapper {

	public static final String SHORT_NAME = "jackson-oson";

	private final ObjectMapper objectMapper;

	/**
	 * Creates a new JacksonOsonFormatMapper
	 */
	public JacksonOsonFormatMapper() {
		this( new ObjectMapper().findAndRegisterModules() );
	}

	public JacksonOsonFormatMapper(ObjectMapper objectMapper) {
		objectMapper.registerModule( new OsonModule() );
		objectMapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
		this.objectMapper = objectMapper;
	}

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws IOException {
		objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) )
				.writeValue( (JsonGenerator) target, value );
	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {
		return objectMapper.readValue( (JsonParser) source, objectMapper.constructType( javaType.getJavaType() ) );
	}

	@Override
	public boolean supportsSourceType(Class<?> sourceType) {
		return JsonParser.class.isAssignableFrom( sourceType );
	}

	@Override
	public boolean supportsTargetType(Class<?> targetType) {
		return JsonGenerator.class.isAssignableFrom( targetType );
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

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author Christian Beikov
 * @author Yanming Zhou
 */
public class JacksonJsonFormatMapper extends AbstractJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private final ObjectMapper objectMapper;

	public JacksonJsonFormatMapper() {
		this(new ObjectMapper().findAndRegisterModules());
	}

	public JacksonJsonFormatMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
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

		try {
			objectMapper.writerFor(
					objectMapper.constructType( javaType.getJavaType() ) ).writeValueAsString( value );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType.getJavaType(), e );
		}

	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws IOException {

		try {
			return objectMapper.readValue( ((CharSequence)source).toString(), objectMapper.constructType( javaType.getJavaType() ) );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + javaType.getJavaType(), e );
		}

	}
}

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
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;

import java.io.IOException;
import java.lang.reflect.Type;

public class JacksonOsonFormatMapper extends AbstractJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";

	private final ObjectMapper objectMapper;
	private final EmbeddableMappingType embeddableMappingType;



	public JacksonOsonFormatMapper(ObjectMapper objectMapper, EmbeddableMappingType embeddableMappingType) {
		this.objectMapper = objectMapper;
		this.embeddableMappingType = embeddableMappingType;
		this.objectMapper.setAnnotationIntrospector( new JacksonJakartaAnnotationIntrospector( this.embeddableMappingType ) );

	}

	public <T> JacksonOsonFormatMapper(ObjectMapper objectMapper, EmbeddableMappingType embeddableMappingType, JavaType<T> javaType) {
		this.objectMapper = objectMapper;
		this.embeddableMappingType = embeddableMappingType;
		this.objectMapper.setAnnotationIntrospector( new JacksonJakartaAnnotationIntrospector( this.embeddableMappingType ) );

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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.SerializationFeature;
import oracle.jdbc.provider.oson.OsonModule;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.io.IOException;


/**
 * Implementation of FormatMapper for Oracle OSON support
 *
 * @author Emmanuel Jannetti
 * @author Bidyadhar Mohanty
 */
public class JacksonOsonFormatMapper extends JacksonJsonFormatMapper {

	public static final String SHORT_NAME = "jackson";


	/**
	 * Creates a new JacksonOsonFormatMapper
	 */
	public JacksonOsonFormatMapper() {
		super();
		objectMapper.registerModule( new OsonModule() );
		objectMapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws IOException {
		objectMapper.writerFor( objectMapper.constructType( javaType.getJavaType() ) ).writeValue( (JsonGenerator) target, value);

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

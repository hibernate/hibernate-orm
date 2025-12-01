/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.AbstractJsonFormatMapper;
import org.hibernate.type.format.FormatMapperCreationContext;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author Christian Beikov
 * @author Yanming Zhou
 * @author Nick Rayburn
 */
public final class Jackson3JsonFormatMapper extends AbstractJsonFormatMapper {

	public static final String SHORT_NAME = "jackson3";

	private final JsonMapper jsonMapper;

	public Jackson3JsonFormatMapper() {
		this( MapperBuilder.findModules( Jackson3JsonFormatMapper.class.getClassLoader() ) );
	}

	public Jackson3JsonFormatMapper(FormatMapperCreationContext creationContext) {
		this( JacksonIntegration.loadJackson3Modules( creationContext ) );
	}

	private Jackson3JsonFormatMapper(List<JacksonModule> modules) {
		this( JsonMapper.builderWithJackson2Defaults()
				.addModules( modules )
				.build()
		);
	}

	public Jackson3JsonFormatMapper(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options)
			throws JacksonException {
		jsonMapper.writerFor( jsonMapper.constructType( javaType.getJavaType() ) )
				.writeValue( (JsonGenerator) target, value );
	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) throws JacksonException {
		return jsonMapper.readValue( (JsonParser) source, jsonMapper.constructType( javaType.getJavaType() ) );
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
			return jsonMapper.readValue( charSequence.toString(), jsonMapper.constructType( type ) );
		}
		catch (JacksonException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + type, e );
		}
	}

	@Override
	public <T> String toString(T value, Type type) {
		try {
			return jsonMapper.writerFor( jsonMapper.constructType( type ) ).writeValueAsString( value );
		}
		catch (JacksonException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + type, e );
		}
	}
}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import java.io.IOException;


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

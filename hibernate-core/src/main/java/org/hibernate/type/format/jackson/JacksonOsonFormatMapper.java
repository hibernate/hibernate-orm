/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializationFeature;
import oracle.sql.json.OracleJsonFactory;
import oracle.sql.json.OracleJsonGenerator;
import org.hibernate.dialect.JsonHelper;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonJdbcType;
import org.hibernate.type.format.ObjectArrayOsonDocumentWriter;

import java.io.ByteArrayOutputStream;
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

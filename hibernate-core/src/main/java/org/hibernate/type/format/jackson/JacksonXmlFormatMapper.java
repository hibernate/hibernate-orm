/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.format.jackson;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.Module;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import org.hibernate.type.format.FormatMapperCreationContext;

/**
 * @author Christian Beikov
 */
public final class JacksonXmlFormatMapper implements FormatMapper {

	public static final String SHORT_NAME = "jackson-xml";

	private final ObjectMapper objectMapper;

	public JacksonXmlFormatMapper() {
		this(
				createXmlMapper( XmlMapper.findModules( JacksonXmlFormatMapper.class.getClassLoader() ) )
		);
	}

	public JacksonXmlFormatMapper(FormatMapperCreationContext creationContext) {
		this(
				createXmlMapper(
						creationContext.getBootstrapContext()
								.getServiceRegistry()
								.requireService( ClassLoaderService.class )
								.<List<Module>>workWithClassLoader( XmlMapper::findModules )
				)
		);
	}

	public JacksonXmlFormatMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	private static XmlMapper createXmlMapper(List<Module> modules) {
		final XmlMapper xmlMapper = new XmlMapper();
		// needed to automatically find and register Jackson's jsr310 module for java.time support
		xmlMapper.registerModules( modules );
		xmlMapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
		xmlMapper.enable( ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL );
		// Workaround for null vs empty string handling inside arrays,
		// see: https://github.com/FasterXML/jackson-dataformat-xml/issues/344
		final SimpleModule module = new SimpleModule();
		module.addDeserializer( String[].class, new StringArrayDeserializer() );
		xmlMapper.registerModule( module );
		return xmlMapper;
	}

	@Override
	public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (T) charSequence.toString();
		}
		try {
			return objectMapper.readValue(
					charSequence.toString(),
					objectMapper.constructType( javaType.getJavaType() )
			);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + javaType, e );
		}
	}

	@Override
	public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (String) value;
		}
		return writeValueAsString( value, javaType, javaType.getJavaType() );
	}

	private <T> String writeValueAsString(Object value, JavaType<T> javaType, Type type) {
		try {
			return objectMapper.writerFor( objectMapper.constructType( type ) ).writeValueAsString( value );
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType, e );
		}
	}

	private static class StringArrayDeserializer extends JsonDeserializer<String[]> {
		@Override
		public String[] deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
			final ArrayList<String> result = new ArrayList<>();
			JsonToken token;
			while ( ( token = jp.nextValue() ) != JsonToken.END_OBJECT ) {
				if ( token.isScalarValue() ) {
					result.add( jp.getValueAsString() );
				}
			}
			return result.toArray( String[]::new );
		}
	}
}

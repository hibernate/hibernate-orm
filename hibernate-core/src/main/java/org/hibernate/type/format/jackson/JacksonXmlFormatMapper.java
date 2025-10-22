/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
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
import org.hibernate.type.internal.ParameterizedTypeImpl;

/**
 * @author Christian Beikov
 * @author Emmanuel Jannetti
 */
public final class JacksonXmlFormatMapper implements FormatMapper {

	public static final String SHORT_NAME = "jackson-xml";
	private final boolean legacyFormat;

	private final ObjectMapper objectMapper;

	public JacksonXmlFormatMapper() {
		this( true );
	}

	public JacksonXmlFormatMapper(boolean legacyFormat) {
		this(
				createXmlMapper( XmlMapper.findModules( JacksonXmlFormatMapper.class.getClassLoader() ), legacyFormat ),
				legacyFormat
		);
	}

	public JacksonXmlFormatMapper(FormatMapperCreationContext creationContext) {
		this(
				createXmlMapper(
						creationContext.getBootstrapContext()
								.getClassLoaderService()
								.<List<Module>>workWithClassLoader( XmlMapper::findModules ),
						creationContext.getBootstrapContext()
								.getMetadataBuildingOptions()
								.isXmlFormatMapperLegacyFormatEnabled()
				),
				creationContext.getBootstrapContext()
						.getMetadataBuildingOptions()
						.isXmlFormatMapperLegacyFormatEnabled()
		);
	}

	public JacksonXmlFormatMapper(ObjectMapper objectMapper) {
		this( objectMapper, false );
	}

	public JacksonXmlFormatMapper(ObjectMapper objectMapper, boolean legacyFormat) {
		this.objectMapper = objectMapper;
		this.legacyFormat = legacyFormat;
	}

	private static XmlMapper createXmlMapper(List<Module> modules, boolean legacyFormat) {
		final XmlMapper xmlMapper = new XmlMapper();
		// needed to automatically find and register Jackson's jsr310 module for java.time support
		xmlMapper.registerModules( modules );
		xmlMapper.configure( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false );
		xmlMapper.enable( ToXmlGenerator.Feature.WRITE_NULLS_AS_XSI_NIL );
		// Workaround for null vs empty string handling inside arrays,
		// see: https://github.com/FasterXML/jackson-dataformat-xml/issues/344
		final SimpleModule module = new SimpleModule();
		module.addDeserializer( String[].class, new StringArrayDeserializer() );
		if ( !legacyFormat ) {
			module.addDeserializer( byte[].class, new ByteArrayDeserializer() );
			module.addSerializer( byte[].class, new ByteArraySerializer() );
		}
		xmlMapper.registerModule( module );
		return xmlMapper;
	}

	@Override
	public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (T) charSequence.toString();
		}
		try {
			if ( !legacyFormat ) {
				if ( Map.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
					final Type keyType;
					final Type elementType;
					if ( javaType.getJavaType() instanceof ParameterizedType parameterizedType ) {
						keyType = parameterizedType.getActualTypeArguments()[0];
						elementType = parameterizedType.getActualTypeArguments()[1];
					}
					else {
						keyType = Object.class;
						elementType = Object.class;
					}
					final MapWrapper<?, ?> collectionWrapper = objectMapper.readValue(
							charSequence.toString(),
							objectMapper.constructType( new ParameterizedTypeImpl( MapWrapper.class,
									new Type[] {keyType, elementType}, null ) )
					);
					final Map<Object, Object> map = new LinkedHashMap<>( collectionWrapper.entry.size() );
					for ( EntryWrapper<?, ?> entry : collectionWrapper.entry ) {
						map.put( entry.key, entry.value );
					}
					return javaType.wrap( map, wrapperOptions );
				}
				else if ( Collection.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
					final Type elementType =
							javaType.getJavaType() instanceof ParameterizedType parameterizedType
									? parameterizedType.getActualTypeArguments()[0]
									: Object.class;
					final CollectionWrapper<?> collectionWrapper = objectMapper.readValue(
							charSequence.toString(),
							objectMapper.constructType(
									new ParameterizedTypeImpl( CollectionWrapper.class, new Type[] {elementType},
											null ) )
					);
					return javaType.wrap( collectionWrapper.value, wrapperOptions );
				}
				else if ( javaType.getJavaTypeClass().isArray() ) {
					final CollectionWrapper<?> collectionWrapper = objectMapper.readValue(
							charSequence.toString(),
							objectMapper.constructType( new ParameterizedTypeImpl( CollectionWrapper.class,
									new Type[] {javaType.getJavaTypeClass().getComponentType()}, null ) )
					);
					return javaType.wrap( collectionWrapper.value, wrapperOptions );
				}
			}
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
		if ( !legacyFormat ) {
			if ( Map.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final Type keyType;
				final Type elementType;
				if ( javaType.getJavaType() instanceof ParameterizedType parameterizedType ) {
					keyType = parameterizedType.getActualTypeArguments()[0];
					elementType = parameterizedType.getActualTypeArguments()[1];
				}
				else {
					keyType = Object.class;
					elementType = Object.class;
				}
				final MapWrapper<Object, Object> mapWrapper = new MapWrapper<>();
				for ( Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet() ) {
					mapWrapper.entry.add( new EntryWrapper<>( entry.getKey(), entry.getValue() ) );
				}
				return writeValueAsString(
						mapWrapper,
						javaType,
						new ParameterizedTypeImpl( MapWrapper.class, new Type[] {keyType, elementType}, null )
				);
			}
			else if ( Collection.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final Type elementType =
						javaType.getJavaType() instanceof ParameterizedType parameterizedType
								? parameterizedType.getActualTypeArguments()[0]
								: Object.class;
				return writeValueAsString(
						new CollectionWrapper<>( (Collection<?>) value ),
						javaType,
						new ParameterizedTypeImpl( CollectionWrapper.class, new Type[] {elementType}, null )
				);
			}
			else if ( javaType.getJavaTypeClass().isArray() ) {
				final CollectionWrapper<Object> collectionWrapper;
				if ( Object[].class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
					collectionWrapper = new CollectionWrapper<>( Arrays.asList( (Object[]) value ) );
				}
				else {
					// Primitive arrays get a special treatment
					final int length = Array.getLength( value );
					final List<Object> list = new ArrayList<>( length );
					for ( int i = 0; i < length; i++ ) {
						list.add( Array.get( value, i ) );
					}
					collectionWrapper = new CollectionWrapper<>( list );
				}
				return writeValueAsString(
						collectionWrapper,
						javaType,
						new ParameterizedTypeImpl( CollectionWrapper.class,
								new Type[] {javaType.getJavaTypeClass().getComponentType()}, null )
				);
			}
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

	@JacksonXmlRootElement(localName = "Collection")
	public static class CollectionWrapper<E> {
		@JacksonXmlElementWrapper(useWrapping = false)
		@JacksonXmlProperty(localName = "e")
		Collection<E> value;

		public CollectionWrapper() {
			this.value = new ArrayList<>();
		}

		public CollectionWrapper(Collection<E> value) {
			this.value = value;
		}
	}

	@JacksonXmlRootElement(localName = "Map")
	public static class MapWrapper<K, V> {
		@JacksonXmlElementWrapper(useWrapping = false)
		@JacksonXmlProperty(localName = "e")
		Collection<EntryWrapper<K, V>> entry;

		public MapWrapper() {
			this.entry = new ArrayList<>();
		}

		public MapWrapper(Collection<EntryWrapper<K, V>> entry) {
			this.entry = entry;
		}
	}

	public static class EntryWrapper<K, V> {
		@JacksonXmlProperty(localName = "k")
		K key;
		@JacksonXmlProperty(localName = "v")
		V value;

		public EntryWrapper() {
		}

		public EntryWrapper(K key, V value) {
			this.key = key;
			this.value = value;
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

	private static class ByteArrayDeserializer extends JsonDeserializer<byte[]> {
		@Override
		public byte[] deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException {
			return PrimitiveByteArrayJavaType.INSTANCE.fromString( jp.getValueAsString() );
		}
	}

	public static class ByteArraySerializer extends StdSerializer<byte[]> {

		public ByteArraySerializer() {
			super( byte[].class );
		}

		@Override
		public boolean isEmpty(SerializerProvider prov, byte[] value) {
			return value.length == 0;
		}

		@Override
		public void serialize(byte[] value, JsonGenerator g, SerializerProvider provider) throws IOException {
			g.writeString( PrimitiveByteArrayJavaType.INSTANCE.toString( value ) );
		}
	}
}

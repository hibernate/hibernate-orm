/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jackson;

import com.fasterxml.jackson.annotation.JsonRootName;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.FormatMapperCreationContext;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christian Beikov
 * @author Emmanuel Jannetti
 * @author Nick Rayburn
 */
public final class Jackson3XmlFormatMapper implements FormatMapper {

	public static final String SHORT_NAME = "jackson3-xml";
	private final boolean legacyFormat;

	private final XmlMapper xmlMapper;

	public Jackson3XmlFormatMapper() {
		this( true );
	}

	public Jackson3XmlFormatMapper(boolean legacyFormat) {
		this(
				createXmlMapper( MapperBuilder.findModules( Jackson3XmlFormatMapper.class.getClassLoader() ), legacyFormat ),
				legacyFormat
		);
	}

	public Jackson3XmlFormatMapper(FormatMapperCreationContext creationContext) {
		this(
				createXmlMapper(
						JacksonIntegration.loadJackson3Modules( creationContext ),
						creationContext.getBootstrapContext()
								.getMetadataBuildingOptions()
								.isXmlFormatMapperLegacyFormatEnabled()
				),
				creationContext.getBootstrapContext()
						.getMetadataBuildingOptions()
						.isXmlFormatMapperLegacyFormatEnabled()
		);
	}

	public Jackson3XmlFormatMapper(XmlMapper xmlMapper) {
		this( xmlMapper, false );
	}

	public Jackson3XmlFormatMapper(XmlMapper xmlMapper, boolean legacyFormat) {
		this.xmlMapper = xmlMapper;
		this.legacyFormat = legacyFormat;
	}

	private static XmlMapper createXmlMapper(List<JacksonModule> modules, boolean legacyFormat) {
		final XmlMapper.Builder builder = XmlMapper.builderWithJackson2Defaults()
				.disable( DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS )
				.enable( XmlWriteFeature.WRITE_NULLS_AS_XSI_NIL )
				.addModules( modules );
		// Workaround for null vs empty string handling inside arrays,
		// see: https://github.com/FasterXML/jackson-dataformat-xml/issues/344
		final SimpleModule module = new SimpleModule();
		module.addDeserializer( String[].class, new StringArrayDeserializer() );
		if ( !legacyFormat ) {
			module.addDeserializer( byte[].class, new ByteArrayDeserializer() );
			module.addSerializer( byte[].class, new ByteArraySerializer() );
		}
		builder.addModule( module );
		return builder.build();
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
					final MapWrapper<?, ?> collectionWrapper = xmlMapper.readValue(
							charSequence.toString(),
							xmlMapper.constructType( new ParameterizedTypeImpl( MapWrapper.class,
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
					final CollectionWrapper<?> collectionWrapper = xmlMapper.readValue(
							charSequence.toString(),
							xmlMapper.constructType(
									new ParameterizedTypeImpl( CollectionWrapper.class, new Type[] {elementType},
											null ) )
					);
					return javaType.wrap( collectionWrapper.value, wrapperOptions );
				}
				else if ( javaType.getJavaTypeClass().isArray() ) {
					final CollectionWrapper<?> collectionWrapper = xmlMapper.readValue(
							charSequence.toString(),
							xmlMapper.constructType( new ParameterizedTypeImpl( CollectionWrapper.class,
									new Type[] {javaType.getJavaTypeClass().getComponentType()}, null ) )
					);
					return javaType.wrap( collectionWrapper.value, wrapperOptions );
				}
			}
			return xmlMapper.readValue(
					charSequence.toString(),
					xmlMapper.constructType( javaType.getJavaType() )
			);
		}
		catch (JacksonException e) {
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
			return xmlMapper.writerFor( xmlMapper.constructType( type ) ).writeValueAsString( value );
		}
		catch (JacksonException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType, e );
		}
	}

	@JsonRootName(value = "Collection")
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

	@JsonRootName(value = "Map")
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

	private static class StringArrayDeserializer extends ValueDeserializer<String[]> {
		@Override
		public String[] deserialize(JsonParser jp, DeserializationContext deserializationContext) throws JacksonException {
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

	private static class ByteArrayDeserializer extends ValueDeserializer<byte[]> {
		@Override
		public byte[] deserialize(JsonParser jp, DeserializationContext deserializationContext) throws JacksonException {
			return PrimitiveByteArrayJavaType.INSTANCE.fromString( jp.getValueAsString() );
		}
	}

	public static class ByteArraySerializer extends StdSerializer<byte[]> {

		public ByteArraySerializer() {
			super( byte[].class );
		}

		@Override
		public boolean isEmpty(SerializationContext prov, byte[] value) {
			return value.length == 0;
		}

		@Override
		public void serialize(byte[] value, JsonGenerator g, SerializationContext provider) throws JacksonException {
			g.writeString( PrimitiveByteArrayJavaType.INSTANCE.toString( value ) );
		}
	}
}

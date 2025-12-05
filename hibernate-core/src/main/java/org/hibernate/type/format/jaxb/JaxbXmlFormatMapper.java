/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format.jaxb;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.xml.bind.annotation.XmlElement;
import org.hibernate.type.descriptor.jdbc.XmlHelper;
import org.hibernate.internal.build.AllowReflection;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.type.descriptor.java.JavaTypeHelper;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicPluralJavaType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.JAXBIntrospector;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Christian Beikov
 */
public final class JaxbXmlFormatMapper implements FormatMapper {

	public static final String SHORT_NAME = "jaxb";
	private final boolean legacyFormat;
	private final String collectionElementTagName;
	private final String mapKeyTagName;
	private final String mapValueTagName;

	public JaxbXmlFormatMapper() {
		this( true );
	}

	public JaxbXmlFormatMapper(boolean legacyFormat) {
		this.legacyFormat = legacyFormat;
		if ( legacyFormat ) {
			collectionElementTagName = "value";
			mapKeyTagName = "key";
			mapValueTagName = "value";
		}
		else {
			collectionElementTagName = XmlHelper.ROOT_TAG;
			mapKeyTagName = "k";
			mapValueTagName = "v";
		}
	}

	@Override
	@AllowReflection
	public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (T) charSequence.toString();
		}
		try {
			// No need for an appender here, but we need it for creating a transformer
			final StringBuilderSqlAppender appender = null;
			if ( Map.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final JAXBContext context;
				final Class<Object> keyClass;
				final Class<Object> valueClass;
				if ( javaType.getJavaType() instanceof ParameterizedType parameterizedType ) {
					final Type[] typeArguments = parameterizedType.getActualTypeArguments();
					keyClass = ReflectHelper.getClass( typeArguments[0] );
					valueClass = ReflectHelper.getClass( typeArguments[1] );
					if ( legacyFormat ) {
						context = JAXBContext.newInstance( LegacyMapWrapper.class, keyClass, valueClass );
					}
					else {
						context = JAXBContext.newInstance( MapWrapper.class, EntryWrapper.class, keyClass, valueClass );
					}
				}
				else {
					keyClass = Object.class;
					valueClass = Object.class;
					if ( legacyFormat ) {
						context = JAXBContext.newInstance( LegacyMapWrapper.class );
					}
					else {
						context = JAXBContext.newInstance( MapWrapper.class, EntryWrapper.class );
					}
				}
				final Unmarshaller unmarshaller = context.createUnmarshaller();
				final ManagedMapWrapper mapWrapper = (ManagedMapWrapper) unmarshaller
						.unmarshal( new StringReader( charSequence.toString() ) );
				final Map<Object, Object> map = CollectionHelper.linkedMapOfSize( mapWrapper.size() >> 1 );
				final JAXBIntrospector jaxbIntrospector = context.createJAXBIntrospector();
				final JAXBElementTransformer keyTransformer;
				final JAXBElementTransformer valueTransformer;
				if ( javaType instanceof BasicPluralJavaType<?> ) {
					keyTransformer = createTransformer(
							appender,
							keyClass,
							mapKeyTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
					valueTransformer = createTransformer(
							appender,
							( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
							mapValueTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				else {
					keyTransformer = createTransformer(
							appender,
							keyClass,
							mapKeyTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
					valueTransformer = createTransformer(
							appender,
							valueClass,
							mapValueTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				if ( legacyFormat ) {
					final Collection<Object> elements = ( (LegacyMapWrapper) mapWrapper).elements;
					for ( final Iterator<Object> iterator = elements.iterator(); iterator.hasNext(); ) {
						final Object key = keyTransformer.fromJAXBElement( iterator.next(), unmarshaller );
						final Object value = valueTransformer.fromJAXBElement( iterator.next(), unmarshaller );
						map.put( key, value );
					}
				}
				else {
					for ( EntryWrapper entry : ((MapWrapper) mapWrapper).entries ) {
						final Object key = keyTransformer.fromXmlContent( entry.key );
						final Object value = valueTransformer.fromXmlContent( entry.value );
						map.put( key, value );
					}
				}
				return javaType.wrap( map, wrapperOptions );
			}
			else if ( Collection.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final JAXBContext context;
				final Class<Object> valueClass;
				if ( javaType.getJavaType() instanceof ParameterizedType parameterizedType ) {
					final Type[] typeArguments = parameterizedType.getActualTypeArguments();
					valueClass = ReflectHelper.getClass( typeArguments[0] );
					context = JAXBContext.newInstance( CollectionWrapper.class, valueClass );
				}
				else {
					valueClass = Object.class;
					context = JAXBContext.newInstance( CollectionWrapper.class );
				}
				final Unmarshaller unmarshaller = context.createUnmarshaller();
				final CollectionWrapper collectionWrapper = (CollectionWrapper) unmarshaller
						.unmarshal( new StringReader( charSequence.toString() ) );
				final Collection<Object> elements = collectionWrapper.elements;
				final Collection<Object> collection = new ArrayList<>( elements.size() );
				final JAXBIntrospector jaxbIntrospector = context.createJAXBIntrospector();
				final JAXBElementTransformer valueTransformer;
				if ( javaType instanceof BasicPluralJavaType<?> ) {
					valueTransformer = createTransformer(
							appender,
							( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
							collectionElementTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				else {
					valueTransformer = createTransformer(
							appender,
							valueClass,
							collectionElementTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				for ( Object element : elements ) {
					final Object value = valueTransformer.fromJAXBElement( element, unmarshaller );
					collection.add( value );
				}
				return javaType.wrap( collection, wrapperOptions );
			}
			else if ( javaType.getJavaTypeClass().isArray() ) {
				final Class<?> valueClass = javaType.getJavaTypeClass().getComponentType();
				final JAXBContext context = JAXBContext.newInstance( CollectionWrapper.class, valueClass );
				final Unmarshaller unmarshaller = context.createUnmarshaller();
				final CollectionWrapper collectionWrapper = (CollectionWrapper) unmarshaller
						.unmarshal( new StringReader( charSequence.toString() ) );
				final Collection<Object> elements = collectionWrapper.elements;
				final JAXBIntrospector jaxbIntrospector = context.createJAXBIntrospector();
				final JAXBElementTransformer valueTransformer;
				if ( javaType instanceof BasicPluralJavaType<?> ) {
					valueTransformer = createTransformer(
							appender,
							( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
							collectionElementTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				else {
					valueTransformer = createTransformer(
							appender,
							valueClass,
							collectionElementTagName,
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				final int length = elements.size();
				if ( Object[].class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
					final Object[] array = (Object[]) Array.newInstance( valueClass, length );
					int i = 0;
					for ( Object element : elements ) {
						final Object value = valueTransformer.fromJAXBElement( element, unmarshaller );
						array[i] = value;
						i++;
					}
					//noinspection unchecked
					return (T) array;
				}
				else {
					//noinspection unchecked
					final T array = (T) Array.newInstance( valueClass, length );
					int i = 0;
					for ( Object element : elements ) {
						final Object value = valueTransformer.fromJAXBElement( element, unmarshaller );
						Array.set( array, i, value );
						i++;
					}
					return array;
				}
			}
			else {
				final JAXBContext context = JAXBContext.newInstance( javaType.getJavaTypeClass() );
				//noinspection unchecked
				return (T) context.createUnmarshaller().unmarshal( new StringReader( charSequence.toString() ) );
			}
		}
		catch (JAXBException e) {
			throw new IllegalArgumentException( "Could not deserialize string to java type: " + javaType, e );
		}
	}

	@Override
	public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		if ( javaType.getJavaType() == String.class || javaType.getJavaType() == Object.class ) {
			return (String) value;
		}
		try {
			final StringWriter stringWriter = new StringWriter();
			final StringBuilderSqlAppender appender = new StringBuilderSqlAppender();
			if ( Map.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final JAXBContext context;
				final Class<?> keyClass;
				final Class<?> valueClass;
				final Map<?, ?> map = (Map<?, ?>) value;
				if ( javaType.getJavaType() instanceof ParameterizedType parameterizedType ) {
					final Type[] typeArguments = parameterizedType.getActualTypeArguments();
					keyClass = ReflectHelper.getClass( typeArguments[0] );
					valueClass = ReflectHelper.getClass( typeArguments[1] );
					if ( legacyFormat ) {
						context = JAXBContext.newInstance( LegacyMapWrapper.class, keyClass, valueClass );
					}
					else {
						context = JAXBContext.newInstance( MapWrapper.class, EntryWrapper.class, keyClass, valueClass );
					}
				}
				else {
					if ( map.isEmpty() ) {
						keyClass = Object.class;
						valueClass = Object.class;
						context = legacyFormat
								? JAXBContext.newInstance( LegacyMapWrapper.class )
								: JAXBContext.newInstance( MapWrapper.class, EntryWrapper.class );
					}
					else {
						final Map.Entry<?, ?> firstEntry = map.entrySet().iterator().next();
						keyClass = firstEntry.getKey().getClass();
						valueClass = firstEntry.getValue().getClass();
						context = legacyFormat
								? JAXBContext.newInstance( LegacyMapWrapper.class, keyClass, valueClass )
								: JAXBContext.newInstance( MapWrapper.class, EntryWrapper.class, keyClass, valueClass );
					}
				}
				final ManagedMapWrapper managedMapWrapper = legacyFormat ? new LegacyMapWrapper() : new MapWrapper();
				if ( !map.isEmpty() ) {
					Object exampleKey = null;
					Object exampleValue = null;
					for ( Map.Entry<?, ?> entry : map.entrySet() ) {
						final Object mapKey = entry.getKey();
						final Object mapValue = entry.getValue();
						if ( exampleKey == null && mapKey != null ) {
							exampleKey = mapKey;
							if ( exampleValue != null ) {
								break;
							}
						}
						if ( exampleValue == null && mapValue != null ) {
							exampleValue = mapValue;
							if ( exampleKey != null ) {
								break;
							}
						}
					}
					final JAXBIntrospector jaxbIntrospector = context.createJAXBIntrospector();
					final JAXBElementTransformer keyTransformer = createTransformer(
							appender,
							keyClass,
							mapKeyTagName,
							exampleKey,
							jaxbIntrospector,
							wrapperOptions
					);
					final JAXBElementTransformer valueTransformer = createTransformer(
							appender,
							valueClass,
							mapValueTagName,
							exampleValue,
							jaxbIntrospector,
							wrapperOptions
					);
					if ( legacyFormat ) {
						final LegacyMapWrapper legacyMapWrapper = (LegacyMapWrapper) managedMapWrapper;
						for ( Map.Entry<?, ?> entry : map.entrySet() ) {
							legacyMapWrapper.elements.add( keyTransformer.toJAXBElement( entry.getKey() ) );
							legacyMapWrapper.elements.add( valueTransformer.toJAXBElement( entry.getValue() ) );
						}
					}
					else {
						final MapWrapper mapWrapper = (MapWrapper) managedMapWrapper;
						for ( Map.Entry<?, ?> entry : map.entrySet() ) {
							mapWrapper.entries.add( new EntryWrapper(
									(String) keyTransformer.toJAXBElement( entry.getKey() ).getValue(),
									(String) valueTransformer.toJAXBElement( entry.getValue() ).getValue()
							) );
						}
					}
				}
				createMarshaller( context ).marshal( managedMapWrapper, stringWriter );
			}
			else if ( Collection.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final JAXBContext context;
				final Class<Object> valueClass;
				final Collection<?> collection = (Collection<?>) value;
				if ( javaType.getJavaType() instanceof ParameterizedType parameterizedType ) {
					final Type[] typeArguments = parameterizedType.getActualTypeArguments();
					valueClass = ReflectHelper.getClass( typeArguments[0] );
					context = JAXBContext.newInstance( CollectionWrapper.class, valueClass );
				}
				else {
					if ( collection.isEmpty() ) {
						valueClass = Object.class;
						context = JAXBContext.newInstance( CollectionWrapper.class );
					}
					else {
						valueClass = ReflectHelper.getClass( collection.iterator().next() );
						context = JAXBContext.newInstance( CollectionWrapper.class, valueClass );
					}
				}
				final CollectionWrapper collectionWrapper;
				if ( collection.isEmpty() ) {
					collectionWrapper = new CollectionWrapper();
				}
				else {
					collectionWrapper = new CollectionWrapper( new ArrayList<>( collection.size() ) );
					Object exampleValue = null;
					for ( Object o : collection ) {
						if ( o != null ) {
							exampleValue = o;
							break;
						}
					}
					final JAXBElementTransformer valueTransformer;
					if ( javaType instanceof BasicPluralJavaType<?> pluralJavaType ) {
						valueTransformer = createTransformer(
								appender,
								pluralJavaType.getElementJavaType(),
								collectionElementTagName,
								exampleValue,
								context.createJAXBIntrospector(),
								wrapperOptions
						);
					}
					else {
						valueTransformer = createTransformer(
								appender,
								valueClass,
								collectionElementTagName,
								exampleValue,
								context.createJAXBIntrospector(),
								wrapperOptions
						);
					}
					for ( Object o : collection ) {
						collectionWrapper.elements.add( valueTransformer.toJAXBElement( o ) );
					}
				}
				createMarshaller( context ).marshal( collectionWrapper, stringWriter );
			}
			else if ( javaType.getJavaTypeClass().isArray() ) {
				//noinspection unchecked
				final Class<Object> valueClass = (Class<Object>) javaType.getJavaTypeClass().getComponentType();
				final JAXBContext context = JAXBContext.newInstance( CollectionWrapper.class, valueClass );
				final CollectionWrapper collectionWrapper;
				if ( Object[].class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
					final Object[] array = (Object[]) value;
					final List<Object> list = new ArrayList<>( array.length );
					Object exampleElement = null;
					for ( Object o : array ) {
						if ( o != null ) {
							exampleElement = o;
							break;
						}
					}
					final JAXBElementTransformer transformer;
					if ( javaType instanceof BasicPluralJavaType<?> pluralJavaType ) {
						transformer = createTransformer(
								appender,
								pluralJavaType.getElementJavaType(),
								collectionElementTagName,
								exampleElement,
								context.createJAXBIntrospector(),
								wrapperOptions
						);
					}
					else {
						transformer = createTransformer(
								appender,
								valueClass,
								collectionElementTagName,
								exampleElement,
								context.createJAXBIntrospector(),
								wrapperOptions
						);
					}
					for ( Object o : array ) {
						list.add( transformer.toJAXBElement( o ) );
					}
					collectionWrapper = new CollectionWrapper( list );
				}
				else {
					// Primitive arrays get a special treatment
					final int length = Array.getLength( value );
					final List<Object> list = new ArrayList<>( length );
					final JavaTypeJAXBElementTransformer transformer = new JavaTypeJAXBElementTransformer(
							appender,
							( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
							collectionElementTagName
					);
					for ( int i = 0; i < length; i++ ) {
						list.add( transformer.toJAXBElement( Array.get( value, i ) ) );
					}
					collectionWrapper = new CollectionWrapper( list );
				}
				createMarshaller( context ).marshal( collectionWrapper, stringWriter );
			}
			else {
				final JAXBContext context = JAXBContext.newInstance( javaType.getJavaTypeClass() );
				createMarshaller( context ).marshal( value, stringWriter );
			}
			return stringWriter.toString();
		}
		catch (JAXBException e) {
			throw new IllegalArgumentException( "Could not serialize object of java type: " + javaType, e );
		}
	}

	@Override
	public <T> void writeToTarget(T value, JavaType<T> javaType, Object target, WrapperOptions options) {
	}

	@Override
	public <T> T readFromSource(JavaType<T> javaType, Object source, WrapperOptions options) {
		return null;
	}

	private JAXBElementTransformer createTransformer(
			StringBuilderSqlAppender appender,
			Class<?> elementClass,
			String tagName,
			Object exampleElement,
			JAXBIntrospector introspector,
			WrapperOptions wrapperOptions) {
		final JavaType<?> elementJavaType =
				wrapperOptions.getTypeConfiguration().getJavaTypeRegistry()
						.findDescriptor( elementClass );
		if ( exampleElement == null && ( elementJavaType == null || JavaTypeHelper.isUnknown( elementJavaType ) ) ) {
			try {
				final Constructor<?> declaredConstructor = elementClass.getDeclaredConstructor();
				exampleElement = declaredConstructor.newInstance();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		final QName elementName = exampleElement == null ? null : introspector.getElementName( exampleElement );
		if ( elementName == null && elementClass != String.class && elementJavaType != null ) {
			return createTransformer(
					appender,
					elementJavaType,
					tagName,
					exampleElement,
					introspector,
					wrapperOptions
			);
		}
		return new SimpleJAXBElementTransformer( elementClass, tagName );
	}

	private JAXBElementTransformer createTransformer(
			StringBuilderSqlAppender appender,
			JavaType<?> elementJavaType,
			String tagName,
			Object exampleElement,
			JAXBIntrospector introspector,
			WrapperOptions wrapperOptions) {
		if ( exampleElement == null && JavaTypeHelper.isUnknown( elementJavaType ) ) {
			try {
				final Constructor<?> declaredConstructor = elementJavaType.getJavaTypeClass().getDeclaredConstructor();
				exampleElement = declaredConstructor.newInstance();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		final QName elementName = exampleElement == null ? null : introspector.getElementName( exampleElement );
		if ( elementName == null && elementJavaType.getJavaTypeClass() != String.class ) {
			return new JavaTypeJAXBElementTransformer( appender, elementJavaType, tagName );
		}
		return new SimpleJAXBElementTransformer( elementJavaType.getJavaTypeClass(), tagName );
	}

	private Marshaller createMarshaller(JAXBContext context) throws JAXBException {
		final Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
		return marshaller;
	}

	public interface ManagedMapWrapper {
		int size();
	}

	@XmlRootElement(name = "Map")
	public static class LegacyMapWrapper implements ManagedMapWrapper {
		@XmlAnyElement
		Collection<Object> elements;

		public LegacyMapWrapper() {
			this.elements = new ArrayList<>();
		}

		public LegacyMapWrapper(Collection<Object> elements) {
			this.elements = elements;
		}

		@Override
		public int size() {
			return elements.size();
		}
	}

	@XmlRootElement(name = "Map")
	public static class MapWrapper implements ManagedMapWrapper {
		@XmlElement(name = "e")
		Collection<EntryWrapper> entries;

		public MapWrapper() {
			this.entries = new ArrayList<>();
		}

		public MapWrapper(Collection<EntryWrapper> elements) {
			this.entries = elements;
		}

		@Override
		public int size() {
			return entries.size();
		}
	}

	public static class EntryWrapper {
		@XmlElement(name = "k", nillable = true)
		String key;
		@XmlElement(name = "v", nillable = true)
		String value;

		public EntryWrapper() {
		}

		public EntryWrapper(String key, String value) {
			this.key = key;
			this.value = value;
		}
	}

	@XmlRootElement(name = "Collection")
	public static class CollectionWrapper {
		@XmlAnyElement
		Collection<Object> elements;

		public CollectionWrapper() {
			this.elements = new ArrayList<>();
		}

		public CollectionWrapper(Collection<Object> elements) {
			this.elements = elements;
		}
	}

	private interface JAXBElementTransformer {
		JAXBElement<?> toJAXBElement(Object o);
		Object fromJAXBElement(Object element, Unmarshaller unmarshaller) throws JAXBException;
		Object fromXmlContent(String content);
	}

	private static class SimpleJAXBElementTransformer implements JAXBElementTransformer {
		private final Class<Object> elementClass;
		private final QName tagName;

		public SimpleJAXBElementTransformer(Class<?> elementClass, String tagName) {
			//noinspection unchecked
			this.elementClass = (Class<Object>) elementClass;
			this.tagName = new QName( tagName );
		}

		@Override
		public JAXBElement<?> toJAXBElement(Object o) {
			return new JAXBElement<>( tagName, elementClass, o );
		}

		@Override
		public Object fromJAXBElement(Object object, Unmarshaller unmarshaller) throws JAXBException {
			final Object valueElement = unmarshaller.unmarshal( (Node) object, elementClass ).getValue();
			return valueElement instanceof Element element
					? element.getFirstChild().getTextContent()
					: valueElement;
		}

		@Override
		public Object fromXmlContent(String content) {
			return content;
		}
	}

	private static class JavaTypeJAXBElementTransformer implements JAXBElementTransformer {
		private final StringBuilderSqlAppender appender;
		private final JavaType<Object> elementJavaType;
		private final QName tagName;

		public JavaTypeJAXBElementTransformer(
				StringBuilderSqlAppender appender,
				JavaType<?> elementJavaType,
				String tagName) {
			this.appender = appender;
			//noinspection unchecked
			this.elementJavaType = (JavaType<Object>) elementJavaType;
			this.tagName = new QName( tagName );
		}

		@Override
		public JAXBElement<?> toJAXBElement(Object o) {
			final String value;
			if ( o == null ) {
				value = null;
			}
			else {
				elementJavaType.appendEncodedString( appender, o );
				value = appender.toString();
				appender.getStringBuilder().setLength( 0 );
			}
			return new JAXBElement<>( tagName, String.class, value );
		}

		@Override
		public Object fromJAXBElement(Object element, Unmarshaller unmarshaller) throws JAXBException {
			final String value = element == null ? null : unmarshaller.unmarshal( (Node) element, String.class ).getValue();
			return value == null ? null : elementJavaType.fromEncodedString( value, 0, value.length() );
		}

		@Override
		public Object fromXmlContent(String content) {
			return content == null ? null : elementJavaType.fromEncodedString( content, 0, content.length() );
		}
	}
}

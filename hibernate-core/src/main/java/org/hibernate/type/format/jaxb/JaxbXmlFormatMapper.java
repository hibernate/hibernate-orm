/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

	public JaxbXmlFormatMapper() {
	}

	@Override
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
				if ( javaType.getJavaType() instanceof ParameterizedType ) {
					final Type[] typeArguments = ( (ParameterizedType) javaType.getJavaType() ).getActualTypeArguments();
					keyClass = ReflectHelper.getClass( typeArguments[0] );
					valueClass = ReflectHelper.getClass( typeArguments[1] );
					context = JAXBContext.newInstance( MapWrapper.class, keyClass, valueClass );
				}
				else {
					keyClass = Object.class;
					valueClass = Object.class;
					context = JAXBContext.newInstance( MapWrapper.class );
				}
				final Unmarshaller unmarshaller = context.createUnmarshaller();
				final MapWrapper mapWrapper = (MapWrapper) unmarshaller
						.unmarshal( new StringReader( charSequence.toString() ) );
				final Collection<Object> elements = mapWrapper.elements;
				final Map<Object, Object> map = CollectionHelper.linkedMapOfSize( elements.size() >> 1 );
				final JAXBIntrospector jaxbIntrospector = context.createJAXBIntrospector();
				final JAXBElementTransformer keyTransformer;
				final JAXBElementTransformer valueTransformer;
				if ( javaType instanceof BasicPluralJavaType<?> ) {
					keyTransformer = createTransformer(
							appender,
							keyClass,
							"key",
							null,
							jaxbIntrospector,
							wrapperOptions
					);
					valueTransformer = createTransformer(
							appender,
							( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
							"value",
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				else {
					keyTransformer = createTransformer(
							appender,
							keyClass,
							"key",
							null,
							jaxbIntrospector,
							wrapperOptions
					);
					valueTransformer = createTransformer(
							appender,
							valueClass,
							"value",
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				for ( final Iterator<Object> iterator = elements.iterator(); iterator.hasNext(); ) {
					final Object key = keyTransformer.fromJAXBElement( iterator.next(), unmarshaller );
					final Object value = valueTransformer.fromJAXBElement( iterator.next(), unmarshaller );
					map.put( key, value );
				}
				return javaType.wrap( map, wrapperOptions );
			}
			else if ( Collection.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final JAXBContext context;
				final Class<Object> valueClass;
				if ( javaType.getJavaType() instanceof ParameterizedType ) {
					final Type[] typeArguments = ( (ParameterizedType) javaType.getJavaType() ).getActualTypeArguments();
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
							"value",
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				else {
					valueTransformer = createTransformer(
							appender,
							valueClass,
							"value",
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
							"value",
							null,
							jaxbIntrospector,
							wrapperOptions
					);
				}
				else {
					valueTransformer = createTransformer(
							appender,
							valueClass,
							"value",
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
				final Class<Object> keyClass;
				final Class<Object> valueClass;
				final MapWrapper mapWrapper = new MapWrapper();
				final Map<?, ?> map = (Map<?, ?>) value;
				if ( javaType.getJavaType() instanceof ParameterizedType ) {
					final Type[] typeArguments = ( (ParameterizedType) javaType.getJavaType() ).getActualTypeArguments();
					keyClass = ReflectHelper.getClass( typeArguments[0] );
					valueClass = ReflectHelper.getClass( typeArguments[1] );
					context = JAXBContext.newInstance( MapWrapper.class, keyClass, valueClass );
				}
				else {
					if ( map.isEmpty() ) {
						keyClass = Object.class;
						valueClass = Object.class;
						context = JAXBContext.newInstance( MapWrapper.class );
					}
					else {
						final Map.Entry<?, ?> firstEntry = map.entrySet().iterator().next();
						//noinspection unchecked
						keyClass = (Class<Object>) firstEntry.getKey().getClass();
						//noinspection unchecked
						valueClass = (Class<Object>) firstEntry.getValue().getClass();
						context = JAXBContext.newInstance( MapWrapper.class, keyClass, valueClass );
					}
				}
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
							"key",
							exampleKey,
							jaxbIntrospector,
							wrapperOptions
					);
					final JAXBElementTransformer valueTransformer = createTransformer(
							appender,
							valueClass,
							"value",
							exampleValue,
							jaxbIntrospector,
							wrapperOptions
					);
					for ( Map.Entry<?, ?> entry : map.entrySet() ) {
						mapWrapper.elements.add( keyTransformer.toJAXBElement( entry.getKey() ) );
						mapWrapper.elements.add( valueTransformer.toJAXBElement( entry.getValue() ) );
					}
				}
				createMarshaller( context ).marshal( mapWrapper, stringWriter );
			}
			else if ( Collection.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final JAXBContext context;
				final Class<Object> valueClass;
				final Collection<?> collection = (Collection<?>) value;
				if ( javaType.getJavaType() instanceof ParameterizedType ) {
					final Type[] typeArguments = ( (ParameterizedType) javaType.getJavaType() ).getActualTypeArguments();
					valueClass = ReflectHelper.getClass( typeArguments[0] );
					context = JAXBContext.newInstance( CollectionWrapper.class, valueClass );
				}
				else {
					if ( collection.isEmpty() ) {
						valueClass = Object.class;
						context = JAXBContext.newInstance( CollectionWrapper.class );
					}
					else {
						//noinspection unchecked
						valueClass = (Class<Object>) collection.iterator().next().getClass();
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
					if ( javaType instanceof BasicPluralJavaType<?> ) {
						valueTransformer = createTransformer(
								appender,
								( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
								"value",
								exampleValue,
								context.createJAXBIntrospector(),
								wrapperOptions
						);
					}
					else {
						valueTransformer = createTransformer(
								appender,
								valueClass,
								"value",
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
					if ( javaType instanceof BasicPluralJavaType<?> ) {
						transformer = createTransformer(
								appender,
								( (BasicPluralJavaType<?>) javaType ).getElementJavaType(),
								"value",
								exampleElement,
								context.createJAXBIntrospector(),
								wrapperOptions
						);
					}
					else {
						transformer = createTransformer(
								appender,
								valueClass,
								"value",
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
							"value"
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

	private JAXBElementTransformer createTransformer(
			StringBuilderSqlAppender appender,
			Class<?> elementClass,
			String tagName,
			Object exampleElement,
			JAXBIntrospector introspector,
			WrapperOptions wrapperOptions) {
		final JavaType<Object> elementJavaType = wrapperOptions.getSessionFactory()
				.getTypeConfiguration()
				.getJavaTypeRegistry()
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

	@XmlRootElement(name = "Map")
	public static class MapWrapper {
		@XmlAnyElement
		Collection<Object> elements;

		public MapWrapper() {
			this.elements = new ArrayList<>();
		}

		public MapWrapper(Collection<Object> elements) {
			this.elements = elements;
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

	private static interface JAXBElementTransformer {
		JAXBElement<?> toJAXBElement(Object o);
		Object fromJAXBElement(Object element, Unmarshaller unmarshaller) throws JAXBException;
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
		public Object fromJAXBElement(Object element, Unmarshaller unmarshaller) throws JAXBException {
			final Object valueElement = unmarshaller.unmarshal( (Node) element, elementClass ).getValue();
			final Object value;
			if ( valueElement instanceof Element ) {
				value = ( (Element) valueElement ).getFirstChild().getTextContent();
			}
			else {
				value = valueElement;
			}
			return value;
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
			final String value = unmarshaller.unmarshal( (Node) element, String.class ).getValue();
			return value == null ? null : elementJavaType.fromEncodedString( value, 0, value.length() );
		}
	}
}

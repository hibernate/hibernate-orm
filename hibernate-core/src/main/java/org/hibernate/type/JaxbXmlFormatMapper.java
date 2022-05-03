/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author Christian Beikov
 */
public class JaxbXmlFormatMapper implements FormatMapper {

	public static final String SHORT_NAME = "jaxb";
	public static final JaxbXmlFormatMapper INSTANCE = new JaxbXmlFormatMapper();

	public JaxbXmlFormatMapper() {
	}

	@Override
	public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
		try {
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
				final List<Object> elements = mapWrapper.elements;
				final Map<Object, Object> map = CollectionHelper.linkedMapOfSize( elements.size() >> 1 );
				for ( int i = 0; i < elements.size(); i += 2 ) {
					final Object keyElement = unmarshaller.unmarshal( (Node) elements.get( i ), keyClass ).getValue();
					final Object valueElement = unmarshaller.unmarshal( (Node) elements.get( i + 1 ), valueClass )
							.getValue();
					final Object key;
					final Object value;
					if ( keyElement instanceof Element ) {
						key = ( (Element) keyElement ).getFirstChild().getTextContent();
					}
					else {
						key = keyElement;
					}
					if ( valueElement instanceof Element ) {
						value = ( (Element) valueElement ).getFirstChild().getTextContent();
					}
					else {
						value = valueElement;
					}
					map.put( key, value );
				}
				//noinspection unchecked
				return (T) map;
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
				final List<Object> elements = collectionWrapper.elements;
				final Collection<Object> collection = new ArrayList<>( elements.size() >> 1 );
				for ( int i = 0; i < elements.size(); i++ ) {
					final Object valueElement = unmarshaller.unmarshal( (Node) elements.get( i ), valueClass )
							.getValue();
					final Object value;
					if ( valueElement instanceof Element ) {
						value = ( (Element) valueElement ).getFirstChild().getTextContent();
					}
					else {
						value = valueElement;
					}
					collection.add( value );
				}
				//noinspection unchecked
				return (T) collection;
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
		try {
			final StringWriter stringWriter = new StringWriter();
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
				for ( Map.Entry<?, ?> entry : map.entrySet() ) {
					mapWrapper.elements.add(
							new JAXBElement<>(
									new QName( "key" ),
									keyClass,
									entry.getKey()
							)
					);
					mapWrapper.elements.add(
							new JAXBElement<>(
									new QName( "value" ),
									valueClass,
									entry.getValue()
							)
					);
				}
				createMarshaller( context ).marshal( mapWrapper, stringWriter );
			}
			else if ( Collection.class.isAssignableFrom( javaType.getJavaTypeClass() ) ) {
				final JAXBContext context;
				final Class<Object> valueClass;
				final Collection<?> collection = (Collection<?>) value;
				final CollectionWrapper collectionWrapper = new CollectionWrapper( new ArrayList<>( collection ) );
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
//				for ( Object element : collection ) {
//					collectionWrapper.elements.add(
//							new JAXBElement<>(
//									new QName( "key" ),
//									keyClass,
//									entry.getKey()
//							)
//					);
//					collectionWrapper.elements.add(
//							new JAXBElement<>(
//									new QName( "value" ),
//									valueClass,
//									entry.getValue()
//							)
//					);
//				}
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

	private Marshaller createMarshaller(JAXBContext context) throws JAXBException {
		final Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty( Marshaller.JAXB_FRAGMENT, true );
		return marshaller;
	}

	@XmlRootElement(name = "Map")
	public static class MapWrapper {
		@XmlAnyElement
		List<Object> elements = new ArrayList<>();
	}

	@XmlRootElement(name = "Collection")
	public static class CollectionWrapper {
		@XmlAnyElement
		List<Object> elements = new ArrayList<>();

		public CollectionWrapper() {
		}

		public CollectionWrapper(List<Object> elements) {
			this.elements = elements;
		}
	}
}

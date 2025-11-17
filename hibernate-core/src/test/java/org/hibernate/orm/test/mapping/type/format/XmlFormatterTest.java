/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.type.format;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.format.jackson.JacksonXmlFormatMapper;
import org.hibernate.type.format.jaxb.JaxbXmlFormatMapper;
import org.hibernate.type.internal.ParameterizedTypeImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(standardModels = StandardDomainModel.LIBRARY)
@SessionFactory
public class XmlFormatterTest implements SessionFactoryScopeAware {

	private SessionFactoryScope scope;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	private static Stream<Arguments> formatMappers() {
		return Stream.of( new JaxbXmlFormatMapper( false ), new JacksonXmlFormatMapper( false ) )
				.map( Arguments::of );
	}

	@ParameterizedTest
	@MethodSource("formatMappers")
	public void testCollection(FormatMapper formatMapper) {
		assertCollection( List.of(), Integer.class, formatMapper );
		assertCollection( Arrays.asList( new Integer[]{ null } ), Integer.class, formatMapper );
		assertCollection( List.of( "Abc" ), String.class, formatMapper );
		assertCollection( List.of( 123 ), Integer.class, formatMapper );
	}

	@ParameterizedTest
	@MethodSource("formatMappers")
	public void testArray(FormatMapper formatMapper) {
		assertArray( new int[0], formatMapper );
		assertArray( new String[]{ null }, formatMapper );
		assertArray( new String[]{ "Abc" }, formatMapper );
		assertArray( new int[]{ 123 }, formatMapper );
		assertArray( new Integer[]{ 123 }, formatMapper );
	}

	@ParameterizedTest
	@MethodSource("formatMappers")
	public void testByteArray(FormatMapper formatMapper) {
		assertArray( new byte[0][0], formatMapper );
		assertArray( new byte[][]{ new byte[]{ 1 } }, formatMapper );
	}

	@ParameterizedTest
	@MethodSource("formatMappers")
	public void testMap(FormatMapper formatMapper) {
		assertMap( Map.of(), Integer.class, Integer.class, formatMapper );
		assertMap( new HashMap<>(){{ put(null, "Abc"); }}, Integer.class, String.class, formatMapper );
		assertMap( new HashMap<>(){{ put(123, null); }}, Integer.class, String.class, formatMapper );
		assertMap( Map.of( 123, "Abc" ), Integer.class, String.class, formatMapper );
	}

	private void assertCollection(List<Object> values, Type elementType, FormatMapper formatMapper) {
		assertXmlEquals( expectedCollectionString( values ), collectionToString( values, elementType, formatMapper ) );
	}

	private void assertArray(Object values, FormatMapper formatMapper) {
		assertXmlEquals( expectedArrayString( values ), arrayToString( values, formatMapper ) );
	}

	private void assertMap(Map<?, ?> values, Type keyType, Type elementType, FormatMapper formatMapper) {
		assertXmlEquals( expectedMapString( values ), mapToString( values, keyType, elementType, formatMapper ) );
	}

	private String expectedArrayString(Object values) {
		if ( values instanceof Object[] array ) {
			return expectedCollectionString( Arrays.asList( array ) );
		}
		else {
			final int length = Array.getLength( values );
			final ArrayList<Object> list = new ArrayList<>( length );
			for ( int i = 0; i < length; i++ ) {
				list.add( Array.get( values, i ) );
			}
			return expectedCollectionString( list );
		}
	}

	private String expectedCollectionString(Collection<?> values) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "<Collection" );
		if ( values.isEmpty() ) {
			sb.append( "/>" );
		}
		else {
			sb.append( ">" );
			for ( Object value : values ) {
				if ( value == null ) {
					sb.append( "<e xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>" );
				}
				else {
					sb.append( "<e>" );
					if ( value instanceof byte[] bytes ) {
						sb.append( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes ) );
					}
					else {
						sb.append( value );
					}
					sb.append( "</e>" );
				}
			}
			sb.append( "</Collection>" );
		}
		return sb.toString();
	}

	private String expectedMapString(Map<?, ?> values) {
		final StringBuilder sb = new StringBuilder();
		sb.append( "<Map" );
		if ( values.isEmpty() ) {
			sb.append( "/>" );
		}
		else {
			sb.append( ">" );
			for ( Map.Entry<?, ?> entry : values.entrySet() ) {
				final Object key = entry.getKey();
				final Object value = entry.getValue();
				sb.append( "<e>" );

				if ( key == null ) {
					sb.append( "<k xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>" );
				}
				else {
					sb.append( "<k>" );
					if ( key instanceof byte[] bytes ) {
						sb.append( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes ) );
					}
					else {
						sb.append( key );
					}
					sb.append( "</k>" );
				}
				if ( value == null ) {
					sb.append( "<v xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>" );
				}
				else {
					sb.append( "<v>" );
					if ( value instanceof byte[] bytes ) {
						sb.append( PrimitiveByteArrayJavaType.INSTANCE.toString( bytes ) );
					}
					else {
						sb.append( value );
					}
					sb.append( "</v>" );
				}
				sb.append( "</e>" );
			}
			sb.append( "</Map>" );
		}
		return sb.toString();
	}

	private String collectionToString(Collection<?> value, Type elementType, FormatMapper formatMapper) {
		final JavaType<Object> javaType = scope.getSessionFactory().getTypeConfiguration().getJavaTypeRegistry()
				.getDescriptor( new ParameterizedTypeImpl( Collection.class, new Type[] {elementType}, null ) );
		final WrapperOptions wrapperOptions = scope.getSessionFactory().getWrapperOptions();
		final String actualValue = formatMapper.toString(
				value,
				javaType,
				wrapperOptions
		);
		assertXmlEquals(
				actualValue,
				formatMapper.toString(
						formatMapper.fromString( actualValue, javaType, wrapperOptions ),
						javaType,
						wrapperOptions
				)
		);
		return actualValue;
	}

	private String arrayToString(Object value, FormatMapper formatMapper) {
		final JavaType<Object> javaType =
				scope.getSessionFactory().getTypeConfiguration().getJavaTypeRegistry()
						.getDescriptor( value.getClass() );
		final WrapperOptions wrapperOptions = scope.getSessionFactory().getWrapperOptions();
		final String actualValue = formatMapper.toString(
				value,
				javaType,
				wrapperOptions
		);
		assertXmlEquals(
				actualValue,
				formatMapper.toString(
						formatMapper.fromString( actualValue, javaType, wrapperOptions ),
						javaType,
						wrapperOptions
				)
		);
		return actualValue;
	}

	private String mapToString(Map<?, ?> value, Type keyType, Type elementType, FormatMapper formatMapper) {
		final JavaType<Object> javaType = scope.getSessionFactory().getTypeConfiguration().getJavaTypeRegistry()
				.getDescriptor( new ParameterizedTypeImpl( Map.class, new Type[] {keyType, elementType}, null ) );
		final WrapperOptions wrapperOptions = scope.getSessionFactory().getWrapperOptions();
		final String actualValue = formatMapper.toString(
				value,
				javaType,
				wrapperOptions
		);
		assertXmlEquals(
				actualValue,
				formatMapper.toString(
						formatMapper.fromString( actualValue, javaType, wrapperOptions ),
						javaType,
						wrapperOptions
				)
		);
		return actualValue;
	}

	private void assertXmlEquals(String expected, String actual) {
		final Document expectedDoc = parseXml( xmlNormalize( expected ) );
		final Document actualDoc = parseXml( xmlNormalize( actual ) );
		normalize( expectedDoc );
		normalize( actualDoc );
		assertEquals( toXml( expectedDoc ).trim(), toXml( actualDoc ).trim() );
	}

	private void normalize(Document document) {
		normalize( document.getChildNodes() );
	}

	private void normalize(NodeList childNodes) {
		for ( int i = 0; i < childNodes.getLength(); i++ ) {
			final Node childNode = childNodes.item( i );
			if ( childNode.getNodeType() == Node.ELEMENT_NODE ) {
				normalize( childNode.getChildNodes() );
			}
			else if ( childNode.getNodeType() == Node.TEXT_NODE ) {
				if ( childNode.getNodeValue().isBlank() ) {
					childNode.getParentNode().removeChild( childNode );
				}
				else {
					childNode.setNodeValue( childNode.getNodeValue().trim() );
				}
			}
			else if ( childNode.getNodeType() == Node.COMMENT_NODE ) {
				childNode.setNodeValue( childNode.getNodeValue().trim() );
			}
		}
	}

	private String xmlNormalize(String doc) {
		final String prefix = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
		return doc.startsWith( "<?xml" ) ? doc : prefix + doc;
	}

	private static Document parseXml(String document) {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			final DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse( new InputSource( new StringReader( document ) ) );
		}
		catch (ParserConfigurationException | IOException | SAXException e) {
			throw new RuntimeException( e );
		}
	}

	private static String toXml(Document document) {
		final TransformerFactory tf = TransformerFactory.newInstance();
		try {
			final Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
			final StringWriter writer = new StringWriter();
			transformer.transform( new DOMSource( document ), new StreamResult( writer ) );
			return writer.toString();
		}
		catch (TransformerException e) {
			throw new RuntimeException( e );
		}
	}
}

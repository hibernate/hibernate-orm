/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.type.SqlTypes;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel( annotatedClasses = {
		XmlFunctionTests.XmlHolder.class,
		EntityOfBasics.class
})
@ServiceRegistry(settings = @Setting(name = QuerySettings.XML_FUNCTIONS_ENABLED, value = "true"))
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18497")
public class XmlFunctionTests {

	XmlHolder entity;

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					entity = new XmlHolder();
					entity.id = 1L;
					entity.xml = new HashMap<>();
					entity.xml.put( "theInt", 1 );
					entity.xml.put( "theFloat", 0.1 );
					entity.xml.put( "theString", "abc" );
					entity.xml.put( "theBoolean", true );
					entity.xml.put( "theNull", null );
					entity.xml.put( "theArray", new String[] { "a", "b", "c" } );
					entity.xml.put( "theObject", new HashMap<>( entity.xml ) );
					entity.xml.put(
							"theNestedObjects",
							List.of(
									Map.of( "id", 1, "name", "val1" ),
									Map.of( "id", 2, "name", "val2" ),
									Map.of( "id", 3, "name", "val3" )
							)
					);
					em.persist(entity);

					EntityOfBasics e1 = new EntityOfBasics();
					e1.setId( 1 );
					e1.setTheString( "Dog" );
					e1.setTheInteger( 0 );
					e1.setTheUuid( UUID.randomUUID() );
					EntityOfBasics e2 = new EntityOfBasics();
					e2.setId( 2 );
					e2.setTheString( "Cat" );
					e2.setTheInteger( 0 );

					em.persist( e1 );
					em.persist( e2 );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlelement.class)
	public void testXmlelement(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
									"select " +
											"xmlelement(name empty), " +
											"xmlelement(name `the-element`), " +
											"xmlelement(name myElement, 'myContent'), " +
											"xmlelement(name myElement, xmlattributes('123' as attr1)), " +
											"xmlelement(name myElement, xmlattributes('123' as attr1, '456' as `attr-2`)), " +
											"xmlelement(name myElement, xmlattributes('123' as attr1), 'myContent', xmlelement(name empty))",
									Tuple.class
							).getSingleResult();
					assertXmlEquals( "<empty/>", tuple.get( 0, String.class ) );
					assertXmlEquals( "<the-element/>", tuple.get( 1 , String.class ) );
					assertXmlEquals( "<myElement>myContent</myElement>", tuple.get( 2, String.class ) );
					assertXmlEquals( "<myElement attr1=\"123\"/>", tuple.get( 3, String.class ) );
					assertXmlEquals( "<myElement attr1=\"123\" attr-2=\"456\"/>", tuple.get( 4, String.class ) );
					assertXmlEquals( "<myElement attr1=\"123\">myContent<empty/></myElement>", tuple.get( 5, String.class ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlcomment.class)
	public void testXmlcomment(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select " +
									"xmlcomment('Abc'), " +
									"xmlcomment('<>')",
							Tuple.class
					).getSingleResult();
					assertXmlEquals( "<!--Abc--><a/>", tuple.get( 0, String.class ) + "<a/>" );
					assertXmlEquals( "<!--<>--><a/>", tuple.get( 1 , String.class ) + "<a/>" );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlforest.class)
	public void testXmlforest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select xmlforest(123 as e1, 'text' as e2)," +
									"xmlforest(e.id, e.theString) " +
									"from EntityOfBasics e where e.id = 1",
							Tuple.class
					).getSingleResult();
					assertXmlEquals( "<r><e1>123</e1><e2>text</e2></r>", "<r>" + tuple.get( 0, String.class ) + "</r>" );
					assertXmlEquals( "<r><id>1</id><theString>Dog</theString></r>", "<r>" + tuple.get( 1, String.class ) + "</r>" );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlconcat.class)
	public void testXmlconcat(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select xmlconcat(xmlelement(name e1, 123), xmlelement(name e2, 'text'))," +
									"xmlconcat(xmlelement(name id, e.id), xmlelement(name theString, e.theString)) " +
									"from EntityOfBasics e where e.id = 1",
							Tuple.class
					).getSingleResult();
					assertXmlEquals( "<r><e1>123</e1><e2>text</e2></r>", "<r>" + tuple.get( 0, String.class ) + "</r>" );
					assertXmlEquals( "<r><id>1</id><theString>Dog</theString></r>", "<r>" + tuple.get( 1, String.class ) + "</r>" );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlpi.class)
	public void testXmlpi(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select xmlpi(name test, 'abc')",
							Tuple.class
					).getSingleResult();
					assertEquals( "<?test abc?>", tuple.get( 0, String.class ).trim() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlquery.class)
	public void testXmlquery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select xmlquery('/a/val' passing '<a><val>asd</val></a>')",
							Tuple.class
					).getSingleResult();
					assertXmlEquals( "<val>asd</val>", tuple.get( 0, String.class ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlexists.class)
	public void testXmlexists(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select xmlexists('/a/val' passing '<a><val>asd</val></a>')",
							Tuple.class
					).getSingleResult();
					assertTrue( tuple.get( 0, Boolean.class ) );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlagg.class)
	public void testXmlagg(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Tuple tuple = session.createQuery(
							"select xmlagg(xmlelement(name a, e.theString) order by e.id) " +
									"from from EntityOfBasics e",
							Tuple.class
					).getSingleResult();
					assertXmlEquals( "<r><a>Dog</a><a>Cat</a></r>", "<r>" + tuple.get( 0, String.class ) + "</r>" );
				}
		);
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
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
			final StringWriter writer = new StringWriter();
			transformer.transform( new DOMSource( document ), new StreamResult( writer ) );
			return writer.toString();
		}
		catch (TransformerException e) {
			throw new RuntimeException( e );
		}
	}

	@Entity(name = "XmlHolder")
	public static class XmlHolder {
		@Id
		Long id;
		@JdbcTypeCode(SqlTypes.SQLXML)
		Map<String, Object> xml;
	}
}

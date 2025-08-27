/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.function.xml;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.cfg.QuerySettings;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.JpaFunctionRoot;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.tree.expression.SqmXmlTableFunction;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Christian Beikov
 */
@DomainModel(annotatedClasses = XmlTableTest.XmlHolder.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = QuerySettings.XML_FUNCTIONS_ENABLED, value = "true"))
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsXmlTable.class)
public class XmlTableTest {

	private static final String XML = """
			<root>
			<elem>
			<theInt>1</theInt>
			<theFloat>0.1</theFloat>
			<theString>abc</theString>
			<theBoolean>true</theBoolean>
			<theNull/>
			<theObject>
			<nested>Abc</nested>
			</theObject>
			</elem>
			</root>
			""";

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				em -> {
					XmlHolder entity = new XmlHolder();
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
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSimple(SessionFactoryScope scope) {
		scope.inSession( em -> {
			//tag::hql-xml-table-example[]
			final String query = """
					select
					t.theInt,
					t.theFloat,
					t.theString,
					t.theBoolean,
					t.theNull,
					t.theObject,
					t.theNestedString,
					t.nonExisting,
					t.nonExistingWithDefault
					from xmltable('/root/elem' passing :xml columns
					theInt Integer,
					theFloat Float,
					theString String,
					theBoolean Boolean,
					theNull String,
					theObject XML,
					theNestedString String path 'theObject/nested',
					nonExisting String,
					nonExistingWithDefault String default 'none'
					) t
					"""
					//end::hql-xml-table-example[]
					.replace( ":xml", "'" + XML + "'" );
			//tag::hql-xml-table-example[]
			List<Tuple> resultList = em.createQuery( query, Tuple.class )
					.getResultList();
			//end::hql-xml-table-example[]

			assertEquals( 1, resultList.size() );

			assertTupleEquals( resultList.get( 0 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase ASE needs a special emulation for query columns that is impossible with parameters")
	public void testNodeBuilderXmlTableObject(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final NodeBuilder cb = (NodeBuilder) em.getCriteriaBuilder();
			final SqmSelectStatement<Tuple> cq = cb.createTupleQuery();
			final SqmXmlTableFunction<?> xmlTable = cb.xmlTable( "/root/elem", cb.value( XML ) );

			xmlTable.valueColumn( "theInt", Integer.class );
			xmlTable.valueColumn( "theFloat", Float.class );
			xmlTable.valueColumn( "theString", String.class );
			xmlTable.valueColumn( "theBoolean", Boolean.class );
			xmlTable.valueColumn( "theNull", String.class );
			xmlTable.queryColumn( "theObject" );
			xmlTable.valueColumn( "theNestedString", String.class, "theObject/nested" );
			xmlTable.valueColumn( "nonExisting", String.class );
			xmlTable.valueColumn( "nonExistingWithDefault", String.class ).defaultValue( "none" );

			final JpaFunctionRoot<?> root = cq.from( xmlTable );
			cq.multiselect(
					root.get( "theInt" ),
					root.get( "theFloat" ),
					root.get( "theString" ),
					root.get( "theBoolean" ),
					root.get( "theNull" ),
					root.get( "theObject" ),
					root.get( "theNestedString" ),
					root.get( "nonExisting" ),
					root.get( "nonExistingWithDefault" )
			);
			List<Tuple> resultList = em.createQuery( cq ).getResultList();

			assertEquals( 1, resultList.size() );

			assertTupleEquals( resultList.get( 0 ) );
		} );
	}

	@Test
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "Sybase doesn't support such xpath expressions directly in xmltable. We could emulate that through generating xmlextract calls though")
	public void testCorrelateXmlTable(SessionFactoryScope scope) {
		scope.inSession( em -> {
			final String query = """
					select
					t.theInt,
					t.theFloat,
					t.theString,
					t.theBoolean
					from XmlHolder e join lateral xmltable('/Map' passing e.xml columns
					theInt Integer path 'e[k/text()="theInt"]/v',
					theFloat Float path 'e[k/text()="theFloat"]/v',
					theString String path 'e[k/text()="theString"]/v',
					theBoolean Boolean path 'e[k/text()="theBoolean"]/v'
					) t
					""";
			List<Tuple> resultList = em.createQuery( query, Tuple.class ).getResultList();

			assertEquals( 1, resultList.size() );

			Tuple tuple = resultList.get( 0 );
			assertEquals( 1, tuple.get( 0 ) );
			assertEquals( 0.1F, tuple.get( 1 ) );
			assertEquals( "abc", tuple.get( 2 ) );
			assertEquals( true, tuple.get( 3 ) );
		} );
	}

	private void assertTupleEquals(Tuple tuple) {
		assertEquals( 1, tuple.get( 0 ) );
		assertEquals( 0.1F, tuple.get( 1 ) );
		assertEquals( "abc", tuple.get( 2 ) );
		assertEquals( true, tuple.get( 3 ) );
		if ( DialectContext.getDialect() instanceof OracleDialect
				|| DialectContext.getDialect() instanceof HANADialect
				|| DialectContext.getDialect() instanceof SybaseASEDialect ) {
			// Some databases return null for empty tags rather than an empty string
			assertNull( tuple.get( 4 ) );
		}
		else {
			// Other DBs returns an empty string for an empty tag
			assertEquals( "", tuple.get( 4 ) );
		}

		assertXmlEquals("<theObject><nested>Abc</nested></theObject>", tuple.get( 5, String.class ) );

		assertEquals( "Abc", tuple.get( 6 ) );
		assertNull( tuple.get( 7 ) );
		assertEquals( "none", tuple.get( 8 ) );
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

	@Entity(name = "XmlHolder")
	public static class XmlHolder {
		@Id
		Long id;
		@JdbcTypeCode(SqlTypes.SQLXML)
		Map<String, Object> xml;
	}

}

package org.hibernate.orm.test.boot.jaxb.mapping;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import org.hibernate.boot.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.internal.stax.MappingEventReader;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.xsd.MappingXsdSupport;

import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.xml.bind.JAXBContext;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/// Compatibility of historical mapping namespaces with the canonical HTTPS JAXB model.
///
/// @author Steve Ebersole
public class MappingNamespaceTests {
	private static final String HTTP_NAMESPACE = "http://www.hibernate.org/xsd/orm/mapping";
	private static final String HTTPS_NAMESPACE = "https://www.hibernate.org/xsd/orm/mapping";
	private static final String FOREIGN_NAMESPACE = "urn:foreign";

	@ParameterizedTest
	@CsvSource({
			"http://www.hibernate.org/xsd/orm/mapping, 3.1",
			"http://www.hibernate.org/xsd/orm/mapping, 7.0",
			"http://www.hibernate.org/xsd/orm/mapping, 8.0",
			"https://www.hibernate.org/xsd/orm/mapping, 8.0",
			"http://java.sun.com/xml/ns/persistence/orm, 1.0",
			"http://java.sun.com/xml/ns/persistence/orm, 2.0",
			"http://xmlns.jcp.org/xml/ns/persistence/orm, 2.1",
			"http://xmlns.jcp.org/xml/ns/persistence/orm, 2.2",
			"https://jakarta.ee/xml/ns/persistence/orm, 3.0",
			"https://jakarta.ee/xml/ns/persistence/orm, 3.1",
			"https://jakarta.ee/xml/ns/persistence/orm, 3.2",
			"https://jakarta.ee/xml/ns/persistence/orm, 4.0",
			"'', 8.0"
	})
	void bindHistoricalNamespaces(String namespace, String version) throws Exception {
		final var context = JAXBContext.newInstance( JaxbEntityMappingsImpl.class );
		for ( boolean prefixed : new boolean[] { false, true } ) {
			final String xml = mappingXml( namespace, version, prefixed );
			for ( boolean validate : new boolean[] { false, true } ) {
				final var binder = binder( validate );
				assertThat( binder.isValidationEnabled() ).isEqualTo( validate );
				final Binding<JaxbEntityMappingsImpl> binding = binder.bind(
						new StreamSource( new StringReader( xml ) ), origin()
				);
				verifyMapping( binding.getRoot() );

				final var reader = mappingReader( xml );
				try {
					final var unmarshaller = context.createUnmarshaller();
					if ( validate ) {
						unmarshaller.setSchema( MappingXsdSupport.latestDescriptor().getSchema() );
					}
					verifyMapping( (JaxbEntityMappingsImpl) unmarshaller.unmarshal( reader ) );
				}
				finally {
					reader.close();
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void preserveForeignNamespaces(boolean prefixed) throws Exception {
		final String prefix = prefixed ? "orm:" : "";
		final String declaration = prefixed ? "xmlns:orm" : "xmlns";
		final var reader = mappingReader( """
				<%sentity-mappings %s="%s" xmlns:xsi="%s" xmlns:ext="%s"
						version="7.0" ext:version="99.0" xsi:schemaLocation="%s mapping-7.0.xsd">
					<%sentity class="Example"/>
					<ext:entity-mappings version="99.0"><ext:value/></ext:entity-mappings>
				</%sentity-mappings>
				""".formatted( prefix, declaration, HTTP_NAMESPACE, W3C_XML_SCHEMA_INSTANCE_NS_URI,
				FOREIGN_NAMESPACE, HTTP_NAMESPACE, prefix, prefix ) );
		try {
			while ( reader.hasNext() ) {
				final var event = reader.nextEvent();
				if ( event.isStartElement() ) {
					final var element = event.asStartElement();
					if ( element.getName().getPrefix().equals( "ext" ) ) {
						assertThat( element.getName().getNamespaceURI() ).isEqualTo( FOREIGN_NAMESPACE );
						if ( element.getName().getLocalPart().equals( "entity-mappings" ) ) {
							assertThat( element.getAttributeByName( new QName( "version" ) ).getValue() )
									.isEqualTo( "99.0" );
						}
					}
					else {
						assertThat( element.getName().getNamespaceURI() ).isEqualTo( HTTPS_NAMESPACE );
						assertThat( element.getName().getPrefix() ).isEqualTo( prefixed ? "orm" : "" );
						if ( element.getName().getLocalPart().equals( "entity-mappings" ) ) {
							assertThat( element.getNamespaceURI( "xsi" ) ).isEqualTo( W3C_XML_SCHEMA_INSTANCE_NS_URI );
							assertThat( element.getNamespaceURI( "ext" ) ).isEqualTo( FOREIGN_NAMESPACE );
							assertThat( element.getAttributeByName( new QName( "version" ) ).getValue() )
									.isEqualTo( "8.0" );
							assertThat( element.getAttributeByName( new QName( FOREIGN_NAMESPACE, "version" ) ).getValue() )
									.isEqualTo( "99.0" );
							assertThat( element.getAttributeByName( new QName( W3C_XML_SCHEMA_INSTANCE_NS_URI, "schemaLocation" ) )
									.getValue() ).isEqualTo( HTTP_NAMESPACE + " mapping-7.0.xsd" );
						}
					}
				}
				else if ( event.isEndElement() ) {
					final var name = event.asEndElement().getName();
					assertThat( name.getNamespaceURI() )
							.isEqualTo( name.getPrefix().equals( "ext" ) ? FOREIGN_NAMESPACE : HTTPS_NAMESPACE );
				}
			}
		}
		finally {
			reader.close();
		}
	}

	@Test
	void rejectUnknownRootNamespace() {
		final String xml = mappingXml( FOREIGN_NAMESPACE, "8.0", false );
		assertThatExceptionOfType( MappingException.class )
				.isThrownBy( () -> binder( true ).bind( new StreamSource( new StringReader( xml ) ), origin() ) );
	}

	@Test
	void marshalCanonicalNamespace() throws Exception {
		final var binder = binder( true );
		final Binding<JaxbEntityMappingsImpl> binding = binder.bind(
				new StreamSource( new StringReader( mappingXml( HTTP_NAMESPACE, "7.0", true ) ) ), origin()
		);
		final var writer = new StringWriter();
		binder.mappingJaxbContext().createMarshaller().marshal( binding.getRoot(), writer );
		final String xml = writer.toString();
		assertThat( xml ).contains( HTTPS_NAMESPACE ).doesNotContain( HTTP_NAMESPACE );
		MappingXsdSupport.latestDescriptor().getSchema().newValidator()
				.validate( new StreamSource( new StringReader( xml ) ) );
		final Binding<JaxbEntityMappingsImpl> roundTrip = binder.bind(
				new StreamSource( new StringReader( xml ) ), origin()
		);
		verifyMapping( roundTrip.getRoot() );
	}

	private static MappingBinder binder(boolean validate) {
		return new MappingBinder( ClassLoaderServiceTestingImpl.INSTANCE, (MappingBinder.Options) () -> validate );
	}

	private static Origin origin() {
		return new Origin( SourceType.OTHER, "mapping-namespace-test" );
	}

	private static XMLEventReader mappingReader(String xml) throws XMLStreamException {
		return new MappingEventReader(
				XMLInputFactory.newInstance().createXMLEventReader( new StringReader( xml ) ),
				XMLEventFactory.newInstance()
		);
	}

	private static String mappingXml(String namespace, String version, boolean prefixed) {
		final String prefix = prefixed && !namespace.isEmpty() ? "orm:" : "";
		final String declaration = namespace.isEmpty() ? ""
				: (prefixed ? "xmlns:orm" : "xmlns") + "=\"" + namespace + "\"";
		final String schemaLocation = namespace.isEmpty() ? ""
				: "xsi:schemaLocation=\"" + namespace + " https://www.hibernate.org/xsd/orm/mapping/mapping-"
						+ version + ".xsd\"";
		return """
				<%1$sentity-mappings %2$s xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" %3$s version="%4$s">
					<%1$spackage>org.example</%1$spackage>
					<%1$sentity class="Example" name="ExampleEntity" access="FIELD">
						<%1$sattributes>
							<%1$sid name="id"/>
							<%1$sbasic name="name"><%1$scolumn name="display_name"/></%1$sbasic>
						</%1$sattributes>
					</%1$sentity>
				</%1$sentity-mappings>
				""".formatted( prefix, declaration, schemaLocation, version );
	}

	private static void verifyMapping(JaxbEntityMappingsImpl mapping) {
		assertThat( mapping.getVersion() ).isEqualTo( "8.0" );
		assertThat( mapping.getPackage() ).isEqualTo( "org.example" );
		assertThat( mapping.getEntities() ).hasSize( 1 );
		final var entity = mapping.getEntities().get( 0 );
		assertThat( entity.getClazz() ).isEqualTo( "Example" );
		assertThat( entity.getName() ).isEqualTo( "ExampleEntity" );
		assertThat( entity.getAttributes().getIdAttributes() ).hasSize( 1 );
		assertThat( entity.getAttributes().getIdAttributes().get( 0 ).getName() ).isEqualTo( "id" );
		assertThat( entity.getAttributes().getBasicAttributes() ).hasSize( 1 );
		final var basic = entity.getAttributes().getBasicAttributes().get( 0 );
		assertThat( basic.getName() ).isEqualTo( "name" );
		assertThat( basic.getColumn().getName() ).isEqualTo( "display_name" );
	}
}

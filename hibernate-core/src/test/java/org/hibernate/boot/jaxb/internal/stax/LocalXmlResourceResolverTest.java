package org.hibernate.boot.jaxb.internal.stax;

import static org.assertj.core.api.Assertions.assertThat;

import javax.xml.stream.XMLStreamException;

import org.hibernate.testing.boot.ClassLoaderServiceTestingImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.assertj.core.api.InstanceOfAssertFactories;

/**
 * Test the resolution of known XML schemas/DTDs to local resources.
 * <p>
 * Note that when it comes to XML schemas,
 * LocalXmlResourceResolver doesn't seem to be actually invoked;
 * which makes sense since we set the XML schema ourselves when configuring the parser.
 * So this test is probably only relevant for DTDs, but we keep tests about XML schemas too just in case.
 */
public class LocalXmlResourceResolverTest {

	private final LocalXmlResourceResolver resolver;

	public LocalXmlResourceResolverTest() {
		this.resolver = new LocalXmlResourceResolver( ClassLoaderServiceTestingImpl.INSTANCE );
	}

	@ParameterizedTest
	@CsvSource({
			"http://www.hibernate.org/xsd/orm/mapping,org/hibernate/xsd/mapping/mapping-7.0.xsd",
			"https://www.hibernate.org/xsd/orm/mapping,org/hibernate/xsd/mapping/mapping-9.0.xsd",
			// JPA 1.0 and 2.0 share the same namespace URI
			// NOTE: Behavior differs from Hibernate ORM 5, which resolves to org/hibernate/jpa/orm_2_0.xsd
			"http://java.sun.com/xml/ns/persistence/orm,org/hibernate/jpa/orm_1_0.xsd",
			// JPA 2.1 and 2.2 share the same namespace URI
			"http://xmlns.jcp.org/xml/ns/persistence/orm,org/hibernate/jpa/orm_2_1.xsd",
			"https://jakarta.ee/xml/ns/persistence/orm,org/hibernate/jpa/orm_3_0.xsd",

			// NOTE: Hibernate ORM 5 doesn't resolve persistence.xml XSDs to local resources,
			//       but Hibernate ORM 6+ does.
			// JPA 1.0 and 2.0 share the same namespace URI
			"http://java.sun.com/xml/ns/persistence,org/hibernate/jpa/persistence_1_0.xsd",
			// JPA 2.1 and 2.2 share the same namespace URI
			"http://xmlns.jcp.org/xml/ns/persistence,org/hibernate/jpa/persistence_2_1.xsd",
			"https://jakarta.ee/xml/ns/persistence,org/hibernate/jpa/persistence_3_0.xsd",
	})
	void resolve_namespace_localResource(String namespace, String expectedLocalResource) throws XMLStreamException {
		assertThat( resolver.resolveEntity( null, null, null, namespace ) )
				.asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
				.hasSameContentAs( getClass().getClassLoader().getResourceAsStream( expectedLocalResource ) );
	}

	@ParameterizedTest
	@CsvSource({
			"http://www.hibernate.org/xsd/orm/mapping/mapping-3.1.0.xsd,org/hibernate/xsd/mapping/mapping-3.1.0.xsd",
			"https://www.hibernate.org/xsd/orm/mapping/mapping-3.1.0.xsd,org/hibernate/xsd/mapping/mapping-3.1.0.xsd",
			"http://www.hibernate.org/xsd/orm/mapping/mapping-7.0.xsd,org/hibernate/xsd/mapping/mapping-7.0.xsd",
			"https://www.hibernate.org/xsd/orm/mapping/mapping-7.0.xsd,org/hibernate/xsd/mapping/mapping-7.0.xsd",
			"http://www.hibernate.org/xsd/orm/mapping/mapping-8.0.xsd,org/hibernate/xsd/mapping/mapping-8.0.xsd",
			"https://www.hibernate.org/xsd/orm/mapping/mapping-8.0.xsd,org/hibernate/xsd/mapping/mapping-8.0.xsd"
	})
	void resolve_mappingSchema_localResource(String systemId, String expectedLocalResource) throws XMLStreamException {
		// The schema location identifies the version even when historical versions share a namespace.
		assertThat( resolver.resolveEntity( null, systemId, null, "http://www.hibernate.org/xsd/orm/mapping" ) )
				.asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
				.hasSameContentAs( getClass().getClassLoader().getResourceAsStream( expectedLocalResource ) );
		assertThat( resolver.resolveEntity( null, systemId, null, null ) )
				.asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
				.hasSameContentAs( getClass().getClassLoader().getResourceAsStream( expectedLocalResource ) );
	}

}

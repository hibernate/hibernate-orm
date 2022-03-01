/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
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
			// JPA 1.0 and 2.0 share the same namespace URI
			// NOTE: Behavior differs from Hibernate ORM 6, which resolves to org/hibernate/jpa/orm_1_0.xsd
			"http://java.sun.com/xml/ns/persistence/orm,org/hibernate/jpa/orm_2_0.xsd",
			// JPA 2.1 and 2.2 share the same namespace URI
			"http://xmlns.jcp.org/xml/ns/persistence/orm,org/hibernate/jpa/orm_2_1.xsd",
			"https://jakarta.ee/xml/ns/persistence/orm,org/hibernate/jpa/orm_3_0.xsd",

			// NOTE: Hibernate ORM 5 doesn't resolve persistence.xml XSDs to local resources
			//       so we don't test them here.

			"http://www.hibernate.org/xsd/orm/hbm,org/hibernate/xsd/mapping/legacy-mapping-4.0.xsd",
			"http://www.hibernate.org/xsd/hibernate-mapping,org/hibernate/hibernate-mapping-4.0.xsd",
			"http://www.hibernate.org/xsd/orm/cfg,org/hibernate/xsd/cfg/legacy-configuration-4.0.xsd",
	})
	void resolve_namespace_localResource(String namespace, String expectedLocalResource) throws XMLStreamException {
		assertThat( resolver.resolveEntity( null, null, null, namespace ) )
				.asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
				.hasSameContentAs( getClass().getClassLoader().getResourceAsStream( expectedLocalResource ) );
	}

	@ParameterizedTest
	@CsvSource({
			"http://www.hibernate.org/dtd/hibernate-mapping,org/hibernate/hibernate-mapping-3.0.dtd",
			"https://www.hibernate.org/dtd/hibernate-mapping,org/hibernate/hibernate-mapping-3.0.dtd",

			"http://hibernate.org/dtd/hibernate-mapping,org/hibernate/hibernate-mapping-3.0.dtd",
			"https://hibernate.org/dtd/hibernate-mapping,org/hibernate/hibernate-mapping-3.0.dtd",

			"http://hibernate.sourceforge.net/hibernate-mapping,org/hibernate/hibernate-mapping-3.0.dtd",
			"https://hibernate.sourceforge.net/hibernate-mapping,org/hibernate/hibernate-mapping-3.0.dtd",

			"http://www.hibernate.org/dtd/hibernate-configuration,org/hibernate/hibernate-configuration-3.0.dtd",
			"https://www.hibernate.org/dtd/hibernate-configuration,org/hibernate/hibernate-configuration-3.0.dtd",

			"http://hibernate.org/dtd/hibernate-configuration,org/hibernate/hibernate-configuration-3.0.dtd",
			"https://hibernate.org/dtd/hibernate-configuration,org/hibernate/hibernate-configuration-3.0.dtd",

			"http://hibernate.sourceforge.net/hibernate-configuration,org/hibernate/hibernate-configuration-3.0.dtd",
			"https://hibernate.sourceforge.net/hibernate-configuration,org/hibernate/hibernate-configuration-3.0.dtd"
	})
	void resolve_dtd_localResource(String id, String expectedLocalResource) throws XMLStreamException {
		// publicId
		assertThat( resolver.resolveEntity( id, null, null, null ) )
				.asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
				.hasSameContentAs( getClass().getClassLoader().getResourceAsStream( expectedLocalResource ) );

		// systemId
		assertThat( resolver.resolveEntity( null, id, null, null ) )
				.asInstanceOf( InstanceOfAssertFactories.INPUT_STREAM )
				.hasSameContentAs( getClass().getClassLoader().getResourceAsStream( expectedLocalResource ) );
	}

}
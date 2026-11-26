/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.cfgxml.internal;


import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import static javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD;
import static javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Applies the XML security configuration shared by every StAX {@link XMLInputFactory}
 * Hibernate uses to read {@code cfg.xml}, {@code orm.xml}, {@code hbm.xml} and
 * {@code persistence.xml} documents, both at runtime and in the metamodel generator.
 *
 * @see org.hibernate.cfg.MappingSettings#XML_ALLOW_DTD_AND_EXTERNAL_ENTITIES
 */
public class XmlFactoryUtil {

	/**
	 * Creates and configures a StAX {@link XMLInputFactory} with the given DTD and external entity settings.
	 *
	 * @param allowDtdAndExternalEntities whether to set {@link XMLInputFactory#SUPPORT_DTD} and
	 * {@link XMLInputFactory#IS_SUPPORTING_EXTERNAL_ENTITIES} to {@code true}
	 * @return a configured {@link XMLInputFactory} instance
	 */
	public static XMLInputFactory newXmlInputFactory(boolean allowDtdAndExternalEntities) {
		final var xmlFactory = XMLInputFactory.newInstance();
		xmlFactory.setProperty( XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, allowDtdAndExternalEntities );
		xmlFactory.setProperty( XMLInputFactory.SUPPORT_DTD, allowDtdAndExternalEntities );
		return xmlFactory;
	}

	/**
	 * Creates a W3C XML Schema {@link SchemaFactory} which does not access external DTDs
	 * or external schemas referenced via {@code xs:import}, {@code xs:include} or {@code xs:redefine}.
	 * <p>
	 * The schemas Hibernate loads are bundled and self-contained, so they never need such access.
	 */
	public static SchemaFactory newSchemaFactory() {
		final var schemaFactory = SchemaFactory.newInstance( W3C_XML_SCHEMA_NS_URI );
		try {
			schemaFactory.setProperty( ACCESS_EXTERNAL_DTD, "" );
			schemaFactory.setProperty( ACCESS_EXTERNAL_SCHEMA, "" );
		}
		catch (SAXNotRecognizedException | SAXNotSupportedException ignore) {
			// the SchemaFactory implementation does not support JAXP 1.5 access restrictions
		}
		return schemaFactory;
	}
}

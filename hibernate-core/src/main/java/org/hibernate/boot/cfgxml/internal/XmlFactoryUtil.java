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
	 * Enables or disables DTD processing and external entity resolution on the given factory.
	 * <p>
	 *
	 * @param xmlFactory the factory to configure
	 * @param allowDtdAndExternalEntities whether to set {@link XMLInputFactory#SUPPORT_DTD} and
	 * {@link XMLInputFactory#IS_SUPPORTING_EXTERNAL_ENTITIES} to {@code true}
	 */
	public static void configureXmlFactory(XMLInputFactory xmlFactory, boolean allowDtdAndExternalEntities) {
		xmlFactory.setProperty( XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, allowDtdAndExternalEntities );
		xmlFactory.setProperty( XMLInputFactory.SUPPORT_DTD, allowDtdAndExternalEntities );
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

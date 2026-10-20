package org.hibernate.boot.cfgxml.internal;


import javax.xml.stream.XMLInputFactory;

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
}

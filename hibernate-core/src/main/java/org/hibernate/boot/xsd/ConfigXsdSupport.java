/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.xsd;

import org.hibernate.Internal;

/**
 * Support for XSD handling related to Hibernate's `cfg.xml` and
 * JPA's `persistence.xml`.
 * The implementation attempts to not load XsdDescriptor instances which are not
 * necessary and favours memory efficiency over CPU efficiency, as this is expected
 * to be used only during bootstrap.
 *
 * @author Steve Ebersole
 * @author Sanne Grinovero
 */
@Internal
@SuppressWarnings("unused")
public class ConfigXsdSupport {

	/**
	 * Needs synchronization on any access.
	 * Custom keys:
	 * 0: cfgXml
	 * 1: configurationXML
	 * 2: JPA 1.0
	 * 3: JPA 2.0
	 * 4: JPA 2.1
	 * 5: JPA 2.2 (default)
	 * 6: Jakarta Persistence 3.0
	 * 7: Jakarta Persistence 3.1
	 */
	private static final XsdDescriptor[] xsdCache = new XsdDescriptor[8];

	public XsdDescriptor latestJpaDescriptor() {
		return getJPA31();
	}

	public static boolean shouldBeMappedToLatestJpaDescriptor(String uri) {
		// Any namespace prior to move to Jakarta (3.0) needs to be remapped
		//		NOTE:
		// 			- JPA 1.0 and 2.0 share the same namespace URI
		// 			- JPA 2.1 and 2.2 share the same namespace URI
		return !configurationXsd().getNamespaceUri().equals( uri );

	}

	public XsdDescriptor jpaXsd(String version) {
		switch ( version ) {
			case "1.0": {
				return getJPA10();
			}
			case "2.0": {
				return getJPA20();
			}
			case "2.1": {
				return getJPA21();
			}
			case "2.2": {
				return getJPA22();
			}
			case "3.0": {
				return getJPA30();
			}
			case "3.1": {
				return getJPA31();
			}
			case "3.2": {
				return getJPA32();
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized JPA persistence.xml XSD version : `" + version + "`" );
			}
		}
	}

	public static XsdDescriptor cfgXsd() {
		final int index = 0;
		synchronized ( xsdCache ) {
			XsdDescriptor cfgXml = xsdCache[index];
			if ( cfgXml == null ) {
				cfgXml = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/xsd/cfg/legacy-configuration-4.0.xsd",
						"4.0" ,
						"http://www.hibernate.org/xsd/orm/cfg"
				);
				xsdCache[index] = cfgXml;
			}
			return cfgXml;
		}
	}

	public static XsdDescriptor configurationXsd() {
		final int index = 1;
		synchronized ( xsdCache ) {
			XsdDescriptor cfgXml = xsdCache[index];
			if ( cfgXml == null ) {
				cfgXml = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/xsd/cfg/configuration-3.2.0.xsd",
						"3.2.0" ,
						"http://www.hibernate.org/xsd/orm/configuration"
				);
				xsdCache[index] = cfgXml;
			}
			return cfgXml;
		}
	}

	public static XsdDescriptor getJPA10() {
		final int index = 2;
		synchronized ( xsdCache ) {
			XsdDescriptor jpa10 = xsdCache[index];
			if ( jpa10 == null ) {
				jpa10 = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/jpa/persistence_1_0.xsd",
						"1.0",
						"http://java.sun.com/xml/ns/persistence"
				);
				xsdCache[index] = jpa10;
			}
			return jpa10;
		}
	}

	public static XsdDescriptor getJPA20() {
		final int index = 2;
		synchronized ( xsdCache ) {
			XsdDescriptor jpa20 = xsdCache[index];
			if ( jpa20 == null ) {
				jpa20 = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/jpa/persistence_2_0.xsd",
						"2.0" ,
						"http://java.sun.com/xml/ns/persistence"
				);
				xsdCache[index] = jpa20;
			}
			return jpa20;
		}
	}

	public static XsdDescriptor getJPA21() {
		final int index = 4;
		synchronized ( xsdCache ) {
			XsdDescriptor jpa21 = xsdCache[index];
			if ( jpa21 == null ) {
				jpa21 = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/jpa/persistence_2_1.xsd",
						"2.1",
						"http://xmlns.jcp.org/xml/ns/persistence"
				);
				xsdCache[index] = jpa21;
			}
			return jpa21;
		}
	}

	public static XsdDescriptor getJPA22() {
		final int index = 5;
		synchronized ( xsdCache ) {
			XsdDescriptor jpa22 = xsdCache[index];
			if ( jpa22 == null ) {
				jpa22 = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/jpa/persistence_2_2.xsd",
						"2.2",
						"http://xmlns.jcp.org/xml/ns/persistence"
				);
				xsdCache[index] = jpa22;
			}
			return jpa22;
		}
	}

	public static XsdDescriptor getJPA30() {
		final int index = 6;
		synchronized ( xsdCache ) {
			XsdDescriptor jpa30 = xsdCache[index];
			if ( jpa30 == null ) {
				jpa30 = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/jpa/persistence_3_0.xsd",
						"3.0",
						"https://jakarta.ee/xml/ns/persistence"
				);
				xsdCache[index] = jpa30;
			}
			return jpa30;
		}
	}

	public static XsdDescriptor getJPA31() {
		final int index = 7;
		synchronized ( xsdCache ) {
			XsdDescriptor jpa31 = xsdCache[index];
			if ( jpa31 == null ) {
				jpa31 = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/jpa/persistence_3_1.xsd",
						"3.1",
						"https://jakarta.ee/xml/ns/persistence"
				);
				xsdCache[index] = jpa31;
			}
			return jpa31;
		}
	}

	public static XsdDescriptor getJPA32() {
		final int index = 8;
		synchronized ( xsdCache ) {
			XsdDescriptor jpa32 = xsdCache[index];
			if ( jpa32 == null ) {
				jpa32 = LocalXsdResolver.buildXsdDescriptor(
						"org/hibernate/jpa/persistence_3_2.xsd",
						"3.2",
						"https://jakarta.ee/xml/ns/persistence"
				);
				xsdCache[index] = jpa32;
			}
			return jpa32;
		}
	}

}

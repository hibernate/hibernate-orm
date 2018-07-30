/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.xsd;

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
@SuppressWarnings("unused")
public class ConfigXsdSupport {

	/**
	 * Needs synchronization on any access.
	 * Custom keys:
	 * 0: cfgXml
	 * 1: JPA 1.0
	 * 2: JPA 2.0
	 * 3: JPA 2.1
	 * 4: JPA 2.2
	 */
	private static final XsdDescriptor[] xsdCache = new XsdDescriptor[5];

	public XsdDescriptor latestJpaDescriptor() {
		return getJPA22();
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
			default: {
				throw new IllegalArgumentException( "Unrecognized JPA persistence.xml XSD version : `" + version + "`" );
			}
		}
	}

	public XsdDescriptor cfgXsd() {
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

	private XsdDescriptor getJPA10() {
		final int index = 1;
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

	private XsdDescriptor getJPA20() {
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

	private XsdDescriptor getJPA21() {
		final int index = 3;
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

	private XsdDescriptor getJPA22() {
		final int index = 4;
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

}

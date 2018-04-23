/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.xsd;

import org.jboss.logging.Logger;

/**
 * Support for XSD handling related to Hibernate's `cfg.xml` and
 * JPA's `persistence.xml`.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class ConfigXsdSupport {
	private static final Logger log = Logger.getLogger( ConfigXsdSupport.class );

	/**
	 * Singleton access
	 */
	public static final ConfigXsdSupport INSTANCE = new ConfigXsdSupport();

	private ConfigXsdSupport() {
		//Do not construct new instances
	}

	private final XsdDescriptor jpa10 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/persistence_1_0.xsd",
			"1.0",
			"http://java.sun.com/xml/ns/persistence"
	);

	private final XsdDescriptor jpa20 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/persistence_2_0.xsd",
			"2.0" ,
			"http://java.sun.com/xml/ns/persistence"
	);

	private final XsdDescriptor jpa21 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/persistence_2_1.xsd",
			"2.1",
			"http://xmlns.jcp.org/xml/ns/persistence"
	);

	private final XsdDescriptor jpa22 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/persistence_2_2.xsd",
			"2.2" ,
			"http://xmlns.jcp.org/xml/ns/persistence"
	);

	private final XsdDescriptor cfgXml = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/xsd/cfg/legacy-configuration-4.0.xsd",
			"4.0" ,
			"http://www.hibernate.org/xsd/orm/cfg"
	);

	public XsdDescriptor latestJpaDescriptor() {
		return jpa22;
	}

	public XsdDescriptor jpaXsd(String version) {
		switch ( version ) {
			case "1.0": {
				return jpa10;
			}
			case "2.0": {
				return jpa20;
			}
			case "2.1": {
				return jpa21;
			}
			case "2.2": {
				return jpa22;
			}
			default: {
				throw new IllegalArgumentException( "Unrecognized JPA persistence.xml XSD version : `" + version + "`" );
			}
		}
	}

	public XsdDescriptor cfgXsd() {
		return cfgXml;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.xsd;

/**
 * Support for XSD handling related to Hibernate's `hbm.xml` and
 * JPA's `orm.xml`.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("unused")
public class MappingXsdSupport {

	/**
	 * Singleton access
	 */
	public static final MappingXsdSupport INSTANCE = new MappingXsdSupport();

	private final XsdDescriptor jpa10 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/orm_1_0.xsd",
			"1.0",
			"http://java.sun.com/xml/ns/persistence/orm"
	);

	private final XsdDescriptor jpa20 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/orm_2_0.xsd",
			"2.0",
			"http://java.sun.com/xml/ns/persistence/orm"
	);

	private final XsdDescriptor jpa21 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/orm_2_1.xsd",
			"2.1",
			"http://xmlns.jcp.org/xml/ns/persistence"
	);

	private final XsdDescriptor jpa22 = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/jpa/orm_2_2.xsd",
			"2.2",
			"http://xmlns.jcp.org/xml/ns/persistence"
	);

	private final XsdDescriptor hbmXml = LocalXsdResolver.buildXsdDescriptor(
			"org/hibernate/xsd/mapping/legacy-mapping-4.0.xsd",
			"4.0",
			"http://www.hibernate.org/xsd/orm/hbm"
	);

	private MappingXsdSupport() {
		//Do not construct new instances
	}

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
				throw new IllegalArgumentException( "Unrecognized JPA orm.xml XSD version : `" + version + "`" );
			}
		}
	}

	public XsdDescriptor hbmXsd() {
		return hbmXml;
	}

}

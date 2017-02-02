/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.internal.stax;

import java.net.URL;
import javax.xml.validation.Schema;

import org.hibernate.boot.jaxb.Origin;

/**
 * @author Steve Ebersole
 *
 * @deprecated since 5.0; no longer used internally.
 */
@Deprecated
public enum SupportedOrmXsdVersion {
	ORM_1_0( "org/hibernate/jpa/orm_1_0.xsd" ),
	ORM_2_0( "org/hibernate/jpa/orm_2_0.xsd" ),
	ORM_2_1( "org/hibernate/jpa/orm_2_1.xsd" ),
	ORM_2_1_0( "org/hibernate/xsd/mapping/mapping-2.1.0.xsd" ),
	HBM_4_0( "org/hibernate/xsd/mapping/legacy-mapping-4.0.xsd" );

	private final String schemaResourceName;

	SupportedOrmXsdVersion(String schemaResourceName) {
		this.schemaResourceName = schemaResourceName;
	}

	public static SupportedOrmXsdVersion parse(String name, Origin origin) {
		if ( "1.0".equals( name ) ) {
			return ORM_1_0;
		}
		else if ( "2.0".equals( name ) ) {
			return ORM_2_0;
		}
		else if ( "2.1".equals( name ) ) {
			return ORM_2_1;
		}
		else if ( "2.1.0".equals( name ) ) {
			return ORM_2_1_0;
		}
		else if ( "4.0".equals( name ) ) {
			return HBM_4_0;
		}
		throw new UnsupportedOrmXsdVersionException( name, origin );
	}

	private URL schemaUrl;

	public URL getSchemaUrl() {
		if ( schemaUrl == null ) {
			schemaUrl = LocalSchemaLocator.resolveLocalSchemaUrl( schemaResourceName );
		}
		return schemaUrl;
	}

	private Schema schema;

	public Schema getSchema() {
		if ( schema == null ) {
			schema = LocalSchemaLocator.resolveLocalSchema( getSchemaUrl() );
		}
		return schema;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.xml.internal.stax;

import java.net.URL;
import javax.xml.validation.Schema;

import org.hibernate.xml.spi.Origin;

/**
 * @author Steve Ebersole
 */
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

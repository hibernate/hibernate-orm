/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg;
import org.hibernate.HibernateException;

/**
 * Enumeration of the types of sources of mapping metadata
 * 
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public enum MetadataSourceType {
	/**
	 * Indicates metadata coming from <tt>hbm.xml</tt> files
	 */
	HBM( "hbm" ),
	/**
	 * Indicates metadata coming from either annotations, <tt>orx.xml</tt> or a combination of the two.
	 */
	CLASS( "class" );

	private final String name;

	private MetadataSourceType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	static MetadataSourceType parsePrecedence(String value) {
		if ( HBM.name.equalsIgnoreCase( value ) ) {
			return HBM;
		}

		if ( CLASS.name.equalsIgnoreCase( value ) ) {
			return CLASS;
		}

		throw new HibernateException( "Unknown metadata source type value [" + value + "]" );
	}
}



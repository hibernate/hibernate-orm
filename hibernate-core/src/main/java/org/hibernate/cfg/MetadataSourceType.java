/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public static MetadataSourceType parsePrecedence(String value) {
		if ( HBM.name.equalsIgnoreCase( value ) ) {
			return HBM;
		}

		if ( CLASS.name.equalsIgnoreCase( value ) ) {
			return CLASS;
		}

		throw new HibernateException( "Unknown metadata source type value [" + value + "]" );
	}
}

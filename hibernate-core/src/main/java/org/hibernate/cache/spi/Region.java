/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import org.hibernate.cache.CacheException;
import org.hibernate.internal.util.StringHelper;

/**
 * Defines a contract for accessing a particular named region within the 
 * underlying cache implementation.
 *
 * @author Steve Ebersole
 */
public interface Region {
	// todo (6.0) : consider a multi-part name representation for a few reasons:
	//		1) encapsulate the qualified/non-qualified aspects into one thing
	//		2) parameter type safety (RegionName rather than just String)

	class Name {
		public static Name generate(String qualifier, String localName) {
			return new Name( qualifier, localName );
		}

		private final String qualifier;
		private final String localName;
		private final String qualifiedName;

		private Name(String qualifier, String localName) {
			this.qualifier = qualifier;
			this.localName = localName;

			this.qualifiedName = qualify( qualifier, localName );
		}

		private String qualify(String qualifier, String localName) {
			if ( StringHelper.isEmpty( qualifier ) ) {
				return localName;
			}

			while ( qualifier.endsWith( "." ) ) {
				qualifier = qualifier.substring( 0, qualifier.length()-1 );
			}

			return qualifier + '.' + localName;
		}

		public String getQualifier() {
			return qualifier;
		}

		public String getLocalName() {
			return localName;
		}

		public String getQualifiedName() {
			return qualifiedName;
		}
	}

	/**
	 * Retrieve the name of this region.
	 *
	 * @return The region name
	 */
	String getName();

	RegionFactory getRegionFactory();

	/**
	 * The "end state" contract of the region's lifecycle.  Called
	 * during {@link org.hibernate.SessionFactory#close()} to give
	 * the region a chance to cleanup.
	 *
	 * @throws org.hibernate.cache.CacheException Indicates problem shutting down
	 */
	void destroy() throws CacheException;
}

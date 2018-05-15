/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dynamicentity.tuplizer;
import org.hibernate.EmptyInterceptor;
import org.hibernate.test.dynamicentity.ProxyHelper;

/**
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class EntityNameInterceptor extends EmptyInterceptor {
	/**
	 * The callback from Hibernate to determine the entity name given
	 * a presumed entity instance.
	 *
	 * @param object The presumed entity instance.
	 * @return The entity name (pointing to the proper entity mapping).
	 */
	public String getEntityName(Object object) {
		String entityName = ProxyHelper.extractEntityName( object );
		if ( entityName == null ) {
			entityName = super.getEntityName( object );
		}
		return entityName;
	}
}

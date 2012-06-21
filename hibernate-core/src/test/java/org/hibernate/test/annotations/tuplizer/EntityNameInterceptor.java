//$Id$
package org.hibernate.test.annotations.tuplizer;
import org.hibernate.EmptyInterceptor;

/**
 * @author Emmanuel Bernard
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


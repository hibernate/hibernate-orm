package org.hibernate.test.dynamicentity.interceptor;
import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.test.dynamicentity.Company;
import org.hibernate.test.dynamicentity.Customer;
import org.hibernate.test.dynamicentity.ProxyHelper;

/**
 * Our custom {@link org.hibernate.Interceptor} impl which performs the
 * interpretation of entity-name -> proxy instance and vice-versa.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public class ProxyInterceptor extends EmptyInterceptor {

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

	/**
	 * The callback from Hibernate in order to build an instance of the
	 * entity represented by the given entity name.  Here, we build a
	 * {@link Proxy} representing the entity.
	 *
	 * @param entityName The entity name for which to create an instance.  In our setup,
	 * this is the interface name.
	 * @param entityMode The entity mode in which to create an instance.  Here, we are only
	 * interestes in custom behavior for the POJO entity mode.
	 * @param id The identifier value for the given entity.
	 * @return The instantiated instance.
	 */
	public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
		if ( entityMode == EntityMode.POJO ) {
			if ( Customer.class.getName().equals( entityName ) ) {
				return ProxyHelper.newCustomerProxy( id );
			}
			else if ( Company.class.getName().equals( entityName ) ) {
				return ProxyHelper.newCompanyProxy( id );
			}
		}
		return super.instantiate( entityName, entityMode, id );
	}

}

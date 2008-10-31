//$
package org.hibernate.ejb.packaging;

/**
 * Filter on pachage element
 *
 * @author Emmanuel Bernard
 * @see JavaElementFilter
 */
public abstract class PackageFilter extends JavaElementFilter {
	/**
	 * @see JavaElementFilter#JavaElementFilter(boolean, Class[])
	 */
	protected PackageFilter(boolean retrieveStream, Class[] annotations) {
		super( retrieveStream, annotations );
	}
}
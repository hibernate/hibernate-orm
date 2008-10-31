//$
package org.hibernate.ejb.packaging;

/**
 * Filter on class elements
 *
 * @author Emmanuel Bernard
 * @see JavaElementFilter
 */
public abstract class ClassFilter extends JavaElementFilter {
	/**
	 * @see JavaElementFilter#JavaElementFilter(boolean, Class[])
	 */
	protected ClassFilter(boolean retrieveStream, Class[] annotations) {
		super( retrieveStream, annotations );
	}
}
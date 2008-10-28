//$Id$
package org.hibernate.annotations.common.reflection;

/**
 * Filter properties
 *
 * @author Emmanuel Bernard
 */
public interface Filter {
	boolean returnStatic();

	boolean returnTransient();
}

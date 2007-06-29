// $Id$
package org.hibernate.mapping;

/**
 * Defines mapping elements to which filters may be applied.
 *
 * @author Steve Ebersole
 */
public interface Filterable {
	public void addFilter(String name, String condition);

	public java.util.Map getFilterMap();
}

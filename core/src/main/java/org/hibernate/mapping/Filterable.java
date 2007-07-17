// $Id: Filterable.java 4219 2004-08-10 05:11:47Z steveebersole $
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

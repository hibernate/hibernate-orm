//$Id$
package org.hibernate.test.interfaceproxy;

/**
 * @author Gavin King
 */
public interface Item {
	/**
	 * @return Returns the id.
	 */
	public Long getId();

	/**
	 * @return Returns the name.
	 */
	public String getName();

	/**
	 * @param name The name to set.
	 */
	public void setName(String name);
}
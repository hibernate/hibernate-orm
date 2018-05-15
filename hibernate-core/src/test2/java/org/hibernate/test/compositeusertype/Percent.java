/**
 * 
 */
package org.hibernate.test.compositeusertype;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public enum Percent implements Unit {

	INSTANCE;

	public String getName() {
		return "%";
	}

}

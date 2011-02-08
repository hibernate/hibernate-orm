//$Id: Address.java 9914 2006-05-09 09:37:18Z max.andersen@jboss.com $
package org.hibernate.test.onetoone.joined;


/**
 * @author Gavin King
 */
public class Address {
	public String entityName;
	public String street;
	public String state;
	public String zip;
	
	public String toString() {
		return this.getClass() + ":" + street;
	}
}

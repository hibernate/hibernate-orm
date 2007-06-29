//$Id: Identity.java 7587 2005-07-21 01:22:38Z oneovthafew $
package org.hibernate.test.propertyref.component.partial;

public class Identity {
	private String name;
	private String ssn;
	
	public String getSsn() {
		return ssn;
	}
	public void setSsn(String id) {
		this.ssn = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}

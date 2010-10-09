//$Id$
package org.hibernate.test.usercollection.basic;

/**
 * @author Gavin King
 */
public class Email {
	
	private Long id;
	private String address;
	
	Email() {}
	
	public String getAddress() {
		return address;
	}
	public void setAddress(String type) {
		this.address = type;
	}
	public Email(String type) {
		this.address = type;
	}
	public boolean equals(Object that) {
		if ( !(that instanceof Email) ) return false;
		Email p = (Email) that;
		return this.address.equals(p.address);
	}
	public int hashCode() {
		return address.hashCode();
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
}

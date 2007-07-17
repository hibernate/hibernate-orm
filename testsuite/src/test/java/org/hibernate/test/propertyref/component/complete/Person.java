package org.hibernate.test.propertyref.component.complete;


public class Person {
	private Long id;
	private Identity identity;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Identity getIdentity() {
		return identity;
	}
	public void setIdentity(Identity identity) {
		this.identity = identity;
	}
}

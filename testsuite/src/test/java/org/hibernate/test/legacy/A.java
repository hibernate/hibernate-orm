package org.hibernate.test.legacy;


public class A {
	private Long id;
	private String name;
	private E forward;
	
	/**
	 * Returns the id.
	 * @return Long
	 */
	public Long getId() {
		return id;
	}
	
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	
	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	public E getForward() {
		return forward;
	}

	public void setForward(E e) {
		forward = e;
	}

}







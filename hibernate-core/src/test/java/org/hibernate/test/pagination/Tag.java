package org.hibernate.test.pagination;


public class Tag {
	private int id;
	private String surrogate;
	
	public Tag() {
	
	}

	public Tag(String surrogate) {
		this.surrogate = surrogate;
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getSurrogate() {
		return surrogate;
	}

	public void setSurrogate(String surrogate) {
		this.surrogate = surrogate;
	}

}

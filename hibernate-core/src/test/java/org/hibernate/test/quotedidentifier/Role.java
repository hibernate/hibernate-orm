package org.hibernate.test.quotedidentifier;


public class Role {
	private long id;
	
	private String fooProp;
	
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	public String getFooProp() {
		return fooProp;
	}

	public void setFooProp(String fooProp) {
		this.fooProp = fooProp;
	}
}

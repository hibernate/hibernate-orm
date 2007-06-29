package org.hibernate.test.legacy;


public class Y {

	private Long id;
	private String x;
	private X theX;
	/**
	 * Returns the id.
	 * @return Long
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Returns the x.
	 * @return String
	 */
	public String getX() {
		return x;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sets the x.
	 * @param x The x to set
	 */
	public void setX(String x) {
		this.x = x;
	}

	/**
	 * @return
	 */
	public X getTheX() {
		return theX;
	}

	/**
	 * @param x
	 */
	public void setTheX(X x) {
		theX = x;
	}

}

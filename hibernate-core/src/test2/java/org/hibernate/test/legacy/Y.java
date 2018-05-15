/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
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

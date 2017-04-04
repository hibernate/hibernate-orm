/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Jay.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;


/**
 * @author Gavin King
 */
public class Jay {
	private long id;
	private Eye eye;
	/**
	 * @return Returns the eye.
	 */
	public Eye getEye() {
		return eye;
	}

	/**
	 * @param eye The eye to set.
	 */
	public void setEye(Eye eye) {
		this.eye = eye;
	}

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
	
	public Jay() {}
	
	public Jay(Eye eye) {
		eye.getJays().add(this);
		this.eye = eye;
	}

}
